package com.barometer.crm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "notification_rules")
public class NotificationRule {

    @Id
    private String id;

    private String userId;
    private String name;
    private String description;
    private boolean enabled = true;

    private Trigger trigger;
    private List<String> channels; // "push", "browser", "email"
    private Message message;

    @CreatedDate
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    public static class Trigger {
        private String type; // follow_up_due|overdue_followup|renewal_due|no_contact|health_changed|weekly_digest|daily_digest|custom_cron
        private Integer offsetMinutes;
        private Integer offsetDays;
        private String cronExpression;
        private String digestTime; // HH:mm
        private Long thresholdValue;
    }

    @Data
    @NoArgsConstructor
    public static class Message {
        private String title;  // supports {{lead.name}} etc.
        private String body;
        private String url;    // deep link template
    }
}
