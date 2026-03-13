package com.release.rr.domain.rank.repository;

import com.release.rr.domain.rank.entity.BossClearRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BossClearRepository extends JpaRepository<BossClearRecord, Long> {
    List<BossClearRecord> findTop10ByOrderByTimeTakenSecondsAsc();
}
