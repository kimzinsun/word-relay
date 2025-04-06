package com.wordrelay.server.dto;

import lombok.Data;

@Data
public class ConnectResponse {
  private String nickname;
  private String browserId;

  public ConnectResponse(String nickname, String browserId) {
    this.nickname = nickname;
    this.browserId = browserId;
  }

}
