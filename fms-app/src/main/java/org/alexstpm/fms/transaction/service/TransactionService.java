package org.alexstpm.fms.transaction.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.account.model.Account;
import org.alexstpm.fms.account.repository.AccountRepository;
import org.alexstpm.fms.audit.model.Action;
import org.alexstpm.fms.audit.model.Audit;
import org.alexstpm.fms.audit.repository.AuditRepository;
import org.alexstpm.fms.category.model.Category;
import org.alexstpm.fms.category.model.CategoryType;
import org.alexstpm.fms.category.repository.CategoryRepository;
import org.alexstpm.fms.counterparty.model.Counterparty;
import org.alexstpm.fms.counterparty.repository.CounterpartyRepository;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.transaction.dto.CreateTransactionRequest;
import org.alexstpm.fms.transaction.dto.TransactionResponse;
import org.alexstpm.fms.transaction.model.Transaction;
import org.alexstpm.fms.transaction.model.TransactionStatus;
import org.alexstpm.fms.transaction.model.TransactionType;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.alexstpm.fms.user.model.Role;
import org.alexstpm.fms.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final AuditRepository auditRepository;
    private final AccessService accessService;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public TransactionResponse createDraft(CreateTransactionRequest request) {
        User author = accessService.currentUser();

        Account from = findAccount(request.accountFromId());
        Account to = findAccount(request.accountToId());
        Category category = findCategory(request.categoryId());
        Counterparty counterparty = findCounterparty(request.counterpartyId());

        validateTransaction(request, from, to, category, counterparty);
        checkDepartmentAccess(author, from, to);

        Transaction tx = new Transaction();
        tx.setType(request.transactionType());
        tx.setAmount(request.amount());
        tx.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        tx.setDate(request.date());
        tx.setAccountFrom(from);
        tx.setAccountTo(to);
        tx.setCategory(category);
        tx.setCounterparty(counterparty);
        tx.setStatus(TransactionStatus.DRAFT);
        tx.setAuthor(author);

        Transaction saved = transactionRepository.save(tx);
        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.TRANSACTION_CREATED);
        audit.setEntityType("TRANSACTION");
        audit.setEntityId(saved.getId());
        audit.setOccurredAt(Instant.now());
        audit.setDetails("Draft transaction created");
        auditRepository.save(audit);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER','ACCOUNTANT')")
    public TransactionResponse confirmTransaction(Long id) {
        User author = accessService.currentUser();
        Transaction tx = transactionRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction with id = " + id + " not found"));
        checkDepartmentAccess(author, tx.getAccountFrom(), tx.getAccountTo());
        if (!tx.getStatus().equals(TransactionStatus.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot confirm non draft transaction");
        }
        tx.setStatus(TransactionStatus.CONFIRMED);
        Transaction saved = transactionRepository.save(tx);
        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.TRANSACTION_CONFIRMED);
        audit.setEntityType("TRANSACTION");
        audit.setEntityId(tx.getId());
        audit.setOccurredAt(Instant.now());
        audit.setDetails("Transaction confirmed");
        auditRepository.save(audit);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER','ACCOUNTANT')")
    public TransactionResponse reverseTransaction(Long id) {
        Transaction tx = transactionRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction with id = " + id + " not found"));
        User author = accessService.currentUser();
        checkDepartmentAccess(author, tx.getAccountFrom(), tx.getAccountTo());
        if (transactionRepository.existsByReversalOf_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Reverse transaction for transaction with id = " + id + " already exists");
        }
        if (!tx.getStatus().equals(TransactionStatus.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reverse not confirmed transaction");
        }
        Transaction newTx = new Transaction();
        newTx.setReversalOf(tx);
        switch (tx.getType()) {
            case INCOME -> {
                newTx.setType(TransactionType.EXPENSE);
            }
            case EXPENSE -> {
                newTx.setType(TransactionType.INCOME);
            }
            default -> {
                newTx.setType(TransactionType.TRANSFER);
            }
        }
        newTx.setAuthor(author);
        newTx.setAmount(tx.getAmount());
        newTx.setDate(LocalDate.now());
        newTx.setAccountFrom(tx.getAccountTo());
        newTx.setAccountTo(tx.getAccountFrom());
        newTx.setCurrency(tx.getCurrency());
        newTx.setCategory(null);
        newTx.setStatus(TransactionStatus.CONFIRMED);
        tx.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(tx);
        Transaction savedReversalTx = transactionRepository.save(newTx);
        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.TRANSACTION_REVERSED);
        audit.setEntityType("TRANSACTION");
        audit.setEntityId(tx.getId());
        audit.setOccurredAt(Instant.now());
        audit.setDetails("Transaction with id = " + id + " reversed");
        auditRepository.save(audit);
        return toResponse(savedReversalTx);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        User user = accessService.currentUser();
        Transaction transaction;
        if (accessService.isGlobalReader(user)) {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Transaction with id = " + id + " not found"));
        } else {
            Set<Long> departmentIds = membershipRepository
                    .findAllByUser_Id(user.getId())
                    .stream()
                    .map(membership -> membership.getDepartment().getId())
                    .collect(Collectors.toSet());
            if (departmentIds.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction with id = " + id + " not found");
            }
            transaction = transactionRepository
                    .findVisibleByIdToDepartments(id, departmentIds)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Transaction with id = " + id + " not found"));
        }
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Pageable pageable) {
        User user = accessService.currentUser();
        if (accessService.isGlobalReader(user)) {
            return transactionRepository.findAll(pageable).map(this::toResponse);
        }
        Set<Long> departmentIds = membershipRepository.findAllByUser_Id(user.getId())
                .stream()
                .map(membership -> membership.getDepartment().getId())
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return transactionRepository
                .findVisibleToDepartments(departmentIds, pageable)
                .map(this::toResponse);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public TransactionResponse updateDraft(
            Long id,
            CreateTransactionRequest request) {
        User author = accessService.currentUser();
        Transaction tx = transactionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction with id = " + id + " not found"));

        checkDepartmentAccess(author, tx.getAccountFrom(), tx.getAccountTo());
        if (tx.getStatus() != TransactionStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft transactions can be updated");
        }
        Account from = findAccount(request.accountFromId());
        Account to = findAccount(request.accountToId());
        Category category = findCategory(request.categoryId());
        Counterparty counterparty = findCounterparty(request.counterpartyId());
        validateTransaction(request, from, to, category, counterparty);
        checkDepartmentAccess(author, from, to);
        tx.setType(request.transactionType());
        tx.setAmount(request.amount());
        tx.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        tx.setDate(request.date());
        tx.setAccountFrom(from);
        tx.setAccountTo(to);
        tx.setCategory(category);
        tx.setCounterparty(counterparty);
        return toResponse(transactionRepository.save(tx));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void deleteTransaction(Long id) {
        User author = accessService.currentUser();
        Transaction tx = transactionRepository.findByIdForUpdate(id).orElse(null);
        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.TRANSACTION_DELETE_ATTEMPT);
        audit.setEntityType("TRANSACTION");
        audit.setEntityId(id);
        audit.setOccurredAt(Instant.now());
        if (tx == null) {
            audit.setDetails("Delete attempt for a transaction that does not exist");
            auditRepository.save(audit);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transaction with id = " + id + " not found");
        }
        audit.setDetails(
                "Delete attempt; role=" + author.getRole()
                        + ", transaction status=" + tx.getStatus());
        auditRepository.save(audit);
        if (author.getRole() != Role.ADMIN && author.getRole() != Role.ACCOUNTANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not allowed to delete transactions");
        }
        checkDepartmentAccess(author, tx.getAccountFrom(), tx.getAccountTo());
        if (tx.getStatus() != TransactionStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft transactions can be deleted");
        }
        audit.setDetails("Draft transaction deleted");
        transactionRepository.delete(tx);
    }

    private Account findAccount(Long id) {
        if (id == null) {
            return null;
        }
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account with id = " + id + " not found"));
    }

    private Category findCategory(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category with id = " + id + " not found"));
    }

    private Counterparty findCounterparty(Long id) {
        if (id == null) {
            return null;
        }
        return counterpartyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Counterparty with id = " + id + " not found"));
    }

    private void validateTransaction(
            CreateTransactionRequest request,
            Account from,
            Account to,
            Category category,
            Counterparty counterparty
    ) {
        switch (request.transactionType()) {
            case EXPENSE -> {
                if (from == null || to != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Expense requires source account only");
                }
                if (category == null || category.getType() != CategoryType.EXPENSE) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Expense requires an expense category");
                }
                if (!from.getCurrency().equalsIgnoreCase(request.currency())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Currency mismatch");
                }
            }
            case INCOME -> {
                if (to == null || from != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Income requires destination account only");
                }
                if (category == null || category.getType() != CategoryType.INCOME) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Income requires an income category");
                }
                if (!to.getCurrency().equalsIgnoreCase(request.currency())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Currency mismatch");
                }
            }
            case TRANSFER -> {
                if (from == null || to == null || from.getId().equals(to.getId())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Transfer requires two different accounts");
                }
                if (category != null || counterparty != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Transfer must not have category or counterparty");
                }
                if (!from.getCurrency().equalsIgnoreCase(to.getCurrency()) ||
                        !from.getCurrency().equalsIgnoreCase(request.currency())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Currency mismatch");
                }
            }
        }
    }

    private void checkDepartmentAccess(User user, Account from, Account to) {
        checkOneAccountAccess(user, from);
        checkOneAccountAccess(user, to);
    }

    private void checkOneAccountAccess(User user, Account account) {
        if (account == null || accessService.isGlobalReader(user)) {
            return;
        }
        accessService.requireDepartmentRead(user, account.getDepartment().getId());
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getDate(),
                tx.getAccountFrom() == null ? null : tx.getAccountFrom().getId(),
                tx.getAccountTo() == null ? null : tx.getAccountTo().getId(),
                tx.getCategory() == null ? null : tx.getCategory().getId(),
                tx.getCounterparty() == null ? null : tx.getCounterparty().getId(),
                tx.getStatus()
        );
    }
}
