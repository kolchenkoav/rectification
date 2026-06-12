package com.example.rectificat.services;

import com.example.rectificat.model.Detail;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.repository.DetailRepository;
import com.example.rectificat.repository.RectificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RectificationServiceImpl implements RectificationService {

    private final RectificationHistoryRepository historyRepository;
    private final DetailRepository detailRepository;

    private static final double HEAD_FRACTION = 0.08;
    private static final double HEADS_FRACTION = 0.03;
    private static final double HEADS_AND_COMMERCIAL_FRACTION = 0.05;
    private static final double COMMERCIAL_ALCOHOL_FRACTION = 0.65;
    private static final double TAILS_FRACTION = 0.035;
    private static final double VOLUME_CORRECTION_DIVISOR = 96.0;

    @Override
    public OutData calc(InData inData) {
        OutData outData = new OutData();
        double absoluteAlcohol = Math.round((inData.getAlcoholStrength() / 100.0) * inData.getAmountOfRawAlcohol() * 1000.0);

        outData.setAbsoluteAlcohol(absoluteAlcohol);
        outData.setHeadFactions((int) (absoluteAlcohol * HEAD_FRACTION));
        outData.setHeads((int) (absoluteAlcohol * HEADS_FRACTION));
        outData.setHeadsAndCommercialAlcohol((int) (absoluteAlcohol * HEADS_AND_COMMERCIAL_FRACTION));
        outData.setCommercialAlcohol((int) (absoluteAlcohol * COMMERCIAL_ALCOHOL_FRACTION));
        outData.setTails((int) (absoluteAlcohol * TAILS_FRACTION));

        return outData;
    }

    @Override
    public String resultToString(InData inData, OutData outData) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nисточник: https://www.youtube.com/watch?v=OyaRYPjnJ1U\n");
        sb.append("======================================\n");
        sb.append("ИСХОДНЫЕ ДАННЫЕ\n");
        sb.append(String.format("%s л. спирта сырца при крепости %s \n", inData.getAmountOfRawAlcohol(), inData.getAlcoholStrength()));
        sb.append(String.format("Мощность (при отборе ТС):  %5.2f кВт\n", inData.getPower()));
        sb.append(String.format("Вода в узле отбора:  %s мл.\n", inData.getWater()));
        sb.append("======================================\n");
        sb.append(String.format("Абсолютный спирт: %5.0f мл.\n", outData.getAbsoluteAlcohol()));
        sb.append("\n");
        sb.append(String.format(" --> Головы     : %5.0f мл. + %s мл. вода = %5.0f мл. -НА РОЗЖИГ\n", (outData.getHeads() * 100.0) / VOLUME_CORRECTION_DIVISOR, inData.getWater(), (outData.getHeads() * 100.0) / VOLUME_CORRECTION_DIVISOR + inData.getWater()));
        sb.append(String.format("     --> (1/3) %5.0f мл. (1 к/с - 2 к/с - 3 к/с)\n", (((outData.getHeads() * 100.0) / VOLUME_CORRECTION_DIVISOR) + inData.getWater()) / 3.0));
        sb.append(String.format(" --> Головы + ТС: %5.0f мл. (4 к/с) ОБОРОТ\n", (outData.getHeadsAndCommercialAlcohol() * 100.0) / VOLUME_CORRECTION_DIVISOR));
        sb.append("\n");
        sb.append(String.format(" --> Товарный спирт: %s мл.\n", (outData.getCommercialAlcohol() * 100.0) / VOLUME_CORRECTION_DIVISOR));
        sb.append(String.format(" --> Отбор: %s л./час длительность: %5.2f часов \n", inData.getPower() * 1.0, outData.getCommercialAlcohol() / (inData.getPower() * 1000)));
        sb.append("\n");
        sb.append(String.format(" --> Спиртовой остаток: %5.0f мл. АС\n", outData.getAbsoluteAlcohol() - outData.getCommercialAlcohol() - outData.getHeadFactions()));
        sb.append(String.format("     --> АС: %5.0f мл. ОБОРОТ\n", outData.getAbsoluteAlcohol() - outData.getCommercialAlcohol() - outData.getHeadFactions() - outData.getTails()));
        sb.append("         --> отбор до 85 градусов\n");
        sb.append(String.format("     --> Хвосты: %5.0f мл. -ВЫЛИТЬ\n", outData.getTails()));
        sb.append("======================================\n");
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public List<String> resultToStringForHtml(InData inData, OutData outData) {
        return new ArrayList<>();
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
