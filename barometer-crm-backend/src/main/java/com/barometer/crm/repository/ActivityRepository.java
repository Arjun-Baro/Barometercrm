package com.barometer.crm.repository;

import com.barometer.crm.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {
    List<Activity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
    List<Activity> findByEntityIdOrderByCreatedAtDesc(String entityId);
}
