package org.alexstpm.fms.counterparty.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.counterparty.dto.CounterpartyResponse;
import org.alexstpm.fms.counterparty.dto.SaveCounterpartyRequest;
import org.alexstpm.fms.counterparty.service.CounterpartyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {
    private final CounterpartyService counterpartyService;

    @GetMapping
    public Page<CounterpartyResponse> getAll(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return counterpartyService.getCounterparties(pageable);
    }

    @GetMapping("/{id}")
    public CounterpartyResponse getCounterpartyById(@PathVariable Long id) {
        return counterpartyService.getCounterpartyById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CounterpartyResponse createCounterparty(
            @Valid @RequestBody SaveCounterpartyRequest request) {
        return counterpartyService.createCounterparty(request);
    }

    @PutMapping("/{id}")
    public CounterpartyResponse updateCounterparty(
            @PathVariable Long id,
            @Valid @RequestBody SaveCounterpartyRequest request) {
        return counterpartyService.updateCounterparty(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCounterparty(@PathVariable Long id) {
        counterpartyService.deleteCounterparty(id);
    }
}