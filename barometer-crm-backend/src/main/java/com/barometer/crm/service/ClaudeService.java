package com.barometer.crm.service;

import com.barometer.crm.model.Lead;
import com.barometer.crm.model.ChatMessage;
import com.barometer.crm.repository.ChatMessageRepository;
import com.barometer.crm.repository.ClientRepository;
import com.barometer.crm.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    @Value("${claude.api-key}")
    private String claudeApiKey;

    @Value("${claude.model:claude-sonnet-4-20250514}")
    private String claudeModel;

    private static final int MAX_TOKENS = 2048;
    private static final String CLAUDE_API = "https://api.anthropic.com/v1/messages";

    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * Build live CRM context string from MongoDB data to inject into Claude's system prompt.
     */
    public String buildCrmContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LIVE CRM DATA ===\n\n");

        // Lead stats
        long totalLeads = leadRepository.count();
        List<Lead> hotLeads = leadRepository.findHotLeads();
        List<Lead> stale = leadRepository.findStaleLeadsIncludingNull(
                java.time.LocalDate.now().minusDays(7));
        sb.append("LEADS: total=").append(totalLeads)
          .append(", hot=").append(hotLeads.size())
          .append(", stale=").append(stale.size()).append("\n");

        // Recent hot leads sample (up to 5)
        if (!hotLeads.isEmpty()) {
            sb.append("HOT LEADS (up to 5):\n");
            hotLeads.stream().limit(5).forEach(l ->
                sb.append("  - ").append(l.getName()).append(" | ").append(l.getContact())
                  .append(" | ").append(l.getStatus()).append(" | ₹")
                  .append(l.getRevenue() != null ? l.getRevenue() : 0).append("\n")
            );
        }

        // Client stats
        long totalClients = clientRepository.count();
        long atRisk = clientRepository.countByHealth("at_risk");
        long churning = clientRepository.countByHealth("churning");
        java.time.LocalDate today = java.time.LocalDate.now();
        long renewingSoon = clientRepository.countByNextRenewalBetween(today, today.plusDays(30));
        sb.append("\nCLIENTS: total=").append(totalClients)
          .append(", at_risk=").append(atRisk)
          .append(", churning=").append(churning)
          .append(", renewing_in_30_days=").append(renewingSoon).append("\n");

        return sb.toString();
    }

    public String buildSystemPrompt() {
        String context = buildCrmContext();
        return """
                You are Barometer CRM Assistant — an expert sales assistant for Barometer Technologies, \
                a Mumbai-based F&B inventory management SaaS company. \
                You help the sales team manage leads, clients, follow-ups, and renewals. \
                Be concise, data-driven, and actionable. \
                Always speak in the context of B2B SaaS sales. \
                
                """ + context;
    }

    /**
     * Non-streaming chat — returns complete response string.
     */
    public String chat(String userId, String userMessage) {
        List<Map<String, String>> messages = buildMessages(userId, userMessage);

        Map<String, Object> requestBody = Map.of(
                "model", claudeModel,
                "max_tokens", MAX_TOKENS,
                "system", buildSystemPrompt(),
                "messages", messages
        );

        Map<?, ?> response = webClient.post()
                .uri(CLAUDE_API)
                .header("x-api-key", claudeApiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> new RuntimeException(
                            "Claude API error " + clientResponse.statusCode() + ": " + body))
                )
                .bodyToMono(Map.class)
                .block();

        String assistantText = extractContent(response);
        persistMessages(userId, userMessage, assistantText);
        return assistantText;
    }

    /**
     * Streaming chat — returns SSE text chunks via Flux<String>.
     */
    public Flux<String> chatStream(String userId, String userMessage) {
        List<Map<String, String>> messages = buildMessages(userId, userMessage);

        Map<String, Object> requestBody = Map.of(
                "model", claudeModel,
                "max_tokens", MAX_TOKENS,
                "stream", true,
                "system", buildSystemPrompt(),
                "messages", messages
        );

        StringBuilder fullResponse = new StringBuilder();

        return webClient.post()
                .uri(CLAUDE_API)
                .header("x-api-key", claudeApiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> new RuntimeException(
                            "Claude API error " + clientResponse.statusCode() + ": " + body))
                )
                .bodyToFlux(String.class)
                .filter(chunk -> chunk.contains("\"type\":\"content_block_delta\""))
                .map(chunk -> {
                    // Extract delta text from SSE data line
                    try {
                        // chunk looks like: data: {...}
                        String json = chunk.startsWith("data: ") ? chunk.substring(6) : chunk;
                        // Quick parse of delta.text
                        int idx = json.indexOf("\"text\":\"");
                        if (idx == -1) return "";
                        int start = idx + 8;
                        int end = json.indexOf("\"", start);
                        if (end == -1) return "";
                        String text = json.substring(start, end)
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                        fullResponse.append(text);
                        return text;
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(text -> !text.isEmpty())
                .doOnComplete(() -> {
                    // Persist after streaming is done
                    persistMessages(userId, userMessage, fullResponse.toString());
                })
                .onErrorResume(e -> {
                    log.error("Chat stream error for user {}: {}", userId, e.getMessage());
                    return Flux.just("Sorry, I encountered an error. Please try again.");
                });
    }

    public List<Map<String, String>> buildMessages(String userId, String newUserMessage) {
        List<ChatMessage> history = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        // Reverse to chronological order, keep last 20
        Collections.reverse(history);
        if (history.size() > 20) {
            history = history.subList(history.size() - 20, history.size());
        }

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", newUserMessage));
        return messages;
    }

    private void persistMessages(String userId, String userMessage, String assistantMessage) {
        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        chatMessageRepository.save(userMsg);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantMessage);
        chatMessageRepository.save(assistantMsg);
    }

    private String extractContent(Map<?, ?> response) {
        if (response == null) return "";
        Object content = response.get("content");
        if (content instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object text = map.get("text");
                if (text instanceof String s) return s;
            }
        }
        return "";
    }
}
