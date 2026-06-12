package com.example.rectificat.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InData {
    @Min(value = 1, message = "Спирт-сырец должен быть не меньше 1 л")
    @Max(value = 1000, message = "Спирт-сырец должен быть не больше 1000 л")
    private int amountOfRawAlcohol;

    @DecimalMin(value = "0.1", message = "Крепость должна быть не меньше 0.1%")
    @DecimalMax(value = "100.0", message = "Крепость не может быть больше 100%")
    private double alcoholStrength;

    @DecimalMin(value = "0.1", message = "Мощность должна быть не меньше 0.1 кВт")
    @DecimalMax(value = "100.0", message = "Мощность должна быть не больше 100 кВт")
    private double power;

    @NotNull(message = "Вода обязательна: укажите 0 мл, если воды нет")
    @Min(value = 0, message = "Вода может быть 0 мл или больше")
    @Max(value = 10000, message = "Вода должна быть не больше 10000 мл")
    private Integer water;
}
