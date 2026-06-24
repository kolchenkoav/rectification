package com.example.rectification.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.rectification.services.RectificationConstants;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rectification_history")
public class RectificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount_of_raw_alcohol", nullable = false)
    private int amountOfRawAlcohol;

    @Column(name = "alcohol_strength", nullable = false)
    private double alcoholStrength;

    @Column(name = "power", nullable = false)
    private double power;

    @Column(name = "water", nullable = false)
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
        outData.setHeadFractions((int) (absoluteAlcohol * RectificationConstants.HEAD_FRACTION));
        outData.setHeads(heads);
        outData.setTails(tails);
        outData.setHeadsAndCommercialAlcohol((int) (absoluteAlcohol * RectificationConstants.HEADS_AND_COMMERCIAL_FRACTION));
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
