package com.example.rectificat.repository;

import com.example.rectificat.model.RectificationHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RectificationHistoryRepository extends JpaRepository<RectificationHistory, Long> {
    List<RectificationHistory> findAllByOrderByCalculationDateDesc();
    
    @EntityGraph(attributePaths = {"details"})
    Optional<RectificationHistory> findById(Long id);
}
