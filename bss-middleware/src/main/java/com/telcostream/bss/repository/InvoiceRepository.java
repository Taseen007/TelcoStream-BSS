package com.telcostream.bss.repository;

import com.telcostream.bss.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findTop50ByOrderByCreatedAtDesc();

    List<Invoice> findByCallingNumberOrderByCreatedAtDesc(@Param("callingNumber") String callingNumber);

    boolean existsByRecordId(String recordId);

    long countByCreatedAtAfter(Instant since);

    @Query("select coalesce(sum(i.chargedAmount), 0) from Invoice i")
    BigDecimal sumAllChargedAmount();

    @Query("select coalesce(sum(i.chargedAmount), 0) from Invoice i where i.createdAt > :since")
    BigDecimal sumChargedAmountSince(@Param("since") Instant since);
}
