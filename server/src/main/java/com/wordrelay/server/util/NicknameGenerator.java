package com.wordrelay.server.util;

import java.util.UUID;

public class NicknameGenerator {
  public static String generateRandomNickname() {
    return "Player_" + UUID.randomUUID().toString().substring(0, 5);
  }

}
