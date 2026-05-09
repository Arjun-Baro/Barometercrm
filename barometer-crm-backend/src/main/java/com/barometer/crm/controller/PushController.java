package com.barometer.crm.controller;

import com.barometer.crm.dto.PushSubscribeRequest;
import com.barometer.crm.model.PushSubscription;
import com.barometer.crm.repository.PushSubscriptionRepository;
import com.barometer.crm.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushNotificationService pushNotificationService;

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidPublicKey));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<PushSubscription> subscribe(@Valid @RequestBody PushSubscribeRequest req,
                                                       Authentication auth) {
        // Upsert by endpoint
        PushSubscription sub = pushSubscriptionRepository.findByEndpoint(req.getEndpoint())
                .orElse(new PushSubscription());

        sub.setUserId(auth.getName());
        sub.setEndpoint(req.getEndpoint());
        sub.setP256dh(req.getP256dh());
        sub.setAuth(req.getAuth());
        sub.setDeviceLabel(req.getDeviceLabel());

        return ResponseEntity.ok(pushSubscriptionRepository.save(sub));
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> test(Authentication auth) {
        Map<String, Object> payload = pushNotificationService.buildPayload(
                "🔔 Test Notification",
                "Push notifications are working!",
                null, "/", "test", null);
        pushNotificationService.sendToUser(auth.getName(), payload);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<PushSubscription>> getSubscriptions(Authentication auth) {
        return ResponseEntity.ok(pushSubscriptionRepository.findByUserId(auth.getName()));
    }
}
