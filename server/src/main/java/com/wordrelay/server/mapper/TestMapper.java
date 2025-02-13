package com.wordrelay.server.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper {
  int testQuery();
}
