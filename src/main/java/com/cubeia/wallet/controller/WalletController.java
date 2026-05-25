package com.cubeia.wallet.controller;

import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.BalanceResponse;
import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.dto.StatsResponse;
import com.cubeia.wallet.dto.TransactionListResponse;
import com.cubeia.wallet.dto.TransactionResponse;
import com.cubeia.wallet.dto.TransferRequest;
import com.cubeia.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        return ResponseEntity.ok(walletService.listAllAccounts());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = walletService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(walletService.getStats());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(walletService.getAccount(accountId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(walletService.getBalance(accountId));
    }

    @PostMapping("/{accountId}/transfers")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable String accountId,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = walletService.transfer(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionListResponse> listTransactions(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(walletService.listTransactions(accountId, page, size));
    }
}
