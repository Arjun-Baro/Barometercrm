package com.barometer.crm.controller;

import com.barometer.crm.dto.BulkImportResult;
import com.barometer.crm.dto.PagedResponse;
import com.barometer.crm.model.Client;
import com.barometer.crm.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<PagedResponse<Client>> list(
            @RequestParam(required = false) String health,
            @RequestParam(required = false) String owner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(clientService.getClients(health, owner, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getById(@PathVariable String id) {
        return ResponseEntity.ok(clientService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Client> create(@Valid @RequestBody Client client, Authentication auth) {
        return ResponseEntity.status(201).body(clientService.create(client, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> update(@PathVariable String id,
                                         @RequestBody Client patch,
                                         Authentication auth) {
        return ResponseEntity.ok(clientService.update(id, patch, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<Client> checkin(@PathVariable String id,
                                          @RequestParam(required = false) String notes,
                                          Authentication auth) {
        return ResponseEntity.ok(clientService.checkin(id, notes, auth.getName()));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResult> bulkImport(@RequestBody List<Client> clients, Authentication auth) {
        return ResponseEntity.ok(clientService.bulkImport(clients, auth.getName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(clientService.getStats());
    }
}
