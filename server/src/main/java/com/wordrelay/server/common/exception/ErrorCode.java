package com.wordrelay.server.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import javax.management.loading.MLetContent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  INVALID_WORD(400, "유효하지 않은 단어입니다."),
  NOT_FOLLOWING_RULES(400, "끝말잇기 규칙을 따르지 않는 단어입니다."),
  WORD_ALREADY_USED(400, "이미 사용된 단어입니다."),
  WORD_TOO_SHORT(400, "단어가 너무 짧습니다."),
  NOT_A_REAL_WORD(400, "존재하지 않는 단어입니다."),
  GAME_NOT_STARTED(400, "게임이 아직 시작되지 않았습니다."),
  GAME_ALREADY_ENDED(400, "게임이 종료되었습니다."),
  PLAYER_NOT_FOUND(404, "플레이어를 찾을 수 없습니다."),
  UNAUTHORIZED_MOVE(403, "잘못된 차례입니다."),
  SERVER_ERROR(500, "서버 오류가 발생했습니다."),
  BROWSER_ID_MISSING(400, "브라우저 ID가 누락되었습니다."),
  USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
  INVALID_TABLE_NAME(400, "테이블 이름이 유효하지 않습니다.");

  private final int code;
  private final String message;

}
