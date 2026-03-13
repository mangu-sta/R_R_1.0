package com.release.rr.domain.map.service;

import com.release.rr.domain.map.repository.MapRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapCommandService {

    private final MapRepository mapRepository;

    @Transactional
    public void increaseKillCount(String nanoId) {
        mapRepository.increaseKillCount(nanoId);
    }
}
