package com.barometer.crm.service;

import com.barometer.crm.dto.BulkImportResult;
import com.barometer.crm.dto.PagedResponse;
import com.barometer.crm.exception.EntityNotFoundException;
import com.barometer.crm.model.Activity;
import com.barometer.crm.model.Lead;
import com.barometer.crm.repository.ActivityRepository;
import com.barometer.crm.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final ActivityRepository activityRepository;

    public PagedResponse<Lead> getLeads(String status, String owner, String type, String channel,
                                        int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Lead> result;
        if (status != null && !status.isBlank()) {
            result = leadRepository.findByStatus(status, pageable);
        } else if (owner != null && !owner.isBlank()) {
            result = leadRepository.findByOwner(owner, pageable);
        } else if (type != null && !type.isBlank()) {
            result = leadRepository.findByType(type, pageable);
        } else if (channel != null && !channel.isBlank()) {
            result = leadRepository.findByChannel(channel, pageable);
        } else {
            result = leadRepository.findAll(pageable);
        }

        return new PagedResponse<>(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    public Lead getById(String id) {
        return leadRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Lead not found: " + id));
    }

    public Lead create(Lead lead, String performedBy) {
        lead.setId(null);
        Lead saved = leadRepository.save(lead);
        logActivity(saved.getId(), "lead", "note", "Lead created", performedBy);
        return saved;
    }

    public Lead update(String id, Lead patch, String performedBy) {
        Lead existing = getById(id);

        // Track status change
        boolean statusChanged = patch.getStatus() != null && !patch.getStatus().equals(existing.getStatus());

        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getContact() != null) existing.setContact(patch.getContact());
        if (patch.getPhone() != null) existing.setPhone(patch.getPhone());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        if (patch.getCity() != null) existing.setCity(patch.getCity());
        if (patch.getType() != null) existing.setType(patch.getType());
        if (patch.getChannel() != null) existing.setChannel(patch.getChannel());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getRevenue() != null) existing.setRevenue(patch.getRevenue());
        if (patch.getFollowDate() != null) existing.setFollowDate(patch.getFollowDate());
        if (patch.getFollowTime() != null) existing.setFollowTime(patch.getFollowTime());
        if (patch.getOwner() != null) existing.setOwner(patch.getOwner());
        if (patch.getNotes() != null) existing.setNotes(patch.getNotes());
        if (patch.getRemarks() != null) existing.setRemarks(patch.getRemarks());
        if (patch.getGstName() != null) existing.setGstName(patch.getGstName());
        if (patch.getGstNo() != null) existing.setGstNo(patch.getGstNo());
        if (patch.getOutlets() != null) existing.setOutlets(patch.getOutlets());
        if (patch.getPlan() != null) existing.setPlan(patch.getPlan());
        if (patch.getDecisionMaker() != null) existing.setDecisionMaker(patch.getDecisionMaker());
        if (patch.getDecisionMakerRole() != null) existing.setDecisionMakerRole(patch.getDecisionMakerRole());
        if (patch.getWebsite() != null) existing.setWebsite(patch.getWebsite());
        if (patch.getStartDate() != null) existing.setStartDate(patch.getStartDate());
        if (patch.getPaymentCycle() != null) existing.setPaymentCycle(patch.getPaymentCycle());
        if (patch.getTags() != null) existing.setTags(patch.getTags());

        Lead saved = leadRepository.save(java.util.Objects.requireNonNull(existing));

        if (statusChanged) {
            logActivity(id, "lead", "status_change",
                    "Status changed to " + patch.getStatus(), performedBy);
        }
        return saved;
    }

    public void delete(String id) {
        getById(id); // ensure exists
        leadRepository.deleteById(java.util.Objects.requireNonNull(id));
    }

    public Lead logContact(String id, String performedBy) {
        Lead lead = getById(id);
        lead.setLastContacted(LocalDate.now());
        Lead saved = leadRepository.save(lead);
        logActivity(id, "lead", "contact_logged", "Contact logged", performedBy);
        return saved;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = leadRepository.count();
        stats.put("total", total);
        stats.put("hot", leadRepository.findHotLeads().size());
        stats.put("stale", leadRepository.findStaleLeadsIncludingNull(LocalDate.now().minusDays(7)).size());
        stats.put("followUpsToday", leadRepository.countByFollowDate(LocalDate.now()));
        stats.put("overdueFollowUps", leadRepository.countByFollowDateBefore(LocalDate.now()));

        // Pipeline value
        long pipeline = leadRepository.findAllForPipelineValue()
                .stream()
                .mapToLong(l -> l.getRevenue() != null ? l.getRevenue() : 0L)
                .sum();
        stats.put("pipelineValue", pipeline);

        return stats;
    }

    public BulkImportResult bulkImport(List<Lead> leads, String performedBy) {
        int imported = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < leads.size(); i++) {
            try {
                Lead lead = leads.get(i);
                lead.setId(null);
                if (lead.getName() == null || lead.getName().isBlank()) {
                    errors.add("Row " + (i + 1) + ": name is required");
                    continue;
                }
                if (lead.getContact() == null || lead.getContact().isBlank()) {
                    errors.add("Row " + (i + 1) + ": contact is required");
                    continue;
                }
                leadRepository.save(lead);
                imported++;
            } catch (Exception e) {
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new BulkImportResult(leads.size(), imported, leads.size() - imported, errors);
    }

    public String exportCsv() {
        List<Lead> leads = leadRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,contact,phone,email,city,type,channel,status,revenue,followDate,lastContacted,owner,plan,outlets\n");
        for (Lead l : leads) {
            sb.append(csv(l.getId())).append(",")
              .append(csv(l.getName())).append(",")
              .append(csv(l.getContact())).append(",")
              .append(csv(l.getPhone())).append(",")
              .append(csv(l.getEmail())).append(",")
              .append(csv(l.getCity())).append(",")
              .append(csv(l.getType())).append(",")
              .append(csv(l.getChannel())).append(",")
              .append(csv(l.getStatus())).append(",")
              .append(l.getRevenue() != null ? l.getRevenue() : "").append(",")
              .append(l.getFollowDate() != null ? l.getFollowDate().toString() : "").append(",")
              .append(l.getLastContacted() != null ? l.getLastContacted().toString() : "").append(",")
              .append(csv(l.getOwner())).append(",")
              .append(csv(l.getPlan())).append(",")
              .append(l.getOutlets() != null ? l.getOutlets() : "").append("\n");
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void logActivity(String entityId, String entityType, String type, String desc, String by) {
        Activity activity = new Activity();
        activity.setEntityId(entityId);
        activity.setEntityType(entityType);
        activity.setType(type);
        activity.setDescription(desc);
        activity.setPerformedBy(by);
        activityRepository.save(activity);
    }
}
