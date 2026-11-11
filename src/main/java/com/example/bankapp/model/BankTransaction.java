
package com.example.bankapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromAccount;

    private String toAccount;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String description;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

