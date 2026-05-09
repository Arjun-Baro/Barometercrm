package com.barometer.crm.repository;

import com.barometer.crm.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientRepository extends MongoRepository<Client, String> {

    Page<Client> findByHealth(String health, Pageable pageable);

    Page<Client> findByOwner(String owner, Pageable pageable);

    // Renewing soon
    List<Client> findByNextRenewalBetween(LocalDate from, LocalDate to);

    @Query("{ 'nextRenewal': { $gte: ?0, $lte: ?1 } }")
    List<Client> findRenewingSoon(LocalDate from, LocalDate to);

    long countByHealth(String health);

    long countByNextRenewalBetween(LocalDate from, LocalDate to);

    // Needs checkin — last checkin null or older than cutoff
    @Query("{ $or: [ { 'lastCheckin': null }, { 'lastCheckin': { $lt: ?0 } } ] }")
    List<Client> findNeedsCheckin(LocalDate cutoff);

    long countByNextRenewalBetweenAndHealthNot(LocalDate from, LocalDate to, String health);
}
