package com.example.demo.controller;

import com.example.demo.dto.CreatePaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grupos/{groupId}/pagos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable Long groupId,
            @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getGroupPayments(@PathVariable Long groupId) {
        List<PaymentResponse> payments = paymentService.getGroupPayments(groupId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{memberId1}/{memberId2}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsBetweenMembers(
            @PathVariable Long groupId,
            @PathVariable Long memberId1,
            @PathVariable Long memberId2) {
        List<PaymentResponse> payments = paymentService.getPaymentsBetweenMembers(groupId, memberId1, memberId2);
        return ResponseEntity.ok(payments);
    }
}
