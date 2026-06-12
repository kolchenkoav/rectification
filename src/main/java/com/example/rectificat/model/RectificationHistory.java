package com.example.rectificat.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rectification_history")
public class RectificationHistory {
    private static final double HEAD_FRACTION = 0.08;
    private static final double HEADS_AND_COMMERCIAL_FRACTION = 0.05;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "amount_of_raw_alcohol", nullable = false)
    @Min(value = 1, message = "Спирт-сырец должен быть не меньше 1 л")
    @Max(value = 1000, message = "Спирт-сырец должен быть не больше 1000 л")
    private int amountOfRawAlcohol;

    @Column(name = "alcohol_strength", nullable = false)
    @DecimalMin(value = "0.1", message = "Крепость должна быть не меньше 0.1%")
    @DecimalMax(value = "100.0", message = "Крепость не может быть больше 100%")
    private double alcoholStrength;

    @Column(name = "power", nullable = false)
    @DecimalMin(value = "0.1", message = "Мощность должна быть не меньше 0.1 кВт")
    @DecimalMax(value = "100.0", message = "Мощность должна быть не больше 100 кВт")
    private double power;

    @Column(name = "water", nullable = false)
    @Min(value = 0, message = "Вода может быть 0 мл или больше")
    @Max(value = 10000, message = "Вода должна быть не больше 10000 мл")
    private int water;

    // Immutable result snapshot persisted in V2 columns.
    @Column(name = "absolute_alcohol", nullable = false, updatable = false)
    private double absoluteAlcohol;

    @Column(name = "heads", nullable = false, updatable = false)
    private double heads;

    @Column(name = "commercial_alcohol", nullable = false, updatable = false)
    private int commercialAlcohol;

    @Column(name = "tails", nullable = false, updatable = false)
    private double tails;

    // Фактические показатели

    @Column(name = "actual_commercial_alcohol")
    @Positive(message = "Фактический товарный спирт должен быть больше 0 мл")
    private Double actualCommercialAlcohol;

    @Column(name = "actual_heads")
    @PositiveOrZero(message = "Фактические головы должны быть 0 мл или больше")
    private Double actualHeads;

    @Column(name = "actual_tails")
    @PositiveOrZero(message = "Фактические хвосты должны быть 0 мл или больше")
    private Double actualTails;

    @Column(name = "calculation_date")
    private LocalDateTime calculationDate = LocalDateTime.now();

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Detail> details = new ArrayList<>();

    public RectificationHistory(int amountOfRawAlcohol, double alcoholStrength, double power, int water) {
        this.amountOfRawAlcohol = amountOfRawAlcohol;
        this.alcoholStrength = alcoholStrength;
        this.power = power;
        this.water = water;
    }

    public void setResultSnapshot(OutData outData) {
        this.absoluteAlcohol = outData.getAbsoluteAlcohol();
        this.heads = outData.getHeads();
        this.commercialAlcohol = outData.getCommercialAlcohol();
        this.tails = outData.getTails();
    }

    public OutData toOutData() {
        OutData outData = new OutData();
        outData.setAbsoluteAlcohol(absoluteAlcohol);
        outData.setHeadFactions((int) (absoluteAlcohol * HEAD_FRACTION));
        outData.setHeads(heads);
        outData.setTails(tails);
        outData.setHeadsAndCommercialAlcohol((int) (absoluteAlcohol * HEADS_AND_COMMERCIAL_FRACTION));
        outData.setCommercialAlcohol(commercialAlcohol);
        return outData;
    }

    // Методы для установки фактических показателей

    public void setActualData(Double actualCommercialAlcohol, Double actualHeads, Double actualTails) {
        this.actualCommercialAlcohol = actualCommercialAlcohol;
        this.actualHeads = actualHeads;
        this.actualTails = actualTails;
    }

    public boolean hasActualData() {
        return actualCommercialAlcohol != null || actualHeads != null || actualTails != null;
    }

    public void addDetail(Detail detail) {
        details.add(detail);
        detail.setHistory(this);
    }

    public void removeDetail(Detail detail) {
        details.remove(detail);
        detail.setHistory(null);
    }
}
