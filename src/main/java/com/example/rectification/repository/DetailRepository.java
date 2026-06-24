package com.example.rectification.repository;

import com.example.rectification.model.Detail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailRepository extends JpaRepository<Detail, Long> {
    List<Detail> findByHistoryIdOrderByRecordTimeDesc(Long historyId);

    long deleteByIdAndHistoryId(Long id, Long historyId);
}
