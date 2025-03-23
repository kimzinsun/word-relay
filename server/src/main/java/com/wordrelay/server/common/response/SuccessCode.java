package com.wordrelay.server.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {
  WORD_VALID(200, "정상 입력되었습니다."),
  WORD_INVALID(200, "유효하지 않은 단어입니다."),
  WORD_NOT_FOLLOWING_RULES(200, "끝말잇기 규칙을 따르지 않는 단어입니다."),
  WORD_ALREADY_USED(200, "이미 사용된 단어입니다."),
  WORD_TOO_SHORT(200, "단어가 너무 짧습니다."),
  WORD_NOT_A_REAL_WORD(200, "존재하지 않는 단어입니다.");

  private final int code;
  private final String message;
}
