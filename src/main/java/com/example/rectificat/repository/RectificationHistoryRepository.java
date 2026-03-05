package com.example.rectificat.repository;

import com.example.rectificat.model.RectificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RectificationHistoryRepository extends JpaRepository<RectificationHistory, Long> {
    List<RectificationHistory> findAllByOrderByCalculationDateDesc();
}
