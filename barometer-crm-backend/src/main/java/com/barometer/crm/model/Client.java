package com.barometer.crm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "clients")
@CompoundIndex(name = "next_renewal_idx", def = "{'nextRenewal': 1}")
@CompoundIndex(name = "health_idx", def = "{'health': 1}")
@CompoundIndex(name = "client_owner_idx", def = "{'owner': 1}")
public class Client {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Contact is required")
    private String contact;

    private String phone;
    private String email;
    private String city;
    private String type;

    @NotBlank(message = "Plan key is required")
    private String planKey;

    private Integer costCentres = 1;
    private LocalDate startDate;
    private String billingCycle = "yearly";

    private LocalDate nextRenewal;   // always recomputed server-side on save

    @Indexed
    private String health = "happy";

    @Min(1) @Max(5)
    private Integer nps;

    private LocalDate lastCheckin;

    @Size(max = 1000)
    private String upsell;

    @Size(max = 1000)
    private String notes;

    private String owner;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
