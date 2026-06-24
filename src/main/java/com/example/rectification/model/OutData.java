package com.example.rectification.model;

import lombok.Data;

@Data
public class OutData {
    private double absoluteAlcohol;
    private int headFractions;
    private double heads;
    private double tails;
    private double headsAndCommercialAlcohol;
    private int commercialAlcohol;
}
