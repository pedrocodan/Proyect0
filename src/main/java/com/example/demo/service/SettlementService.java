package com.example.demo.service;

import com.example.demo.dto.BalanceDTO;
import com.example.demo.entity.Expense;
import com.example.demo.entity.ExpenseDetail;
import com.example.demo.entity.Member;
import com.example.demo.entity.Payment;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
    private final ExpenseRepository expenseRepository;
    private final ExpenseDetailRepository expenseDetailRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    public List<BalanceDTO> getBalances(Long groupId) {
        List<Member> members = memberRepository.findByGroupId(groupId);
        List<BalanceDTO> balances = new ArrayList<>();
        
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                Member memberX = members.get(i);
                Member memberY = members.get(j);
                
                BigDecimal balance = calculateBalance(groupId, memberX.getId(), memberY.getId());
                
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    BalanceDTO dto = new BalanceDTO();
                    
                    if (balance.compareTo(BigDecimal.ZERO) > 0) {
                        // memberX owes memberY
                        dto.setDebtorId(memberX.getId());
                        dto.setDebtorName(memberX.getName());
                        dto.setCreditorId(memberY.getId());
                        dto.setCreditorName(memberY.getName());
                        dto.setAmount(balance);
                    } else {
                        // memberY owes memberX
                        dto.setDebtorId(memberY.getId());
                        dto.setDebtorName(memberY.getName());
                        dto.setCreditorId(memberX.getId());
                        dto.setCreditorName(memberX.getName());
                        dto.setAmount(balance.abs());
                    }
                    
                    balances.add(dto);
                }
            }
        }
        
        return balances;
    }

    public BigDecimal getBalance(Long groupId, Long memberId1, Long memberId2) {
        return calculateBalance(groupId, memberId1, memberId2);
    }

    private BigDecimal calculateBalance(Long groupId, Long personXId, Long personYId) {
        // Calculate amount X owes to Y (gastos where Y paid and X should pay)
        BigDecimal xOwesToY = calculateExpenseDebt(groupId, personYId, personXId);
        
        // Calculate amount Y owes to X (gastos where X paid and Y should pay)
        BigDecimal yOwesToX = calculateExpenseDebt(groupId, personXId, personYId);
        
        // Calculate net payments from X to Y
        List<Payment> paymentsXToY = paymentRepository.findPaymentsBetweenMembers(groupId, personXId, personYId);
        
        BigDecimal netPayments = BigDecimal.ZERO;
        for (Payment payment : paymentsXToY) {
            if (payment.getDebtorId().equals(personXId)) {
                netPayments = netPayments.add(payment.getAmount());
            } else {
                netPayments = netPayments.subtract(payment.getAmount());
            }
        }
        
        // Calculate final balance: (X owes Y) - (Y owes X) - (Net payments X to Y)
        BigDecimal balance = xOwesToY.subtract(yOwesToX).subtract(netPayments);
        
        return balance;
    }

    private BigDecimal calculateExpenseDebt(Long groupId, Long paidById, Long owedById) {
        // Find all expenses in this group paid by paidById
        List<Expense> expenses = expenseRepository.findByGroupId(groupId).stream()
                .filter(e -> e.getPaidById().equals(paidById))
                .collect(Collectors.toList());
        
        BigDecimal total = BigDecimal.ZERO;
        
        for (Expense expense : expenses) {
            // Find expense details for the person who owes
            List<ExpenseDetail> details = expenseDetailRepository.findByExpenseId(expense.getId()).stream()
                    .filter(d -> d.getMemberId().equals(owedById))
                    .collect(Collectors.toList());
            
            for (ExpenseDetail detail : details) {
                total = total.add(detail.getAmount());
            }
        }
        
        return total;
    }
}
