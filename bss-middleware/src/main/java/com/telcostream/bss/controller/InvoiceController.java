package com.telcostream.bss.controller;

import com.telcostream.bss.entity.Invoice;
import com.telcostream.bss.repository.InvoiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*") // demo-only: lock this down to a specific origin in production
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public List<Invoice> recent() {
        return invoiceRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @GetMapping("/subscriber/{msisdn}")
    public List<Invoice> forSubscriber(@PathVariable String msisdn) {
        return invoiceRepository.findByCallingNumberOrderByCreatedAtDesc(msisdn);
    }
}
