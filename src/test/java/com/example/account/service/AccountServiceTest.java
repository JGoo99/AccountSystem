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
    AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

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
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();

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
    assertEquals(15L, accountDto.getUserId());
    assertEquals("1000000000", captor.getValue().getAccountNumber());
  }

  @Test
  void deleteAccountSuccess() {
    // given
    AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

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
    AccountUser user = AccountUser.builder()
      .id(15L)
      .name("Pobi").build();

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
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();

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
  void deleteAccount_user_accountUnMatch() {
    // given
    AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
    AccountUser user2 = AccountUser.builder().id(13L).name("Rupi").build();

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
    AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

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
    AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

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
}