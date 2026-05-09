package com.barometer.crm.controller;

import com.barometer.crm.dto.BulkImportResult;
import com.barometer.crm.dto.PagedResponse;
import com.barometer.crm.model.Lead;
import com.barometer.crm.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public ResponseEntity<PagedResponse<Lead>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(leadService.getLeads(status, owner, type, channel, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getById(@PathVariable String id) {
        return ResponseEntity.ok(leadService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Lead> create(@Valid @RequestBody Lead lead, Authentication auth) {
        return ResponseEntity.status(201).body(leadService.create(lead, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lead> update(@PathVariable String id,
                                       @RequestBody Lead patch,
                                       Authentication auth) {
        return ResponseEntity.ok(leadService.update(id, patch, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/log-contact")
    public ResponseEntity<Lead> logContact(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(leadService.logContact(id, auth.getName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(leadService.getStats());
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResult> bulkImport(@RequestBody List<Lead> leads, Authentication auth) {
        return ResponseEntity.ok(leadService.bulkImport(leads, auth.getName()));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csv = leadService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
