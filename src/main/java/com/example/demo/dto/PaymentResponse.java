package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long groupId;
    private Long debtorId;
    private String debtorName;
    private Long creditorId;
    private String creditorName;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;
}
