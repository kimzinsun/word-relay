package com.wordrelay.server.dto;

import lombok.Data;

@Data
public class WordQueryDto {
  private String tableName;
  private String word;

}
