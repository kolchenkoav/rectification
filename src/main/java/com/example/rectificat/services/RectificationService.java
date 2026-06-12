package com.example.rectificat.services;

import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;

import java.util.List;
import java.util.Optional;

public interface RectificationService {
    OutData calc(InData inData);
    String resultToString(InData inData, OutData outData);
    List<String> resultToStringForHtml(InData inData, OutData outData);

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
