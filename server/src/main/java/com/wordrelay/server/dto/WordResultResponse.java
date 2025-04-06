package com.wordrelay.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WordResultResponse {
  private boolean success;
  private String currentWord;
  private String message;

}
