package com.barometer.crm.repository;

import com.barometer.crm.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeadRepository extends MongoRepository<Lead, String> {

    // Paginated listing with filters
    Page<Lead> findByStatus(String status, Pageable pageable);

    Page<Lead> findByOwner(String owner, Pageable pageable);

    Page<Lead> findByType(String type, Pageable pageable);

    Page<Lead> findByChannel(String channel, Pageable pageable);

    // Follow-up filters
    List<Lead> findByFollowDate(LocalDate followDate);

    List<Lead> findByFollowDateBefore(LocalDate date);

    List<Lead> findByFollowDateBetween(LocalDate from, LocalDate to);

    // Stale leads (not contacted in 7+ days, excluding inactive statuses)
    @Query("{ 'lastContacted': { $lt: ?0 }, 'status': { $nin: ['on_hold', 'not_relevant', 'future_prospect'] } }")
    List<Lead> findStaleLeads(LocalDate cutoffDate);

    @Query("{ $or: [ { 'lastContacted': null }, { 'lastContacted': { $lt: ?0 } } ], 'status': { $nin: ['on_hold', 'not_relevant', 'future_prospect'] } }")
    List<Lead> findStaleLeadsIncludingNull(LocalDate cutoffDate);

    // Hot leads
    @Query("{ 'status': { $in: ['negotiation', 'proposal_sent', 'demo_scheduled'] } }")
    List<Lead> findHotLeads();

    // Stats aggregation helpers
    @Query(value = "{ 'status': { $nin: ['not_relevant', 'on_hold'] } }", fields = "{ 'revenue': 1 }")
    List<Lead> findAllForPipelineValue();

    // Text search
    @Query("{ $text: { $search: ?0 } }")
    Page<Lead> textSearch(String query, Pageable pageable);

    // Count helpers
    long countByStatus(String status);

    long countByFollowDate(LocalDate date);

    long countByFollowDateBefore(LocalDate date);

    // Owner-based queries for push jobs
    @Query("{ 'followDate': ?0 }")
    List<Lead> findByFollowDateExact(LocalDate date);

    @Query("{ 'followDate': { $lt: ?0 } }")
    List<Lead> findOverdueLeads(LocalDate today);
}
