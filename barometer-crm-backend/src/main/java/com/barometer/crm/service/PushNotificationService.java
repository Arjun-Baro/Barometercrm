package com.barometer.crm.service;

import com.barometer.crm.model.Lead;
import com.barometer.crm.model.PushSubscription;
import com.barometer.crm.repository.ClientRepository;
import com.barometer.crm.repository.LeadRepository;
import com.barometer.crm.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    @Value("${vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${vapid.subject:mailto:admin@barometer.tech}")
    private String vapidSubject;

    private PushService pushService;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;

    @PostConstruct
    public void init() {
        // Register BouncyCastle as security provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (vapidPublicKey == null || vapidPublicKey.isBlank() ||
            vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            log.warn("VAPID keys not configured — push notifications are disabled");
            return;
        }
        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            log.info("PushService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize PushService: {}", e.getMessage());
        }
    }

    // ─── Follow-up due: every 60 seconds ─────────────────────────────────────

    @Scheduled(fixedRate = 60_000, zone = "Asia/Kolkata")
    public void checkFollowUpsDue() {
        try {
            List<Lead> due = leadRepository.findByFollowDate(LocalDate.now());
            if (due.isEmpty()) return;
            String title = "Follow-up Due";
            String body = due.size() == 1
                    ? "Follow up with " + due.get(0).getName() + " today"
                    : due.size() + " follow-ups due today";
            broadcastPush(buildPayload(title, body, "⏰", "/#leads", "follow_up", null));
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("checkFollowUpsDue skipped — DB unavailable: {}", e.getMessage());
        }
    }

    // ─── Daily digest: 8 AM IST ──────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void dailyDigest() {
        try {
            long followUps = leadRepository.countByFollowDate(LocalDate.now());
            long overdue   = leadRepository.countByFollowDateBefore(LocalDate.now());
            String body = "Today: " + followUps + " follow-ups, " + overdue + " overdue.";
            broadcastPush(buildPayload("📋 Daily Digest", body, "📋", "/#leads", "daily_digest", null));
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("dailyDigest skipped — DB unavailable: {}", e.getMessage());
        }
    }

    // ─── Overdue follow-ups: 9 AM IST ────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void overdueFollowUp() {
        try {
            List<Lead> overdue = leadRepository.findOverdueLeads(LocalDate.now());
            if (overdue.isEmpty()) return;
            String body = overdue.size() + " lead(s) have overdue follow-ups. Oldest: " + overdue.get(0).getName();
            broadcastPush(buildPayload("⚠️ Overdue Follow-ups", body, "⚠️", "/#leads", "overdue_followup", null));
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("overdueFollowUp skipped — DB unavailable: {}", e.getMessage());
        }
    }

    // ─── Renewals due: 10 AM IST ─────────────────────────────────────────────

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Kolkata")
    public void renewalDue() {
        try {
            LocalDate today = LocalDate.now();
            long renewing = clientRepository.countByNextRenewalBetween(today, today.plusDays(30));
            if (renewing == 0) return;
            String body = renewing + " client(s) renewing in the next 30 days.";
            broadcastPush(buildPayload("🔄 Renewals Due", body, "🔄", "/#onboarded", "renewal_due", null));
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("renewalDue skipped — DB unavailable: {}", e.getMessage());
        }
    }

    // ─── No contact in 7+ days: every 6 hours ────────────────────────────────

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000L, zone = "Asia/Kolkata")
    public void noContactAlert() {
        try {
            List<Lead> stale = leadRepository.findStaleLeadsIncludingNull(LocalDate.now().minusDays(7));
            if (stale.isEmpty()) return;
            String body = stale.size() + " lead(s) haven't been contacted in 7+ days.";
            broadcastPush(buildPayload("📵 No Contact Alert", body, "📵", "/#leads", "no_contact", null));
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("noContactAlert skipped — DB unavailable: {}", e.getMessage());
        }
    }

    // ─── Weekly digest: Monday 9 AM IST ──────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Kolkata")
    public void weeklyDigest() {
        try {
            long totalLeads   = leadRepository.count();
            long hotLeads     = leadRepository.findHotLeads().size();
            long totalClients = clientRepository.count();
            String body = "This week: " + totalLeads + " leads (" + hotLeads + " hot), " + totalClients + " clients.";
            broadcastPush(buildPayload("📊 Weekly Digest", body, "📊", "/#leads", "weekly_digest", null));
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("weeklyDigest skipped — DB unavailable: {}", e.getMessage());
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public void sendToUser(String userId, Map<String, Object> payload) {
        List<PushSubscription> subs = pushSubscriptionRepository.findByUserId(userId);
        sendToSubscriptions(subs, payload);
    }

    public void broadcastPush(Map<String, Object> payload) {
        List<PushSubscription> subs = pushSubscriptionRepository.findAll();
        sendToSubscriptions(subs, payload);
    }

    private void sendToSubscriptions(List<PushSubscription> subs, Map<String, Object> payload) {
        if (pushService == null) {
            log.warn("PushService not initialized — skipping notification");
            return;
        }
        String json = toJson(payload);
        List<String> stale = new ArrayList<>();

        for (PushSubscription sub : subs) {
            try {
                Subscription.Keys keys = new Subscription.Keys(sub.getP256dh(), sub.getAuth());
                Subscription subscription = new Subscription(sub.getEndpoint(), keys);
                Notification notification = new Notification(subscription, json);
                pushService.send(notification);

                sub.setLastUsed(LocalDateTime.now());
                pushSubscriptionRepository.save(sub);

            } catch (Exception e) {
                log.warn("Push send failed for sub {} — marking stale: {}", sub.getId(), e.getMessage());
                stale.add(sub.getId());
            }
        }

        // Remove stale subscriptions
        if (!stale.isEmpty()) {
            pushSubscriptionRepository.deleteAllById(stale);
        }
    }

    public Map<String, Object> buildPayload(String title, String body, String icon,
                                             String url, String tag, String[] actions) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("icon", icon != null ? icon : "/icons/icon-192.png");
        payload.put("badge", "/icons/badge-72.png");
        payload.put("vibrate", new int[]{200, 100, 200});
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("requireInteraction", false);
        payload.put("tag", tag != null ? tag : "crm-notification");
        payload.put("renotify", true);
        payload.put("data", Map.of("url", url != null ? url : "/"));
        if (actions != null && actions.length > 0) {
            payload.put("actions", actions);
        }
        return payload;
    }

    private String toJson(Map<String, Object> map) {
        // Simple manual JSON serialization for push payload
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
