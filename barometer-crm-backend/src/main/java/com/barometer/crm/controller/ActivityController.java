package com.barometer.crm.controller;

import com.barometer.crm.model.Activity;
import com.barometer.crm.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityRepository activityRepository;

    @GetMapping
    public ResponseEntity<List<Activity>> getByEntity(
            @RequestParam String entityId,
            @RequestParam(required = false) String entityType) {
        List<Activity> list = entityType != null
                ? activityRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                : activityRepository.findByEntityIdOrderByCreatedAtDesc(entityId);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Activity> create(@RequestBody Activity activity, Authentication auth) {
        activity.setId(null);
        if (activity.getPerformedBy() == null) {
            activity.setPerformedBy(auth.getName());
        }
        return ResponseEntity.status(201).body(activityRepository.save(activity));
    }
}
