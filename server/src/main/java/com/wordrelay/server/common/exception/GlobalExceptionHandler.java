package com.wordrelay.server.common.exception;

import com.wordrelay.server.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ApiResponse<String> handleException(Exception e) {
    log.error("Unhandled exception: ", e);
    return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류가 발생했습니다.");
  }

  // 사용자 정의 예외 처리
  @ExceptionHandler(CustomException.class)
  public ApiResponse<String> handleCustomException(CustomException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
  }
}
