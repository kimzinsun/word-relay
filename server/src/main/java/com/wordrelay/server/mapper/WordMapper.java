package com.wordrelay.server.mapper;

import com.wordrelay.server.model.Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WordMapper {
  Boolean selectWord(@Param("tableName") String tableName, @Param("word") String word);

  Word getWord(@Param("tableName") String tableName, @Param("word") String word);

}
