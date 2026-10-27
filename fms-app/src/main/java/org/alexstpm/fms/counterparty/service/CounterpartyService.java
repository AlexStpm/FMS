package org.alexstpm.fms.counterparty.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.counterparty.dto.CounterpartyResponse;
import org.alexstpm.fms.counterparty.dto.SaveCounterpartyRequest;
import org.alexstpm.fms.counterparty.model.Counterparty;
import org.alexstpm.fms.counterparty.repository.CounterpartyRepository;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CounterpartyService {
    private final CounterpartyRepository counterpartyRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<CounterpartyResponse> getCounterparties(Pageable pageable) {
        return counterpartyRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CounterpartyResponse getCounterpartyById(Long id) {
        return toResponse(findCounterparty(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER')")
    public CounterpartyResponse createCounterparty(SaveCounterpartyRequest request) {
        Counterparty counterparty = new Counterparty();
        apply(counterparty, request);
        return toResponse(counterpartyRepository.save(counterparty));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER')")
    public CounterpartyResponse updateCounterparty(Long id, SaveCounterpartyRequest request) {
        Counterparty counterparty = findCounterparty(id);
        apply(counterparty, request);
        return toResponse(counterpartyRepository.save(counterparty));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER')")
    public void deleteCounterparty(Long id) {
        Counterparty counterparty = findCounterparty(id);

        if (transactionRepository.existsByCounterparty_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Counterparty is used by transactions and cannot be deleted");
        }
        counterpartyRepository.delete(counterparty);
    }

    private void apply(Counterparty counterparty, SaveCounterpartyRequest request) {
        counterparty.setName(request.name().trim());
        counterparty.setTaxId(request.taxId().trim());
        counterparty.setType(request.type());
    }

    private Counterparty findCounterparty(Long id) {
        if (id == null) {
            return null;
        }
        return counterpartyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Counterparty with id = " + id + " not found"));
    }

    private CounterpartyResponse toResponse(Counterparty counterparty) {
        return new CounterpartyResponse(
                counterparty.getId(),
                counterparty.getName(),
                counterparty.getTaxId(),
                counterparty.getType());
    }
}