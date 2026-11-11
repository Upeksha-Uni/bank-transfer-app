package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.save(Customer.builder()
                    .accountNumber("100001")
                    .fullName("Kamal Perera")
                    .email("kamal@gmail.com")
                    .balance(new BigDecimal("50000.00"))
                    .build());

            customerRepository.save(Customer.builder()
                    .accountNumber("100002")
                    .fullName("Nimali Silva")
                    .email("nimali@gmail.com")
                    .balance(new BigDecimal("25000.00"))
                    .build());

            customerRepository.save(Customer.builder()
                    .accountNumber("100003")
                    .fullName("Saman Fernando")
                    .email("saman@gmail.com")
                    .balance(new BigDecimal("10000.00"))
                    .build());
        }
    }
}
