package org.alexstpm.fms.transaction.service;

import org.alexstpm.fms.account.model.Account;
import org.alexstpm.fms.account.repository.AccountRepository;
import org.alexstpm.fms.audit.model.Action;
import org.alexstpm.fms.audit.model.Audit;
import org.alexstpm.fms.audit.repository.AuditRepository;
import org.alexstpm.fms.category.model.Category;
import org.alexstpm.fms.category.model.CategoryType;
import org.alexstpm.fms.category.repository.CategoryRepository;
import org.alexstpm.fms.counterparty.repository.CounterpartyRepository;
import org.alexstpm.fms.department.model.Department;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CounterpartyRepository counterpartyRepository;
    @Mock
    private DepartmentMembershipRepository membershipRepository;
    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AccessService accessService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createsDraftAndWritesAuditRecord() {
        // given
        User accountant = accountant();
        Account account = account(10L);
        Category category = expenseCategory(20L);
        CreateTransactionRequest request = expenseRequest("12.00", account.getId(), category.getId());

        when(accessService.currentUser()).thenReturn(accountant);
        when(accessService.isGlobalReader(accountant)).thenReturn(true);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction saved = invocation.getArgument(0);
                    saved.setId(99L);
                    return saved;
                });

        // when
        TransactionResponse response = transactionService.createDraft(request);

        // then
        assertEquals(99L, response.id());
        assertEquals(TransactionStatus.DRAFT, response.status());
        ArgumentCaptor<Audit> auditCaptor = ArgumentCaptor.forClass(Audit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals(Action.TRANSACTION_CREATED, auditCaptor.getValue().getAction());
    }

    @Test
    void updatesDraftTransaction() {
        // given
        Long transactionId = 41L;
        User accountant = accountant();
        Account account = account(10L);
        Category category = expenseCategory(20L);
        Transaction draft = transaction(transactionId, TransactionStatus.DRAFT, account);
        CreateTransactionRequest request = expenseRequest("12.00", account.getId(), category.getId());

        when(accessService.currentUser()).thenReturn(accountant);
        when(accessService.isGlobalReader(accountant)).thenReturn(true);
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(draft));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        TransactionResponse response = transactionService.updateDraft(transactionId, request);

        // then
        assertEquals(new BigDecimal("12.00"), response.amount());
        assertEquals(TransactionStatus.DRAFT, response.status());
        verify(transactionRepository).save(draft);
    }

    @Test
    void rejectsUpdateOfConfirmedTransactionWithoutSavingIt() {
        // given
        Long transactionId = 41L;
        User accountant = accountant();
        Account account = account(10L);
        Transaction confirmed = transaction(transactionId, TransactionStatus.CONFIRMED, account);
        CreateTransactionRequest request = expenseRequest("12.00", account.getId(), 20L);

        when(accessService.currentUser()).thenReturn(accountant);
        when(accessService.isGlobalReader(accountant)).thenReturn(true);
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(confirmed));

        // when
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.updateDraft(transactionId, request));

        // then
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(new BigDecimal("50.00"), confirmed.getAmount());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void reversesConfirmedExpenseAndWritesAuditRecord() {
        // given
        Long transactionId = 41L;
        User accountant = accountant();
        Account source = account(10L);
        Transaction original = transaction(transactionId, TransactionStatus.CONFIRMED, source);

        when(accessService.currentUser()).thenReturn(accountant);
        when(accessService.isGlobalReader(accountant)).thenReturn(true);
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(original));
        when(transactionRepository.existsByReversalOf_Id(transactionId)).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        TransactionResponse reversal = transactionService.reverseTransaction(transactionId);

        // then
        assertEquals(TransactionStatus.REVERSED, original.getStatus());
        assertEquals(TransactionType.INCOME, reversal.type());
        assertEquals(new BigDecimal("50.00"), reversal.amount());
        assertNull(reversal.accountFromId());
        assertEquals(source.getId(), reversal.accountToId());
        assertEquals(TransactionStatus.CONFIRMED, reversal.status());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        Transaction savedReversal = transactionCaptor.getAllValues().get(1);
        assertEquals(original, savedReversal.getReversalOf());

        ArgumentCaptor<Audit> auditCaptor = ArgumentCaptor.forClass(Audit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals(Action.TRANSACTION_REVERSED, auditCaptor.getValue().getAction());
    }

    @Test
    void recordsDeleteAttemptEvenWhenAuditorIsForbidden() {
        // given
        Long transactionId = 41L;
        User auditor = User.builder()
                .id(8L)
                .email("auditor@example.test")
                .role(Role.AUDITOR)
                .enabled(true)
                .build();
        Transaction confirmed = transaction(
                transactionId, TransactionStatus.CONFIRMED, account(10L));

        when(accessService.currentUser()).thenReturn(auditor);
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(confirmed));

        // when
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.deleteTransaction(transactionId));

        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        ArgumentCaptor<Audit> auditCaptor = ArgumentCaptor.forClass(Audit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals(Action.TRANSACTION_DELETE_ATTEMPT, auditCaptor.getValue().getAction());
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void rejectsSecondReversal() {
        // given
        Long transactionId = 41L;
        User accountant = accountant();
        Transaction original = transaction(transactionId, TransactionStatus.CONFIRMED, account(10L));

        when(accessService.currentUser()).thenReturn(accountant);
        when(accessService.isGlobalReader(accountant)).thenReturn(true);
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(original));
        when(transactionRepository.existsByReversalOf_Id(transactionId)).thenReturn(true);

        // when
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.reverseTransaction(transactionId));

        // then
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private static User accountant() {
        return User.builder()
                .id(7L)
                .email("accountant@example.test")
                .role(Role.ACCOUNTANT)
                .enabled(true)
                .build();
    }

    private static Account account(Long id) {
        Department department = new Department();
        department.setId(3L);

        Account account = new Account();
        account.setId(id);
        account.setCurrency("USD");
        account.setDepartment(department);
        return account;
    }

    private static Category expenseCategory(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setType(CategoryType.EXPENSE);
        return category;
    }

    private static Transaction transaction(Long id, TransactionStatus status, Account account) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setCurrency("USD");
        transaction.setDate(LocalDate.of(2026, 10, 1));
        transaction.setAccountFrom(account);
        transaction.setStatus(status);
        return transaction;
    }

    private static CreateTransactionRequest expenseRequest(String amount, Long accountId, Long categoryId) {
        return new CreateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal(amount),
                "USD",
                LocalDate.of(2026, 10, 2),
                accountId,
                null,
                categoryId,
                null);
    }
}
