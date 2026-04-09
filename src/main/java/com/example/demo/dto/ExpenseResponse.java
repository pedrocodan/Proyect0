package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private Long groupId;
    private Long paidById;
    private String paidByName;
    private BigDecimal amount;
    private String description;
    private String category;
    private LocalDateTime createdAt;
    private List<ExpenseDetailResponse> expenseDetails;
}
