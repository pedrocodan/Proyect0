package com.example.demo.service;

import com.example.demo.dto.CreateExpenseRequest;
import com.example.demo.dto.ExpenseDivisionDTO;
import com.example.demo.dto.ExpenseDetailResponse;
import com.example.demo.dto.ExpenseResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final ExpenseDetailRepository expenseDetailRepository;

    public ExpenseResponse createExpense(Long groupId, CreateExpenseRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        Member paidBy = memberRepository.findById(request.getPaidById())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        
        if (!paidBy.getGroupId().equals(groupId)) {
            throw new ResourceNotFoundException("Member does not belong to this group");
        }
        
        // Validate that divisions sum equals amount
        BigDecimal divisionsSum = request.getDivisions().stream()
                .map(ExpenseDivisionDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (divisionsSum.compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("Divisions sum must equal the total amount");
        }
        
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setCategory(Expense.ExpenseCategory.valueOf(request.getCategory().toUpperCase()));
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Create expense details for each division
        for (ExpenseDivisionDTO division : request.getDivisions()) {
            Member member = memberRepository.findById(division.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            
            if (!member.getGroupId().equals(groupId)) {
                throw new ResourceNotFoundException("Member does not belong to this group");
            }
            
            ExpenseDetail detail = new ExpenseDetail();
            detail.setExpense(savedExpense);
            detail.setMember(member);
            detail.setAmount(division.getAmount());
            
            expenseDetailRepository.save(detail);
        }
        
        savedExpense = expenseRepository.findById(savedExpense.getId()).get();
        return mapToResponse(savedExpense);
    }

    public List<ExpenseResponse> getGroupExpenses(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        
        return expenseRepository.findByGroupId(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ExpenseResponse getExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return mapToResponse(expense);
    }

    public void deleteExpense(Long expenseId) {
        if (!expenseRepository.existsById(expenseId)) {
            throw new ResourceNotFoundException("Expense not found");
        }
        expenseRepository.deleteById(expenseId);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setGroupId(expense.getGroupId());
        response.setPaidById(expense.getPaidById());
        response.setPaidByName(expense.getPaidBy().getName());
        response.setAmount(expense.getAmount());
        response.setDescription(expense.getDescription());
        response.setCategory(expense.getCategory().name());
        response.setCreatedAt(expense.getCreatedAt());
        
        if (expense.getExpenseDetails() != null) {
            response.setExpenseDetails(expense.getExpenseDetails().stream()
                    .map(detail -> new ExpenseDetailResponse(
                            detail.getId(),
                            detail.getMemberId(),
                            detail.getMember().getName(),
                            detail.getAmount()
                    ))
                    .collect(Collectors.toList()));
        }
        
        return response;
    }
}
