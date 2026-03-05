package com.example.rectificat.services;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RectificationServiceImpl implements RectificationService {
    private static InData inData;
    private static OutData outData = new OutData();

    @Override
    public OutData calc(InData inData) {
        double absoluteAlcohol = Math.round((inData.getAlcoholStrength()/100) * inData.getAmountOfRawAlcohol() * 1000);
        this.inData = inData;

        outData.setAbsoluteAlcohol(absoluteAlcohol);
        outData.setHeadFactions((int) (outData.getAbsoluteAlcohol() * 0.08));
        outData.setHeads((int) (outData.getAbsoluteAlcohol() * 0.03));
        outData.setHeadsAndCommercialAlcohol((int) (outData.getAbsoluteAlcohol() * 0.05));

        outData.setCommercialAlcohol((int) (outData.getAbsoluteAlcohol() * 0.65));
        outData.setTails((int) (outData.getAbsoluteAlcohol() * 0.035));

        return outData;
    }

    @Override
    public StringBuilder resultToString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        stringBuilder.append("источник: https://www.youtube.com/watch?v=OyaRYPjnJ1U\n");
        stringBuilder.append("======================================\n");
        stringBuilder.append("ИСХОДНЫЕ ДАННЫЕ\n");
        stringBuilder.append(String.format("%s л. спирта сырца при крепости %s \n", inData.getAmountOfRawAlcohol(), inData.getAlcoholStrength()));
        stringBuilder.append(String.format("Мощность (при отборе ТС):  %5.2f кВт\n", inData.getPower()));
        stringBuilder.append(String.format("Вода в узле отбора:  %s мл.\n", inData.getWater()));
        stringBuilder.append("======================================\n");
        stringBuilder.append(String.format("Абсолютный спирт: %5.0f мл.\n", outData.getAbsoluteAlcohol()));
        stringBuilder.append("\n");
        stringBuilder.append(String.format(" --> Головы     : %5.0f мл. + %s мл. вода = %5.0f мл. -НА РОЗЖИГ\n", (outData.getHeads() * 100)/96, inData.getWater(), (outData.getHeads() * 100)/96 + inData.getWater()));
        stringBuilder.append(String.format("     --> (1/3) %5.0f мл. (1 к/с - 2 к/с - 3 к/с)\n", ((outData.getHeads() * 100)/96 + inData.getWater())/3));
        stringBuilder.append(String.format(" --> Головы + ТС: %5.0f мл. (4 к/с) ОБОРОТ\n", (outData.getHeadsAndCommercialAlcohol() * 100)/96));
        stringBuilder.append("\n");
        stringBuilder.append(String.format(" --> Товарный спирт: %s мл.\n", (outData.getCommercialAlcohol() * 100)/96));
        stringBuilder.append(String.format(" --> Отбор: %s л./час длительность: %5.2f часов \n", inData.getPower() * 1.0, outData.getCommercialAlcohol()/(inData.getPower()*1000)));
        stringBuilder.append("\n");
        //System.out.printf(" --> Спиртовой остаток: %s \n", (absoluteAlcohol * 100)/96 - commercialAlcohol - (headsAndCommercialAlcohol * 100)/96);
        stringBuilder.append(String.format(" --> Спиртовой остаток: %5.0f мл. АС\n", outData.getAbsoluteAlcohol() - outData.getCommercialAlcohol() - outData.getHeadFactions()));
        stringBuilder.append(String.format("     --> АС: %5.0f мл. ОБОРОТ\n", outData.getAbsoluteAlcohol() - outData.getCommercialAlcohol() - outData.getHeadFactions() - outData.getTails()));
        stringBuilder.append("         --> отбор до 85 градусов\n");
        stringBuilder.append(String.format("     --> Хвосты: %5.0f мл. -ВЫЛИТЬ\n", outData.getTails()));
        stringBuilder.append("======================================\n");
        stringBuilder.append("\n");
        return stringBuilder;
    }

    @Override
    public List<String> resultToStringForHtml() {
        return new ArrayList<>();
    }
}
