package com.example.account.controller;

import com.example.account.dto.CreateAccount;
import com.example.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AccountController {
  private final AccountService accountService;

  @PostMapping("/account")
  public CreateAccount.Response createAccount(
    @RequestBody @Valid CreateAccount.Request request
  ) {

    return CreateAccount.Response.from(
      accountService.createAccount(
        request.getUserId(), request.getInitBalance()));
  }
}
