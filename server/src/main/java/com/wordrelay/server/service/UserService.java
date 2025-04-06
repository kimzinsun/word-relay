package com.wordrelay.server.service;

import com.wordrelay.server.repository.RankingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    private final RankingRepository rankingRepository;

    public UserService(RankingRepository rankingRepository) {
        this.rankingRepository = rankingRepository;
    }

    public void addScore(String browserId, int score) {
        rankingRepository.addScoreByBrowserId(browserId, score);
    }

}
