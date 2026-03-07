package com.example.rectificat.model;

import javax.persistence.*;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount_of_raw_alcohol")
    private int amountOfRawAlcohol;

    @Column(name = "alcohol_strength")
    private double alcoholStrength;

    @Column(name = "power")
    private double power;

    @Column(name = "water")
    private int water;

    // Фактические показатели
    @Column(name = "actual_commercial_alcohol")
    private Double actualCommercialAlcohol;

    @Column(name = "actual_heads")
    private Double actualHeads;

    @Column(name = "actual_tails")
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
        this.calculationDate = LocalDateTime.now();
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
