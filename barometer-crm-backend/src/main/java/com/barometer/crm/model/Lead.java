package com.barometer.crm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "leads")
@CompoundIndex(name = "follow_date_idx", def = "{'followDate': 1}")
@CompoundIndex(name = "status_idx", def = "{'status': 1}")
@CompoundIndex(name = "owner_idx", def = "{'owner': 1}")
@CompoundIndex(name = "last_contacted_idx", def = "{'lastContacted': 1}")
public class Lead {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    @TextIndexed
    private String name;

    @NotBlank(message = "Contact is required")
    @TextIndexed
    private String contact;

    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @TextIndexed
    private String city;

    private String type;
    private String channel;

    @NotBlank(message = "Status is required")
    @Indexed
    private String status = "contacted";

    private Long revenue;

    private LocalDate followDate;
    private String followTime;

    private LocalDate lastContacted;

    private String owner;

    @Size(max = 1000)
    private String notes;

    @Size(max = 1000)
    private String remarks;

    private String gstName;
    private String gstNo;
    private Integer outlets;
    private String plan;
    private String decisionMaker;
    private String decisionMakerRole;
    private String website;
    private String startDate;
    private String paymentCycle;
    private List<String> tags;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
