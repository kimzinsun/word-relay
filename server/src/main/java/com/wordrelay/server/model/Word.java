package com.wordrelay.server.model;

import lombok.Data;

@Data
public class Word {

  private int id;
  private String word;
  private String definition;
  private Boolean winningWord;

}
