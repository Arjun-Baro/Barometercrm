package com.barometer.crm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "push_subscriptions")
public class PushSubscription {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String endpoint;
    private String p256dh;
    private String auth;
    private String deviceLabel;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime lastUsed;
}
