package com.telcostream.bss.controller;

import com.telcostream.bss.service.BalanceService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/balance")
@CrossOrigin(origins = "*") // demo-only: lock this down to a specific origin in production
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/{msisdn}")
    public Map<String, Object> getBalance(@PathVariable String msisdn) {
        BigDecimal balance = balanceService.getBalance(msisdn);
        return Map.of("msisdn", msisdn, "balance", balance);
    }
}
