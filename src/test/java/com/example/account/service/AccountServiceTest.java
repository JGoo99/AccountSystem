package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private AccountUserRepository accountUserRepository;
  @InjectMocks
  private AccountService accountService;

  @Test
  void createAccountSuccess() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findFirstByOrderByIdDesc())
      .willReturn(Optional.of(Account.builder()
        .accountNumber("1000000012")
        .build()));

    given(accountRepository.save(any()))
      .willReturn(Account.builder()
        .accountUser(user)
        .accountNumber("1000000013")
        .build());

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

    // when
    AccountDto accountDto = accountService.createAccount(1L, 100L);

    // then
    verify(accountRepository, times(1)).save(captor.capture());
    assertEquals(12L, accountDto.getUserId());
    assertEquals("1000000013", captor.getValue().getAccountNumber());
  }

  @Test
  void createFirstAccount() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findFirstByOrderByIdDesc())
      .willReturn(Optional.empty());

    given(accountRepository.save(any()))
      .willReturn(Account.builder()
        .accountUser(user)
        .accountNumber("1000000015")
        .build());

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

    // when
    AccountDto accountDto = accountService.createAccount(1L, 100L);

    // then
    verify(accountRepository, times(1)).save(captor.capture());
    assertEquals(12L, accountDto.getUserId());
    assertEquals("1000000000", captor.getValue().getAccountNumber());
  }

  @Test
  void deleteAccountSuccess() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(
        Account.builder()
          .accountUser(user)
          .accountNumber("1000000012")
          .balance(0L)
          .build())
      );

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

    // when
    AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

    // then
    verify(accountRepository, times(1)).save(captor.capture());
    assertEquals(12L, accountDto.getUserId());
    assertEquals("1000000012", captor.getValue().getAccountNumber());
    assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
  }

  @Test
  void getAccountSuccess() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    List<Account> accounts = Arrays.asList(
      Account.builder()
        .accountUser(user)
        .accountNumber("1234567890")
        .balance(100L)
        .build(),
      Account.builder()
        .accountUser(user)
        .accountNumber("0987654321")
        .balance(200L)
        .build(),
      Account.builder()
        .accountUser(user)
        .accountNumber("1000000000")
        .balance(300L)
        .build()
    );

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountUser(any()))
      .willReturn(accounts);

    // when
    List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

    // then
    assertEquals(3, accountDtos.size());

    assertEquals("1234567890", accountDtos.get(0).getAccountNumber());
    assertEquals(100L, accountDtos.get(0).getBalance());

    assertEquals("0987654321", accountDtos.get(1).getAccountNumber());
    assertEquals(200L, accountDtos.get(1).getBalance());

    assertEquals("1000000000", accountDtos.get(2).getAccountNumber());
    assertEquals(300L, accountDtos.get(2).getBalance());
  }

  @Test
  @DisplayName("해당 유저 없음 - 계좌 생성 실패")
  void createAccount_UserNotFound() {
    // given
    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.createAccount(1L, 100L));

    // then
    assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("유저 당 최대 계좌 오버됨 - 계좌 생성 실패")
  void createAccount_maxAccountIs10() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.countByAccountUser(any()))
      .willReturn(10);

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.createAccount(1L, 100L));

    // then
    assertEquals(ErrorCode.MAX_COUNT_PER_USER_10, exception.getErrorCode());

  }

  @Test
  @DisplayName("해당 유저 없음 - 계좌 해지 실패")
  void deleteAccount_UserNotFound() {
    // given
    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.deleteAccount(1L, "1234567890"));

    // then
    assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
  void deleteAccount_AccountNotFound() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.deleteAccount(1L, "1234567890"));

    // then
    assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("계좌 소유주가 다름 - 계좌 해지 실패")
  void deleteAccount_userAccountUnMatch() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);
    AccountUser user2 = AccountUser.builder().name("Rupi").build();
    user2.setId(13L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(
        Account.builder()
          .accountUser(user2)
          .accountNumber("1000000012")
          .balance(0L)
          .build())
      );

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.deleteAccount(1L, "1234567890"));


    // then
    assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
  }

  @Test
  @DisplayName("계좌 잔액 남아있음 - 계좌 해지 실패")
  void deleteAccount_balanceNotEmpty() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(
        Account.builder()
          .accountUser(user)
          .accountNumber("1000000012")
          .balance(1000L)
          .build())
      );

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.deleteAccount(1L, "1234567890"));

    // then
    assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
  }

  @Test
  @DisplayName("계좌이 이미 해지됨 - 계좌 해지 실패")
  void deleteAccount_alreadyUnregistered() {
    // given
    AccountUser user = AccountUser.builder().name("Pobi").build();
    user.setId(12L);

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(
        Account.builder()
          .accountUser(user)
          .accountNumber("1000000012")
          .balance(0L)
          .accountStatus(AccountStatus.UNREGISTERED)
          .build())
      );

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.deleteAccount(1L, "1234567890"));

    // then
    assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
  }

  @Test
  @DisplayName("사용자가 없는 경우 - 계좌 확인 실패")
  void getAccounts_userNotFound() {
    // given
    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> accountService.getAccountsByUserId(1L));

    // then
    assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }
}