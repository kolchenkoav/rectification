package com.example.rectification.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detail")
public class Detail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "temperature_cube")
    @DecimalMin(value = "-50.0", message = "Температура должна быть не ниже -50 °C")
    @DecimalMax(value = "150.0", message = "Температура должна быть не выше 150 °C")
    private Double temperatureCube;

    @Column(name = "temperature_tsar")
    @DecimalMin(value = "-50.0", message = "Температура должна быть не ниже -50 °C")
    @DecimalMax(value = "150.0", message = "Температура должна быть не выше 150 °C")
    private Double temperatureTsar;

    @Column(name = "temperature_atmosphere")
    @DecimalMin(value = "-50.0", message = "Температура должна быть не ниже -50 °C")
    @DecimalMax(value = "150.0", message = "Температура должна быть не выше 150 °C")
    private Double temperatureAtmosphere;

    @Column(name = "temperature_water")
    @DecimalMin(value = "-50.0", message = "Температура должна быть не ниже -50 °C")
    @DecimalMax(value = "150.0", message = "Температура должна быть не выше 150 °C")
    private Double temperatureWater;

    @Column(name = "record_time")
    private LocalDateTime recordTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id")
    private RectificationHistory history;

    public Detail(Double temperatureCube, Double temperatureTsar,
                  Double temperatureAtmosphere, Double temperatureWater) {
        this.temperatureCube = temperatureCube;
        this.temperatureTsar = temperatureTsar;
        this.temperatureAtmosphere = temperatureAtmosphere;
        this.temperatureWater = temperatureWater;
        this.recordTime = LocalDateTime.now();
    }
}
