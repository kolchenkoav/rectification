package com.example.rectification.services;

import com.example.rectification.model.Detail;
import com.example.rectification.model.InData;
import com.example.rectification.model.OutData;
import com.example.rectification.model.RectificationHistory;
import com.example.rectification.repository.DetailRepository;
import com.example.rectification.repository.RectificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RectificationServiceImpl implements RectificationService {

    private final RectificationHistoryRepository historyRepository;
    private final DetailRepository detailRepository;
    private final RectificationCalculator calculator;

    @Override
    public OutData calc(InData inData) {
        return calculator.calc(inData);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RectificationHistory> getAllHistory() {
        return historyRepository.findAllByOrderByCalculationDateDesc();
    }

    @Override
    @Transactional
    public void deleteHistory(Long id) {
        historyRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void clearAllHistory() {
        historyRepository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RectificationHistory> getHistoryWithDetails(Long id) {
        return historyRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean historyExists(Long id) {
        return historyRepository.existsById(id);
    }

    @Override
    @Transactional
    public RectificationHistory saveCalculation(InData inData) {
        OutData outData = calc(inData);
        RectificationHistory history = new RectificationHistory(
                inData.getAmountOfRawAlcohol(),
                inData.getAlcoholStrength(),
                inData.getPower(),
                inData.getWater()
        );
        history.setResultSnapshot(outData);
        return historyRepository.save(history);
    }


    @Override
    @Transactional
    public boolean addDetail(Long historyId, Double temperatureCube, Double temperatureTsar, Double temperatureAtmosphere, Double temperatureWater) {
        Optional<RectificationHistory> historyOptional = historyRepository.findById(historyId);
        if (historyOptional.isEmpty()) {
            return false;
        }

        RectificationHistory history = historyOptional.get();
        Detail detail = new Detail(temperatureCube, temperatureTsar, temperatureAtmosphere, temperatureWater);
        history.addDetail(detail);
        historyRepository.save(history);
        return true;
    }


    @Override
    @Transactional
    public boolean deleteDetail(Long historyId, Long detailId) {
        return detailRepository.deleteByIdAndHistoryId(detailId, historyId) > 0;
    }


    @Override
    @Transactional
    public boolean saveActualData(Long historyId, Double actualCommercialAlcohol, Double actualHeads, Double actualTails) {
        Optional<RectificationHistory> historyOptional = historyRepository.findById(historyId);
        if (historyOptional.isEmpty()) {
            return false;
        }

        RectificationHistory history = historyOptional.get();
        history.setActualData(actualCommercialAlcohol, actualHeads, actualTails);
        historyRepository.save(history);
        return true;
    }

}
