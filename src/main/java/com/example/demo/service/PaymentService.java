package com.example.demo.service;

import com.example.demo.dto.CreatePaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.entity.Group;
import com.example.demo.entity.Member;
import com.example.demo.entity.Payment;
import com.example.demo.exception.InvalidPaymentException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final SettlementService settlementService;

    public PaymentResponse recordPayment(Long groupId, CreatePaymentRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        Member debtor = memberRepository.findById(request.getDebtorId())
                .orElseThrow(() -> new ResourceNotFoundException("Debtor member not found"));
        
        Member creditor = memberRepository.findById(request.getCreditorId())
                .orElseThrow(() -> new ResourceNotFoundException("Creditor member not found"));
        
        if (!debtor.getGroupId().equals(groupId)) {
            throw new ResourceNotFoundException("Debtor does not belong to this group");
        }
        
        if (!creditor.getGroupId().equals(groupId)) {
            throw new ResourceNotFoundException("Creditor does not belong to this group");
        }
        
        // Check if the payment doesn't exceed the current balance
        BigDecimal currentBalance = settlementService.getBalance(groupId, request.getDebtorId(), request.getCreditorId());
        
        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Debtor does not owe money to creditor");
        }
        
        if (request.getAmount().compareTo(currentBalance) > 0) {
            throw new InvalidPaymentException("Payment amount exceeds current balance of " + currentBalance);
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be positive");
        }
        
        Payment payment = new Payment();
        payment.setGroup(group);
        payment.setDebtor(debtor);
        payment.setCreditor(creditor);
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        
        Payment savedPayment = paymentRepository.save(payment);
        return mapToResponse(savedPayment);
    }

    public List<PaymentResponse> getGroupPayments(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        
        return paymentRepository.findByGroupId(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsBetweenMembers(Long groupId, Long memberId1, Long memberId2) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        
        if (!memberRepository.existsById(memberId1)) {
            throw new ResourceNotFoundException("Member not found");
        }
        
        if (!memberRepository.existsById(memberId2)) {
            throw new ResourceNotFoundException("Member not found");
        }
        
        return paymentRepository.findPaymentsBetweenMembers(groupId, memberId1, memberId2).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getGroupId(),
                payment.getDebtorId(),
                payment.getDebtor().getName(),
                payment.getCreditorId(),
                payment.getCreditor().getName(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getCreatedAt()
        );
    }
}
