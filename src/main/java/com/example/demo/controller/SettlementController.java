package com.example.demo.controller;

import com.example.demo.dto.BalanceDTO;
import com.example.demo.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/grupos/{groupId}/balances")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettlementController {
    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<List<BalanceDTO>> getBalances(@PathVariable Long groupId) {
        List<BalanceDTO> balances = settlementService.getBalances(groupId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/{memberId1}/{memberId2}")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable Long groupId,
            @PathVariable Long memberId1,
            @PathVariable Long memberId2) {
        BigDecimal balance = settlementService.getBalance(groupId, memberId1, memberId2);
        return ResponseEntity.ok(balance);
    }
}
