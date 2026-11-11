package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC")
    List<BankTransaction> findAllLatest();

    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :accountNumber OR t.toAccount = :accountNumber ORDER BY t.createdAt DESC")
    List<BankTransaction> findHistory(@Param("accountNumber") String accountNumber);

    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT t FROM BankTransaction t WHERE t.amount >= :minAmount ORDER BY t.createdAt DESC")
    List<BankTransaction> findTransfersAbove(@Param("minAmount") BigDecimal minAmount);

    @Query("SELECT SUM(t.amount) FROM BankTransaction t WHERE t.fromAccount = :accountNumber AND t.status = com.example.bankapp.model.TransactionStatus.SUCCESS")
    BigDecimal getTotalSentByAccount(@Param("accountNumber") String accountNumber);
}
