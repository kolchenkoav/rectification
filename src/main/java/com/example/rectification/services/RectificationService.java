package com.example.rectification.services;

import com.example.rectification.model.InData;
import com.example.rectification.model.OutData;
import com.example.rectification.model.RectificationHistory;

import java.util.List;
import java.util.Optional;

public interface RectificationService {
    OutData calc(InData inData);

    List<RectificationHistory> getAllHistory();
    void deleteHistory(Long id);
    void clearAllHistory();
    Optional<RectificationHistory> getHistoryWithDetails(Long id);
    boolean historyExists(Long id);
    RectificationHistory saveCalculation(InData inData);

    boolean addDetail(Long historyId, Double temperatureCube, Double temperatureTsar, Double temperatureAtmosphere, Double temperatureWater);
    boolean deleteDetail(Long historyId, Long detailId);
    boolean saveActualData(Long historyId, Double actualCommercialAlcohol, Double actualHeads, Double actualTails);
}
