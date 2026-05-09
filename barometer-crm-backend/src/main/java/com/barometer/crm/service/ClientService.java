package com.barometer.crm.service;

import com.barometer.crm.dto.BulkImportResult;
import com.barometer.crm.dto.PagedResponse;
import com.barometer.crm.exception.EntityNotFoundException;
import com.barometer.crm.model.Activity;
import com.barometer.crm.model.Client;
import com.barometer.crm.repository.ActivityRepository;
import com.barometer.crm.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Map<String, Long> PLAN_PRICES = Map.ofEntries(
        Map.entry("mats_essential", 2500L),
        Map.entry("mats_advanced",  4000L),
        Map.entry("mats_ultimate",  6000L),
        Map.entry("bar_lite",       5000L),
        Map.entry("bar_plus",      12500L),
        Map.entry("bar_managed",   17500L)
    );

    private final ClientRepository clientRepository;
    private final ActivityRepository activityRepository;

    public PagedResponse<Client> getClients(String health, String owner, int page, int size,
                                            String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Client> result;
        if (health != null && !health.isBlank()) {
            result = clientRepository.findByHealth(health, pageable);
        } else if (owner != null && !owner.isBlank()) {
            result = clientRepository.findByOwner(owner, pageable);
        } else {
            result = clientRepository.findAll(pageable);
        }

        return new PagedResponse<>(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    public Client getById(String id) {
        return clientRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
    }

    public Client create(Client client, String performedBy) {
        client.setId(null);
        client.setNextRenewal(computeNextRenewal(client.getStartDate(), client.getBillingCycle()));
        Client saved = clientRepository.save(client);
        logActivity(saved.getId(), "client", "note", "Client created", performedBy);
        return saved;
    }

    public Client update(String id, Client patch, String performedBy) {
        Client existing = getById(id);

        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getContact() != null) existing.setContact(patch.getContact());
        if (patch.getPhone() != null) existing.setPhone(patch.getPhone());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        if (patch.getCity() != null) existing.setCity(patch.getCity());
        if (patch.getType() != null) existing.setType(patch.getType());
        if (patch.getPlanKey() != null) existing.setPlanKey(patch.getPlanKey());
        if (patch.getCostCentres() != null) existing.setCostCentres(patch.getCostCentres());
        if (patch.getStartDate() != null) existing.setStartDate(patch.getStartDate());
        if (patch.getBillingCycle() != null) existing.setBillingCycle(patch.getBillingCycle());
        if (patch.getHealth() != null) existing.setHealth(patch.getHealth());
        if (patch.getNps() != null) existing.setNps(patch.getNps());
        if (patch.getUpsell() != null) existing.setUpsell(patch.getUpsell());
        if (patch.getNotes() != null) existing.setNotes(patch.getNotes());
        if (patch.getOwner() != null) existing.setOwner(patch.getOwner());

        // Always recompute nextRenewal
        existing.setNextRenewal(computeNextRenewal(existing.getStartDate(), existing.getBillingCycle()));

        return clientRepository.save(existing);
    }

    public void delete(String id) {
        getById(id);
        clientRepository.deleteById(java.util.Objects.requireNonNull(id));
    }

    public Client checkin(String id, String notes, String performedBy) {
        Client client = getById(id);
        client.setLastCheckin(LocalDate.now());
        if (notes != null && !notes.isBlank()) {
            client.setNotes(notes);
        }
        Client saved = clientRepository.save(client);
        logActivity(id, "client", "checkin", "Checked in" + (notes != null ? ": " + notes : ""), performedBy);
        return saved;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = clientRepository.count();
        stats.put("total", total);
        stats.put("happy", clientRepository.countByHealth("happy"));
        stats.put("churning", clientRepository.countByHealth("churning"));
        stats.put("atRisk", clientRepository.countByHealth("at_risk"));

        LocalDate today = LocalDate.now();
        long renewingSoon = clientRepository.countByNextRenewalBetween(today, today.plusDays(30));
        stats.put("renewingSoon", renewingSoon);

        // Total ACV
        long totalAcv = clientRepository.findAll().stream()
                .mapToLong(this::computeAcv)
                .sum();
        stats.put("totalAcv", totalAcv);

        return stats;
    }

    private long computeAcv(Client client) {
        long price = PLAN_PRICES.getOrDefault(client.getPlanKey(), 0L);
        int cc = client.getCostCentres() != null ? client.getCostCentres() : 1;
        return price * cc * 12;
    }

    public static LocalDate computeNextRenewal(LocalDate startDate, String billingCycle) {
        if (startDate == null) return null;
        int months = switch (billingCycle != null ? billingCycle : "yearly") {
            case "monthly"   -> 1;
            case "quarterly" -> 3;
            case "biannual"  -> 6;
            default          -> 12;
        };
        LocalDate d = startDate;
        LocalDate today = LocalDate.now();
        while (!d.isAfter(today)) {
            d = d.plusMonths(months);
        }
        return d;
    }

    public BulkImportResult bulkImport(List<Client> clients, String performedBy) {
        int imported = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            try {
                Client client = clients.get(i);
                client.setId(null);
                if (client.getName() == null || client.getName().isBlank()) {
                    errors.add("Row " + (i + 1) + ": name is required");
                    continue;
                }
                client.setNextRenewal(computeNextRenewal(client.getStartDate(), client.getBillingCycle()));
                clientRepository.save(client);
                imported++;
            } catch (Exception e) {
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new BulkImportResult(clients.size(), imported, clients.size() - imported, errors);
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
