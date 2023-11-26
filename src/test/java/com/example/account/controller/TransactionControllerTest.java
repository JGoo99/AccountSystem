package com.example.account.controller;

import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
  @MockBean
  private TransactionService transactionService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void successUseBalance() throws Exception {
    // given
    given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
      .willReturn(
        TransactionDto.builder()
          .accountNumber("1234567890")
          .transactionType(USE)
          .transactionResultType(S)
          .amount(3000L)
          .transactionId("avc")
          .build()
      );

    // when
    // then
    mockMvc.perform(
        post("/transaction/use")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(
            new UseBalance.Request(1L, "1234567890", 3000L)
          ))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accountNumber").value("1234567890"))
      .andExpect(jsonPath("$.transactionResult").value("S"))
      .andExpect(jsonPath("$.transactionId").value("avc"))
      .andExpect(jsonPath("$.amount").value(3000))
      .andDo(print());
  }

  @Test
  void successCancelBalance() throws Exception {

    given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
      .willReturn(
        TransactionDto.builder()
          .accountNumber("1234567890")
          .transactionType(CANCEL)
          .transactionResultType(S)
          .amount(3000L)
          .transactionId("avc")
          .build()
      );

    // when
    // then
    mockMvc.perform(
        post("/transaction/cancel")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(
            new CancelBalance.Request("avc", "1234567890", 500L)
          ))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accountNumber").value("1234567890"))
      .andExpect(jsonPath("$.transactionResult").value("S"))
      .andExpect(jsonPath("$.transactionId").value("avc"))
      .andExpect(jsonPath("$.amount").value(3000))
      .andDo(print());
  }

  @Test
  void successQueryTransaction() throws Exception {
    // given
    given(transactionService.queryTransaction(anyString()))
      .willReturn(TransactionDto.builder()
        .accountNumber("1234567890")
        .transactionType(USE)
        .transactionResultType(S)
        .amount(3000L)
        .transactionId("aaa")
        .build());
    // when
    // then
    mockMvc.perform(get("/transaction/12345"))
      .andExpect(jsonPath("$.accountNumber").value("1234567890"))
      .andExpect(jsonPath("$.transactionType").value("USE"))
      .andExpect(jsonPath("$.transactionResult").value("S"))
      .andExpect(jsonPath("$.transactionId").value("aaa"))
      .andExpect(jsonPath("$.amount").value(3000))
      .andDo(print());
  }
}