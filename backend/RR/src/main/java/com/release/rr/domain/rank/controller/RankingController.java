package com.release.rr.domain.rank.controller;

import com.release.rr.domain.rank.entity.BossClearRecord;
import com.release.rr.domain.rank.repository.BossClearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankingController {

    private final BossClearRepository bossClearRepository;

    @GetMapping("/boss")
    public ResponseEntity<List<BossClearRecord>> getBossRanking() {
        return ResponseEntity.ok(bossClearRepository.findTop10ByOrderByTimeTakenSecondsAsc());
    }
}
