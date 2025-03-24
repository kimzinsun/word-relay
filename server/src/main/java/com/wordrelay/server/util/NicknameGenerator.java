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

        try {
            int adjectiveIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 145);
            int animalIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 50);

            String adjective = adjectivesMapper.selectAdjective(adjectiveIndex);
            String animal = animalsMapper.selectAnimal(animalIndex);

            return adjective + " " + animal;
        } catch (Exception e) {
            return "행운의 동물🦄";
        }

    }
}
