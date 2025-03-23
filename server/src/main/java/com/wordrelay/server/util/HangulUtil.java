package com.wordrelay.server.util;

import com.wordrelay.server.mapper.WordMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HangulUtil {
  private final WordMapper wordMapper;

  @Autowired
  public HangulUtil(WordMapper wordMapper) {
    this.wordMapper = wordMapper;
  }

  public boolean extractChoseong(String word) {
    char firstChar = word.charAt(0);

      int base = firstChar - 0xAC00;
      int choseongIndex = base / (21 * 28);
      char[] choseongList = {
          'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
          'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
      };
      char choseong = choseongList[choseongIndex];
      CHOSEONG_TO_TABLE.get(choseong);

      return wordMapper.selectWord(CHOSEONG_TO_TABLE.get(choseong), word);

  }

  private static final Map<Character, String> CHOSEONG_TO_TABLE = new HashMap<>();
  static {
    CHOSEONG_TO_TABLE.put('ㄱ', "dict_g");
    CHOSEONG_TO_TABLE.put('ㄲ', "dict_gg");
    CHOSEONG_TO_TABLE.put('ㄴ', "dict_n");
    CHOSEONG_TO_TABLE.put('ㄷ', "dict_d");
    CHOSEONG_TO_TABLE.put('ㄸ', "dict_dd");
    CHOSEONG_TO_TABLE.put('ㄹ', "dict_r");
    CHOSEONG_TO_TABLE.put('ㅁ', "dict_m");
    CHOSEONG_TO_TABLE.put('ㅂ', "dict_b");
    CHOSEONG_TO_TABLE.put('ㅃ', "dict_bb");
    CHOSEONG_TO_TABLE.put('ㅅ', "dict_s");
    CHOSEONG_TO_TABLE.put('ㅆ', "dict_ss");
    CHOSEONG_TO_TABLE.put('ㅇ', "dict_ng");
    CHOSEONG_TO_TABLE.put('ㅈ', "dict_j");
    CHOSEONG_TO_TABLE.put('ㅉ', "dict_jj");
    CHOSEONG_TO_TABLE.put('ㅊ', "dict_ch");
    CHOSEONG_TO_TABLE.put('ㅋ', "dict_k");
    CHOSEONG_TO_TABLE.put('ㅌ', "dict_t");
    CHOSEONG_TO_TABLE.put('ㅍ', "dict_p");
    CHOSEONG_TO_TABLE.put('ㅎ', "dict_h");
  }


}
