package com.barometer.crm.controller;

import com.barometer.crm.model.NotificationRule;
import com.barometer.crm.service.NotificationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationRuleService service;

    @GetMapping
    public ResponseEntity<List<NotificationRule>> list(Authentication auth) {
        return ResponseEntity.ok(service.getByUser(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<NotificationRule> create(@RequestBody NotificationRule rule, Authentication auth) {
        return ResponseEntity.status(201).body(service.create(rule, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationRule> update(@PathVariable String id,
                                                    @RequestBody NotificationRule patch,
                                                    Authentication auth) {
        return ResponseEntity.ok(service.update(id, patch, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        service.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
