package com.wordrelay.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WordMapper {
  Boolean selectWord(@Param("tableName") String tableName, @Param("word") String word);

}
