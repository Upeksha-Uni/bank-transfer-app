
package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    public BankController(
            BankService bankService,
            TransactionAuditService auditService) {

        this.bankService = bankService;
        this.auditService = auditService;
    }



    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        loadDashboard(model, keyword);

        return "bank";
    }


    private void loadDashboard(Model model, String keyword) {

        model.addAttribute(
            "totalCustomers", bankService.countCustomersNative()
        );

        model.addAttribute(
            "totalBalance", bankService.getTotalBalance()
        );

        model.addAttribute(
            "successCount", bankService.countSuccessTransactions()
        );

        model.addAttribute(
            "failedCount", bankService.countFailedTransactions()
        );

        model.addAttribute(
            "allCustomers", bankService.getAllCustomers()
        );

        model.addAttribute(
            "customers", bankService.searchCustomers(keyword)
        );

        model.addAttribute(
            "transactions", bankService.getAllTransactions()
        );

        model.addAttribute("customer", new Customer());

        model.addAttribute(
            "transferRequest", new TransferRequest()
        );

        model.addAttribute("keyword", keyword);
    }


    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }

        try {

            bankService.createCustomer(customer);

            redirectAttributes.addFlashAttribute(
                "success", "Customer added successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                "error", e.getMessage()
            );
        }

        return "redirect:/";
    }


    @PostMapping("/transfer")
    public String transferMoney(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }

        try {

            bankService.transfer(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                request.isSimulateFailure()
            );

            redirectAttributes.addFlashAttribute(
                "success", "Transfer completed successfully."
            );

        } catch (Exception e) {

            recordFailedTransfer(request, e);

            redirectAttributes.addFlashAttribute(
                "error", e.getMessage()
            );
        }

        return "redirect:/";
    }


    @GetMapping("/deposit")
    public String depositPage(Model model) {

        model.addAttribute(
            "customers", bankService.getAllCustomers()
        );

        return "deposit";
    }


    @PostMapping("/deposit")
    public String depositMoney(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {

            bankService.deposit(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                "success", "Deposit successful!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                "error", e.getMessage()
            );
        }

        return "redirect:/deposit";
    }


    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {

        model.addAttribute(
            "customers", bankService.getAllCustomers()
        );

        return "withdraw";
    }


    @PostMapping("/withdraw")
    public String withdrawMoney(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {

            bankService.withdraw(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                "success", "Withdrawal successful!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                "error", e.getMessage()
            );
        }

        return "redirect:/withdraw";
    }


    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {

        model.addAttribute("min", min);

        model.addAttribute(
            "transfers", bankService.getLargeTransfers(min)
        );

        return "large-transfers";
    }


    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model) {

        model.addAttribute(
            "customer", bankService.getCustomer(accountNumber)
        );

        model.addAttribute(
            "transactions", bankService.getAccountHistory(accountNumber)
        );

        model.addAttribute(
            "totalSent", bankService.getTotalSent(accountNumber)
        );

        return "history";
    }




    @GetMapping("/api/customers")
    @ResponseBody
    public ResponseEntity<?> getAllCustomersApi() {

        return ResponseEntity.ok(
            bankService.getAllCustomers()
        );
    }


    @PostMapping(
        value = "/api/customers",
        consumes = "application/json"
    )
    @ResponseBody
    public ResponseEntity<?> addCustomerApi(
            @Valid @RequestBody Customer customer,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(
                validationErrors(result)
            );
        }

        try {

            bankService.createCustomer(customer);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Customer added successfully."
                ));

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/api/transactions")
    @ResponseBody
    public ResponseEntity<?> getAllTransactionsApi() {

        return ResponseEntity.ok(
            bankService.getAllTransactions()
        );
    }


    @PostMapping(
        value = "/api/transfer",
        consumes = "application/json"
    )
    @ResponseBody
    public ResponseEntity<?> transferMoneyApi(
            @Valid @RequestBody TransferRequest request,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(
                validationErrors(result)
            );
        }

        try {

            bankService.transfer(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                request.isSimulateFailure()
            );

            return ResponseEntity.ok(
                Map.of("message", "Transfer completed successfully.")
            );

        } catch (Exception e) {

            recordFailedTransfer(request, e);

            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/api/deposit")
    @ResponseBody
    public ResponseEntity<?> depositApi(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount) {

        try {

            bankService.deposit(accountNumber, amount);

            return ResponseEntity.ok(
                Map.of("message", "Deposit successful!")
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/api/withdraw")
    @ResponseBody
    public ResponseEntity<?> withdrawApi(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount) {

        try {

            bankService.withdraw(accountNumber, amount);

            return ResponseEntity.ok(
                Map.of("message", "Withdrawal successful!")
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/api/accounts/{accountNumber}/history")
    @ResponseBody
    public ResponseEntity<?> accountHistoryApi(
            @PathVariable String accountNumber) {

        return ResponseEntity.ok(
            bankService.getAccountHistory(accountNumber)
        );
    }




    private void recordFailedTransfer(
            TransferRequest request, Exception e) {

        auditService.recordFailure(
            request.getFromAccount(),
            request.getToAccount(),
            request.getAmount(),
            e.getMessage()
        );
    }


    private Map<String, Object> validationErrors(
            BindingResult result) {

        Map<String, Object> errors = new java.util.LinkedHashMap<>();

        errors.put("message", "Validation failed");

        errors.put(
            "errors",
            result.getFieldErrors().stream()
                .map(error ->
                    error.getField() + ": " + error.getDefaultMessage()
                )
                .toList()
        );

        return errors;
    }

}
