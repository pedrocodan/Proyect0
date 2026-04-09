package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDetailResponse {
    private Long id;
    private Long memberId;
    private String memberName;
    private BigDecimal amount;
}
