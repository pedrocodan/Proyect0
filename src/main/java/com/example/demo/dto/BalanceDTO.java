package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDTO {
    private Long debtorId;
    private String debtorName;
    private Long creditorId;
    private String creditorName;
    private BigDecimal amount;  // positive: debtor owes creditor, negative: creditor owes debtor
}
