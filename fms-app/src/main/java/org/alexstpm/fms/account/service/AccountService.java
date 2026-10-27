package org.alexstpm.fms.account.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.account.dto.AccountResponse;
import org.alexstpm.fms.account.dto.CreateAccountRequest;
import org.alexstpm.fms.account.model.Account;
import org.alexstpm.fms.account.repository.AccountRepository;
import org.alexstpm.fms.department.model.Department;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.department.repository.DepartmentRepository;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.alexstpm.fms.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DepartmentMembershipRepository departmentMembershipRepository;
    private final DepartmentRepository departmentRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts() {
        User user = accessService.currentUser();
        if (accessService.isGlobalReader(user)) {
            return accountRepository.findAll()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } else {
            return departmentMembershipRepository.findAllByUser_Id(user.getId())
                    .stream()
                    .map(d -> accountRepository.findAllByDepartment_Id(d.getDepartment().getId())
                            .stream()
                            .map(this::toResponse).toList())
                    .flatMap(List::stream)
                    .toList();
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account account = findAccount(id);
        accessService.requireDepartmentRead(
                accessService.currentUser(), account.getDepartment().getId());
        return toResponse(account);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse createAccount(CreateAccountRequest request) {
        Department department = findDepartment(request.departmentId());
        Account newAccount = new Account();
        newAccount.setType(request.type());
        newAccount.setCurrency(normalizeCurrency(request.currency()));
        newAccount.setDepartment(department);
        Account saved = accountRepository.save(newAccount);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse updateAccount(Long id, CreateAccountRequest request) {
        Account account = findAccount(id);
        User user = accessService.currentUser();
        accessService.requireDepartmentRead(user, account.getDepartment().getId());

        Department department = findDepartment(request.departmentId());
        accessService.requireDepartmentRead(user, department.getId());

        boolean hasTransactions = transactionRepository.existsByAccountId(id);
        String currency = normalizeCurrency(request.currency());
        if (hasTransactions) {
            if (!account.getCurrency().equalsIgnoreCase(currency)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Currency cannot be changed for an account with transactions");
            }
            if (!account.getDepartment().getId().equals(department.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Department cannot be changed for an account with transactions");
            }
        }

        account.setType(request.type());
        account.setCurrency(currency);
        account.setDepartment(department);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAccount(Long id) {
        Account account = findAccount(id);
        accessService.requireDepartmentRead(
                accessService.currentUser(), account.getDepartment().getId());
        if (transactionRepository.existsByAccountId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account is used by transactions and cannot be deleted");
        }
        accountRepository.delete(account);
    }

    private Department findDepartment(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department with id = " + id + " not found"));
    }

    private Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account with id = " + id + " not found"));
    }

    private String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal balanceOf(Account account) {
        BigDecimal balance = transactionRepository.calculateAccountBalance(account.getId());
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getType(),
                account.getCurrency(),
                account.getDepartment().getId(),
                balanceOf(account));
    }
}
