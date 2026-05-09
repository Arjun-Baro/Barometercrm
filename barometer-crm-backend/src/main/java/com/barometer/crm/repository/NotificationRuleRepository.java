package com.barometer.crm.repository;

import com.barometer.crm.model.NotificationRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRuleRepository extends MongoRepository<NotificationRule, String> {
    List<NotificationRule> findByUserId(String userId);
    List<NotificationRule> findByUserIdAndEnabled(String userId, boolean enabled);
}
