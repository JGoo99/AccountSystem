package com.example.account.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class AccountInfo {
  private String accountNumber;
  private Long balance;
}
