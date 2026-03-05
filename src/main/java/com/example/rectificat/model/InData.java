package com.example.rectificat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InData {
    private int amountOfRawAlcohol;
    private double alcoholStrength;
    private double power;
    private int water;
}
