package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private AccountUserRepository accountUserRepository;
  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private TransactionService transactionService;

  @Test
  void useBalanceSuccess() {
    // given
    AccountUser user = AccountUser.builder().id(12L).name("pobi").build();

    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(account));

    given(transactionRepository.save(any()))
      .willReturn(Transaction.builder()
        .account(account)
        .transactionType(USE)
        .transactionResultType(S)
        .transactionId("tsId")
        .transactedAt(LocalDateTime.now())
        .amount(6000L)
        .balanceSnapshot(4000L)
        .build());

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    // when
    TransactionDto transactionDto = transactionService
      .useBalance(1L, "1234567890", 2800L);

    // then
    verify(transactionRepository, times(1)).save(captor.capture());
    assertEquals(2800L, captor.getValue().getAmount());
    assertEquals(7200L, captor.getValue().getBalanceSnapshot());
    assertEquals(USE, transactionDto.getTransactionType());
    assertEquals(S, transactionDto.getTransactionResultType());
    assertEquals(4000L, transactionDto.getBalanceSnapshot());
  }

  @Test
  @DisplayName("해당 유저 없음 - 잔액 사용 실패")
  void useBalance_UserNotFound() {
    // given
    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.useBalance(1L, "1234567890", 200L));

    // then
    assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
  void useBalance_AccountNotFound() {
    // given
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.useBalance(1L, "1234567890", 200L));

    // then
    assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("계좌 소유주가 다름 - 잔액 사용 실패")
  void useBalance_userAccountUnMatch() {
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
      () -> transactionService.useBalance(1L, "1234567890", 200L));


    // then
    assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
  }

  @Test
  @DisplayName("계좌이 이미 해지됨 - 잔액 사용 실패")
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
      () -> transactionService.useBalance(1L, "1234567890", 200L));

    // then
    assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
  }
  @Test
  @DisplayName("거래금액이 잔액보다 큰 경우")
  void useBalance_amountExceedBalance() {
    // given
    AccountUser user = AccountUser.builder().id(12L).name("pobi").build();

    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .accountNumber("1234567890")
      .balance(1000L)
      .build();

    given(accountUserRepository.findById(anyLong()))
      .willReturn(Optional.of(user));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(account));

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.useBalance(1L, "1234567890", 2000L));


    // then
    assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
  }

  @Test
  @DisplayName("실패 트랜잭션 저장 성공")
  void saveFailedTransaction() {
    // given
    AccountUser user = AccountUser.builder().id(12L).name("pobi").build();

    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(account));

    given(transactionRepository.save(any()))
      .willReturn(Transaction.builder()
        .account(account)
        .transactionType(USE)
        .transactionResultType(S)
        .transactionId("tsId")
        .transactedAt(LocalDateTime.now())
        .amount(6000L)
        .balanceSnapshot(4000L)
        .build());

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    // when
    transactionService.saveFailedTransaction( "1234567890", 2800L);

    // then
    verify(transactionRepository, times(1)).save(captor.capture());
    assertEquals(2800L, captor.getValue().getAmount());
    assertEquals(10000L, captor.getValue().getBalanceSnapshot());
    assertEquals(USE, captor.getValue().getTransactionType());
    assertEquals(F, captor.getValue().getTransactionResultType());
  }

  @Test
  void cancelBalanceSuccess() {
    // given
    AccountUser user = AccountUser.builder().id(12L).name("pobi").build();

    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();

    Transaction transaction = Transaction.builder()
      .account(account)
      .amount(2000L)
      .transactedAt(LocalDateTime.now())
      .build();

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.of(transaction));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(account));

    given(transactionRepository.save(any()))
      .willReturn(transaction);

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    // when
    TransactionDto transactionDto = transactionService
      .cancelBalance("aaa", "1000000000", 2000L);

    // then
    verify(transactionRepository, times(1)).save(captor.capture());
    assertEquals(2000L, captor.getValue().getAmount());
    assertEquals(10000L + 2000L, captor.getValue().getBalanceSnapshot());
    assertEquals(CANCEL, captor.getValue().getTransactionType());
    assertEquals(S, captor.getValue().getTransactionResultType());
  }

  @Test
  @DisplayName("거래 없음 - 잔액 사용 취소 실패")
  void cancelBalance_transactionNotFound() {
    // given

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.cancelBalance("asdf", "1234567890", 200L));

    // then
    assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
  void cancelBalance_AccountNotFound() {
    // given
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();
    Transaction transaction = Transaction.builder()
      .account(account)
      .amount(2000L)
      .transactedAt(LocalDateTime.now())
      .build();

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.of(transaction));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.cancelBalance("asdf", "1234567890", 200L));

    // then
    assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("거래 계좌 불일치 - 잔액 사용 취소 실패")
  void cancelBalance_transactionAccountUnMatch() {
    // given
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();
    Transaction transaction = Transaction.builder()
      .account(account)
      .amount(2000L)
      .transactedAt(LocalDateTime.now())
      .build();

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.of(transaction));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(Account.builder()
        .id(323L)
        .build()));

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.cancelBalance("asdf", "1234567890", 200L));

    // then
    assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
  }

  @Test
  @DisplayName("부분 취소 불가능 - 잔액 사용 취소 실패")
  void cancelBalance_cancelMushFully() {
    // given
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();
    Transaction transaction = Transaction.builder()
      .account(account)
      .amount(2000L)
      .transactedAt(LocalDateTime.now())
      .build();

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.of(transaction));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(account));

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.cancelBalance("asdf", "1234567890", 1500L));

    // then
    assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
  }

  @Test
  @DisplayName("1년 이상된 거래 취소 불가능 - 잔액 사용 취소 실패")
  void cancelBalance_tooOldOrderToCancel() {
    // given
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();
    Transaction transaction = Transaction.builder()
      .account(account)
      .amount(2000L)
      .transactedAt(LocalDateTime.now().minusYears(2))
      .build();

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.of(transaction));

    given(accountRepository.findByAccountNumber(anyString()))
      .willReturn(Optional.of(account));

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.cancelBalance("asdf", "1234567890", 2000L));

    // then
    assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
  }

  @Test
  void queryTransactionSuccess() {
    // given
    AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
    Account account = Account.builder()
      .accountUser(user)
      .accountStatus(IN_USE)
      .balance(10000L)
      .accountNumber("1000000000")
      .build();
    Transaction transaction = Transaction.builder()
      .transactionId("aaa")
      .transactionType(USE)
      .transactionResultType(S)
      .account(account)
      .amount(2000L)
      .balanceSnapshot(8000L)
      .transactedAt(LocalDateTime.now().minusYears(2))
      .build();

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.of(transaction));

    // when
    TransactionDto transactionDto = transactionService.queryTransaction("aaa");

    // then
    assertEquals("aaa", transactionDto.getTransactionId());
    assertEquals(USE, transactionDto.getTransactionType());
    assertEquals(S, transactionDto.getTransactionResultType());
    assertEquals(account, transaction.getAccount());
    assertEquals(10000L-2000L, transactionDto.getBalanceSnapshot());
  }

  @Test
  @DisplayName("거래 없음 - 잔액 사용 취소 실패")
  void queryTransaction_transactionNotFound() {
    // given

    given(transactionRepository.findByTransactionId(anyString()))
      .willReturn(Optional.empty());

    // when
    AccountException exception = assertThrows(AccountException.class,
      () -> transactionService.queryTransaction("asdf"));

    // then
    assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
  }
}