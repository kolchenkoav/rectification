package com.example.rectificat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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

    @Column(name = "calculation_date")
    private LocalDateTime calculationDate = LocalDateTime.now();

    public RectificationHistory(int amountOfRawAlcohol, double alcoholStrength, double power, int water) {
        this.amountOfRawAlcohol = amountOfRawAlcohol;
        this.alcoholStrength = alcoholStrength;
        this.power = power;
        this.water = water;
        this.calculationDate = LocalDateTime.now();
    }
}
