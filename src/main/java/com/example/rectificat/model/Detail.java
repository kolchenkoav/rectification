package com.example.rectificat.model;

import javax.persistence.*;
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
    private Double temperatureCube;

    @Column(name = "temperature_tsar")
    private Double temperatureTsar;

    @Column(name = "temperature_atmosphere")
    private Double temperatureAtmosphere;

    @Column(name = "temperature_water")
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
