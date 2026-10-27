package org.alexstpm.fms.transaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.transaction.dto.CreateTransactionRequest;
import org.alexstpm.fms.transaction.dto.TransactionResponse;
import org.alexstpm.fms.transaction.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return transactionService.getById(id);
    }

    @GetMapping
    public Page<TransactionResponse> list(
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return transactionService.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createDraft(request);
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.OK)
    public TransactionResponse confirm(@PathVariable Long id) {

        return transactionService.confirmTransaction(id);
    }

    @PostMapping("/{id}/reverse")
    @ResponseStatus(HttpStatus.OK)
    public TransactionResponse reverse(@PathVariable Long id) {

        return transactionService.reverseTransaction(id);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.updateDraft(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }
}
