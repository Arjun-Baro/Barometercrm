package com.barometer.crm.service;

import com.barometer.crm.model.NotificationRule;
import com.barometer.crm.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationRuleService {

    private final NotificationRuleRepository notificationRuleRepository;

    public List<NotificationRule> getByUser(String userId) {
        return notificationRuleRepository.findByUserId(userId);
    }

    public NotificationRule create(NotificationRule rule, String userId) {
        rule.setId(null);
        rule.setUserId(userId);
        return notificationRuleRepository.save(rule);
    }

    public NotificationRule update(String id, NotificationRule patch, String userId) {
        NotificationRule existing = notificationRuleRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new com.barometer.crm.exception.EntityNotFoundException("Rule not found: " + id));
        if (!existing.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your rule");
        }
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        existing.setEnabled(patch.isEnabled());
        if (patch.getTrigger() != null) existing.setTrigger(patch.getTrigger());
        if (patch.getChannels() != null) existing.setChannels(patch.getChannels());
        if (patch.getMessage() != null) existing.setMessage(patch.getMessage());
        return notificationRuleRepository.save(existing);
    }

    public void delete(String id, String userId) {
        NotificationRule existing = notificationRuleRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new com.barometer.crm.exception.EntityNotFoundException("Rule not found: " + id));
        if (!existing.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your rule");
        }
        notificationRuleRepository.deleteById(java.util.Objects.requireNonNull(id));
    }
}
