package com.example.rectification.services;

import com.example.rectification.model.InData;
import com.example.rectification.model.OutData;
import org.springframework.stereotype.Component;

@Component
public class RectificationCalculator {

    public OutData calc(InData inData) {
        OutData outData = new OutData();
        double absoluteAlcohol = Math.round((inData.getAlcoholStrength() / 100.0) * inData.getAmountOfRawAlcohol() * 1000.0);

        outData.setAbsoluteAlcohol(absoluteAlcohol);
        outData.setHeadFractions((int) (absoluteAlcohol * RectificationConstants.HEAD_FRACTION));
        outData.setHeads((int) (absoluteAlcohol * RectificationConstants.HEADS_FRACTION));
        outData.setHeadsAndCommercialAlcohol((int) (absoluteAlcohol * RectificationConstants.HEADS_AND_COMMERCIAL_FRACTION));
        outData.setCommercialAlcohol((int) (absoluteAlcohol * RectificationConstants.COMMERCIAL_ALCOHOL_FRACTION));
        outData.setTails((int) (absoluteAlcohol * RectificationConstants.TAILS_FRACTION));

        return outData;
    }
}
