package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    public List<BankTransaction> getAllTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public long getSuccessCount() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long countSuccessTransactions() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public long countFailedTransactions() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public long countCustomersNative() {
        return customerRepository.countCustomersNative();
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minAmount) {
        return transactionRepository.findTransfersAbove(minAmount);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = transactionRepository.getTotalSentByAccount(accountNumber);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists: " + customer.getAccountNumber());
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email already exists: " + customer.getEmail());
        }
        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Deposit amount must be greater than zero.");
        }
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        customerRepository.credit(accountNumber, amount);

        BankTransaction tx = BankTransaction.builder()
                .fromAccount("SYSTEM")
                .toAccount(accountNumber)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .description("Deposit completed")
                .build();
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Withdrawal amount must be greater than zero.");
        }
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        int updated = customerRepository.debit(accountNumber, amount);
        if (updated == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + accountNumber);
        }

        BankTransaction tx = BankTransaction.builder()
                .fromAccount(accountNumber)
                .toAccount("SYSTEM")
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .description("Withdrawal completed")
                .build();
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Source and destination must be different accounts.");
        }

        if (customerRepository.countAccounts(fromAccount) == 0) {
            throw new AccountNotFoundException("Sender account not found: " + fromAccount);
        }

        if (customerRepository.countAccounts(toAccount) == 0) {
            throw new AccountNotFoundException("Receiver account not found: " + toAccount);
        }

        int updatedRows = customerRepository.debit(fromAccount, amount);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAccount);
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal. All changes were rolled back.");
        }

        customerRepository.credit(toAccount, amount);

        BankTransaction successTx = BankTransaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .description("Transfer completed")
                .build();

        return transactionRepository.save(successTx);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transferWithFee(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Source and destination must be different accounts.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Transfer amount must be greater than zero.");
        }

        if (customerRepository.countAccounts(fromAccount) == 0) {
            throw new AccountNotFoundException("Sender account not found: " + fromAccount);
        }

        if (customerRepository.countAccounts(toAccount) == 0) {
            throw new AccountNotFoundException("Receiver account not found: " + toAccount);
        }

        if (!customerRepository.existsById("999999")) {
            customerRepository.save(Customer.builder()
                    .accountNumber("999999")
                    .fullName("Bank Fee Account")
                    .email("fee@bank.com")
                    .balance(BigDecimal.ZERO)
                    .build());
        }

        BigDecimal fee = amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);

        int updatedRows = customerRepository.debit(fromAccount, totalDebit);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAccount);
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal. All changes were rolled back.");
        }

        customerRepository.credit(toAccount, amount);
        customerRepository.credit("999999", fee);

        BankTransaction tx = BankTransaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .description("Transfer completed (Fee: " + fee + ")")
                .build();

        return transactionRepository.save(tx);
    }
}
