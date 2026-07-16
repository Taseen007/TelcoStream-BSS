package com.telcostream.bss.service;

import com.telcostream.bss.entity.Invoice;
import com.telcostream.bss.repository.InvoiceRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Buffers rated invoices in memory and writes them to Postgres in bulk
 * (every N records OR every T milliseconds, whichever comes first) instead
 * of one row per Kafka message. This single decision is what keeps the
 * database alive under real telecom-scale CDR volume.
 */
@Service
public class InvoiceBatchService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceBatchService.class);

    private final ConcurrentLinkedQueue<Invoice> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger bufferSize = new AtomicInteger(0);
    private final InvoiceRepository invoiceRepository;
    private final int maxBatchSize;

    public InvoiceBatchService(InvoiceRepository invoiceRepository,
                                @Value("${bss.batch.max-size}") int maxBatchSize) {
        this.invoiceRepository = invoiceRepository;
        this.maxBatchSize = maxBatchSize;
    }

    /** Called by the Kafka consumer for every successfully rated CDR. */
    public void add(Invoice invoice) {
        buffer.add(invoice);
        if (bufferSize.incrementAndGet() >= maxBatchSize) {
            flush();
        }
    }

    /** Time-based flush trigger — catches low-traffic periods where the size threshold is never hit. */
    @Scheduled(fixedDelayString = "${bss.batch.max-interval-ms}")
    public void scheduledFlush() {
        if (bufferSize.get() > 0) {
            flush();
        }
    }

    @Transactional
    public synchronized void flush() {
        List<Invoice> toWrite = new ArrayList<>();
        Invoice item;
        while ((item = buffer.poll()) != null) {
            toWrite.add(item);
            bufferSize.decrementAndGet();
        }
        if (toWrite.isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();
        invoiceRepository.saveAll(toWrite);
        long elapsedMs = System.currentTimeMillis() - start;

        log.info("Flushed batch of {} invoices to PostgreSQL in {}ms", toWrite.size(), elapsedMs);
    }

    /** Make sure nothing sitting in memory is lost when the JVM shuts down (graceful shutdown). */
    @PreDestroy
    public void flushOnShutdown() {
        log.info("Shutdown signal received, flushing {} buffered invoices before exit", bufferSize.get());
        flush();
    }
}
