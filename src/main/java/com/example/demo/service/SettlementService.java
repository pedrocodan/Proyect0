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
    
    // Step 1: Calculate net balance for each member
    Map<Long, BigDecimal> memberNetBalance = new HashMap<>();
    for (Member member : members) {
        memberNetBalance.put(member.getId(), BigDecimal.ZERO);
    }
    
    // Calculate net balance from expenses and payments
    for (Member member : members) {
        for (Member other : members) {
            if (!member.getId().equals(other.getId())) {
                BigDecimal balance = calculateBalance(groupId, member.getId(), other.getId());
                memberNetBalance.put(member.getId(), memberNetBalance.get(member.getId()).add(balance));
            }
        }
    }
    
    // Step 2: Separate debtors and creditors
    List<MemberBalance> debtors = new ArrayList<>();
    List<MemberBalance> creditors = new ArrayList<>();
    
    for (Member member : members) {
        BigDecimal netBalance = memberNetBalance.get(member.getId());
        if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
            debtors.add(new MemberBalance(member.getId(), member.getName(), netBalance));
        } else if (netBalance.compareTo(BigDecimal.ZERO) < 0) {
            creditors.add(new MemberBalance(member.getId(), member.getName(), netBalance.abs()));
        }
    }
    
    // Step 3: Match debtors with creditors (greedy algorithm)
    List<BalanceDTO> optimizedBalances = new ArrayList<>();
    
    for (MemberBalance debtor : debtors) {
        BigDecimal remainingDebt = debtor.balance;
        
        for (MemberBalance creditor : creditors) {
            if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            if (creditor.balance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal paymentAmount = remainingDebt.min(creditor.balance);
                
                BalanceDTO dto = new BalanceDTO();
                dto.setDebtorId(debtor.memberId);
                dto.setDebtorName(debtor.memberName);
                dto.setCreditorId(creditor.memberId);
                dto.setCreditorName(creditor.memberName);
                dto.setAmount(paymentAmount);
                
                optimizedBalances.add(dto);
                
                remainingDebt = remainingDebt.subtract(paymentAmount);
                creditor.balance = creditor.balance.subtract(paymentAmount);
            }
        }
    }
    
    return optimizedBalances;
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

    // Helper class for settlement calculation (only used in memory)
    private static class MemberBalance {
        Long memberId;
        String memberName;
        BigDecimal balance;
        
        MemberBalance(Long memberId, String memberName, BigDecimal balance) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.balance = balance;
        }
    }
}
