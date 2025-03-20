package com.wordrelay.server.util;

import com.wordrelay.server.mapper.AdjectivesMapper;
import com.wordrelay.server.mapper.AnimalsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NicknameGenerator {

    private final AdjectivesMapper adjectivesMapper;
    private final AnimalsMapper animalsMapper;

    @Autowired
    public NicknameGenerator(AdjectivesMapper adjectivesMapper, AnimalsMapper animalsMapper) {
        this.adjectivesMapper = adjectivesMapper;
        this.animalsMapper = animalsMapper;
    }

    public String generateRandomNickname() {
        String adjective = adjectivesMapper.selectAdjective((int) (Math.random() * 10));
        String animal = animalsMapper.selectAnimal((int) (Math.random() * 10));
        log.info(adjective + " " + animal);
        return adjective + " " + animal;
    }
}
