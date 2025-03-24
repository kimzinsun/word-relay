package com.wordrelay.server.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {
  WORD_VALID(200, "정상 입력되었습니다.");

  private final int code;
  private final String message;
}
