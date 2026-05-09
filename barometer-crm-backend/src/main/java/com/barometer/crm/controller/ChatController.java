package com.barometer.crm.controller;

import com.barometer.crm.dto.ChatRequest;
import com.barometer.crm.model.ChatMessage;
import com.barometer.crm.repository.ChatMessageRepository;
import com.barometer.crm.service.ClaudeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ClaudeService claudeService;
    private final ChatMessageRepository chatMessageRepository;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@Valid @RequestBody ChatRequest request,
                                                    Authentication auth) {
        String response = claudeService.chat(auth.getName(), request.getMessage());
        return ResponseEntity.ok(Map.of("response", response));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request,
                                   Authentication auth) {
        // Spring's SSE serializer automatically adds "data: " prefix and "\n\n" suffix
        // Do NOT manually add them here to avoid double-wrapping
        return claudeService.chatStream(auth.getName(), request.getMessage());
    }

    @GetMapping("/context")
    public ResponseEntity<Map<String, String>> getContext() {
        return ResponseEntity.ok(Map.of("context", claudeService.buildCrmContext()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory(Authentication auth) {
        List<ChatMessage> history = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(auth.getName());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(Authentication auth) {
        chatMessageRepository.deleteByUserId(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
