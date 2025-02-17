package com.wordrelay.server.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
  private final int code;   // 응답 코드 (예: 200, 400, 500)
  private final String message;  // 응답 메시지
  private final T data;    // 실제 데이터

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(200, "Success", data);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
