package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;
  private final AccountUserRepository accountUserRepository;

  @Transactional
  public AccountDto createAccount(Long userId, Long initialBalance) {
    AccountUser accountUser = getAccountUser(userId);

    validateCreateAccount(accountUser);

    String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
      .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
      .orElse("1000000000");

    return AccountDto.from(accountRepository.save(
      Account.builder()
        .accountUser(accountUser)
        .accountStatus(IN_USE)
        .accountNumber(newAccountNumber)
        .balance(initialBalance)
        .registeredAt(LocalDateTime.now())
        .build()
    ));
  }

  private AccountUser getAccountUser(Long userId) {
    AccountUser accountUser = accountUserRepository.findById(userId)
      .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    return accountUser;
  }

  private void validateCreateAccount(AccountUser accountUser) {
    if (accountRepository.countByAccountUser(accountUser) >= 10) {
      throw new AccountException(MAX_COUNT_PER_USER_10);
    }
  }

  @Transactional
  public AccountDto deleteAccount(Long userId, String accountNumber) {
    AccountUser accountUser = getAccountUser(userId);

    Account account = accountRepository.findByAccountNumber(accountNumber)
      .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    validateDeleteAccount(account, accountUser);

    account.setAccountStatus(UNREGISTERED);
    account.setUnRegisteredAt(LocalDateTime.now());

    accountRepository.save(account);

    return AccountDto.from(account);
  }

  private void validateDeleteAccount(Account account, AccountUser accountUser) {
    if (account.getAccountUser().getId() != accountUser.getId()) {
      throw new AccountException(USER_ACCOUNT_UN_MATCH);
    }

    if (account.getAccountStatus() == UNREGISTERED) {
      throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
    }

    if (account.getBalance() > 0) {
      throw new AccountException(BALANCE_NOT_EMPTY);
    }
  }


  @Transactional
  public Account getAccount(Long id) {
    if (id < 0) {
      throw new RuntimeException("Minus");
    }

    return accountRepository.findById(id).get();
  }

  @Transactional
  public List<AccountDto> getAccountsByUserId(Long userId) {
    AccountUser accountUser = getAccountUser(userId);

    List<Account> accounts = accountRepository.findByAccountUser(accountUser);

    return accounts.stream().map(AccountDto::from).collect(Collectors.toList());
  }
}
