package com.telcostream.bss.controller;

import com.telcostream.bss.repository.InvoiceRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Aggregate metrics for the operator dashboard. Kept separate from
 * InvoiceController since these are summary/analytics reads, not record reads.
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*") // demo-only: lock this down to a specific origin in production
public class StatsController {

    private final InvoiceRepository invoiceRepository;

    public StatsController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public Map<String, Object> stats() {
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);

        long totalInvoices = invoiceRepository.count();
        long invoicesLastMinute = invoiceRepository.countByCreatedAtAfter(oneMinuteAgo);
        BigDecimal totalRevenue = invoiceRepository.sumAllChargedAmount();
        BigDecimal revenueLastMinute = invoiceRepository.sumChargedAmountSince(oneMinuteAgo);

        return Map.of(
                "totalInvoices", totalInvoices,
                "invoicesLastMinute", invoicesLastMinute,
                "totalRevenue", totalRevenue,
                "revenueLastMinute", revenueLastMinute,
                "timestamp", Instant.now().toString()
        );
    }
}
