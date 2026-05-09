package com.barometer.crm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "activities")
@CompoundIndex(name = "entity_created_idx", def = "{'entityId': 1, 'createdAt': -1}")
public class Activity {

    @Id
    private String id;

    private String entityType; // "lead" | "client"

    @Indexed
    private String entityId;

    private String type; // call|email|demo|whatsapp|note|status_change|contact_logged|checkin|proposal_sent|meeting
    private String description;
    private String performedBy;

    @CreatedDate
    private LocalDateTime createdAt;
}
