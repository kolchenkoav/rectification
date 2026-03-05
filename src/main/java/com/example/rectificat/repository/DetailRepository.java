package com.example.rectificat.repository;

import com.example.rectificat.model.Detail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailRepository extends JpaRepository<Detail, Long> {
    List<Detail> findByHistoryIdOrderByRecordTimeDesc(Long historyId);
}
