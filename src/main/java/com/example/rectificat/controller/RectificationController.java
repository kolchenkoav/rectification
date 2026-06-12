package com.example.rectificat.controller;

import com.example.rectificat.model.Detail;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.services.RectificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Slf4j
public class RectificationController {
    private static final double MIN_TEMPERATURE = -50.0;
    private static final double MAX_TEMPERATURE = 150.0;
    private static final double MAX_ACTUAL_VOLUME_ML = 100_000.0;

    private final RectificationService service;
    private final Environment environment;

    public RectificationController(RectificationService service, Environment environment) {
        this.service = service;
        this.environment = environment;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("history", service.getAllHistory());
        model.addAttribute("appVersion", environment.getProperty("app.version", "0.0.1"));
        model.addAttribute("appTag", environment.getProperty("app.tag", "SNAPSHOT"));
        return "History";
    }

    @GetMapping("/new")
    public String newData(Model model) {
        model.addAttribute("inData", new InData());
        return "InData";
    }

    @GetMapping("/info")
    public String infoRedirect() {
        return "redirect:/new";
    }

    @PostMapping("/delete/{id}")
    public String deleteHistory(@PathVariable Long id) {
        service.deleteHistory(id);
        return "redirect:/";
    }

    @PostMapping("/clear")
    public String clearHistory() {
        service.clearAllHistory();
        return "redirect:/";
    }

    @GetMapping("/view/{id}")
    public String viewHistory(@PathVariable Long id, Model model) {
        RectificationHistory history = service.getHistoryWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "История расчета не найдена: " + id));

        InData data = new InData();
        data.setAmountOfRawAlcohol(history.getAmountOfRawAlcohol());
        data.setAlcoholStrength(history.getAlcoholStrength());
        data.setPower(history.getPower());
        data.setWater(history.getWater());

        OutData out = history.toOutData();
        List<Detail> details = history.getDetails();

        model.addAttribute("inData", data);

        model.addAttribute("outData", out);
        model.addAttribute("details", details);
        model.addAttribute("historyId", id);

        model.addAttribute("actualCommercialAlcohol", history.getActualCommercialAlcohol());
        model.addAttribute("actualHeads", history.getActualHeads());
        model.addAttribute("actualTails", history.getActualTails());
        model.addAttribute("hasActualData", history.hasActualData());
        return "OutData";
    }

    @GetMapping("/print/{id}")
    public String printHistory(@PathVariable Long id, Model model) {
        RectificationHistory history = service.getHistoryWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "История расчета не найдена: " + id));

        InData data = new InData();
        data.setAmountOfRawAlcohol(history.getAmountOfRawAlcohol());
        data.setAlcoholStrength(history.getAlcoholStrength());
        data.setPower(history.getPower());
        data.setWater(history.getWater());

        OutData out = history.toOutData();
        List<Detail> details = history.getDetails();

        model.addAttribute("inData", data);

        model.addAttribute("outData", out);
        model.addAttribute("details", details);

        model.addAttribute("actualCommercialAlcohol", history.getActualCommercialAlcohol());
        model.addAttribute("actualHeads", history.getActualHeads());
        model.addAttribute("actualTails", history.getActualTails());
        model.addAttribute("hasActualData", history.hasActualData());

        model.addAttribute("calcCommercialAlcohol", out.getCommercialAlcohol() * 100.0 / 96.0);
        model.addAttribute("calcHeads", out.getHeads() * 100.0 / 96.0 + data.getWater());

        model.addAttribute("calcTails", out.getTails());

        model.addAttribute("calculationDate", history.getCalculationDate());
        return "Print";
    }

    @PostMapping("/view/{id}/detail")
    public String addDetail(@PathVariable Long id,
                            @RequestParam(required = false) String temperatureCube,
                            @RequestParam(required = false) String temperatureTsar,
                            @RequestParam(required = false) String temperatureAtmosphere,
                            @RequestParam(required = false) String temperatureWater,
                            RedirectAttributes redirectAttributes) {
        assertHistoryExists(id);

        Double cube = parseFiniteDouble(temperatureCube);

        Double tsar = parseFiniteDouble(temperatureTsar);
        Double atmosphere = parseFiniteDouble(temperatureAtmosphere);
        Double water = parseFiniteDouble(temperatureWater);

        if (!isInRange(cube, MIN_TEMPERATURE, MAX_TEMPERATURE)
                || !isInRange(tsar, MIN_TEMPERATURE, MAX_TEMPERATURE)
                || !isInRange(atmosphere, MIN_TEMPERATURE, MAX_TEMPERATURE)
                || !isInRange(water, MIN_TEMPERATURE, MAX_TEMPERATURE)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Температуры должны быть конечными числами от -50 до 150 °C.");
            return "redirect:/view/" + id;
        }

        if (!service.addDetail(id, cube, tsar, atmosphere, water)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "История расчета не найдена: " + id);
        }
        log.info("Добавлена запись деталей для расчета {}", id);
        return "redirect:/view/" + id;
    }

    @PostMapping("/view/{historyId}/detail/{detailId}/delete")
    public String deleteDetail(@PathVariable Long historyId, @PathVariable Long detailId) {
        if (!service.deleteDetail(historyId, detailId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Замер не найден для расчета: " + historyId);
        }
        return "redirect:/view/" + historyId;
    }

    @PostMapping("/view/{id}/actual")
    public String saveActualData(@PathVariable Long id,
                                 @RequestParam(required = false) String actualCommercialAlcohol,
                                 @RequestParam(required = false) String actualHeads,
                                 @RequestParam(required = false) String actualTails,
                                 RedirectAttributes redirectAttributes) {
        assertHistoryExists(id);

        Double commercialAlcohol = parseFiniteDouble(actualCommercialAlcohol);

        Double heads = parseFiniteDouble(actualHeads);
        Double tails = parseFiniteDouble(actualTails);

        boolean commercialAlcoholProvided = hasText(actualCommercialAlcohol);
        boolean headsProvided = hasText(actualHeads);
        boolean tailsProvided = hasText(actualTails);

        if ((commercialAlcoholProvided && !isPositive(commercialAlcohol, MAX_ACTUAL_VOLUME_ML))
                || (headsProvided && !isNonNegative(heads, MAX_ACTUAL_VOLUME_ML))
                || (tailsProvided && !isNonNegative(tails, MAX_ACTUAL_VOLUME_ML))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Фактический товарный спирт должен быть больше 0 мл, головы + вода и хвосты — 0 мл или больше.");
            return "redirect:/view/" + id;
        }

        if (!service.saveActualData(id, commercialAlcohol, heads, tails)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "История расчета не найдена: " + id);
        }
        log.info("Сохранены фактические показатели для расчета {}", id);
        return "redirect:/view/" + id;
    }

    @PostMapping("/info")
    public String info(@Valid @ModelAttribute("inData") InData inData, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "InData";
        }

        OutData outData;
        try {
            RectificationHistory savedHistory = service.saveCalculation(inData);
            outData = savedHistory.toOutData();
            log.info("Расчет сохранен в историю: {} л., крепость {}%, мощность {} кВт",
                    inData.getAmountOfRawAlcohol(), inData.getAlcoholStrength(), inData.getPower());
        } catch (Exception e) {
            log.error("Не удалось сохранить в БД: {}", e.getMessage(), e);
            outData = service.calc(inData);
            model.addAttribute("errorMessage", "Не удалось сохранить расчет в историю. Пожалуйста, попробуйте позже.");
        }

        List<String> value = service.resultToStringForHtml(inData, outData);
        model.addAttribute("outData", outData);
        model.addAttribute("result", value);

        return "OutData";

    }

    private void assertHistoryExists(Long id) {
        if (!service.historyExists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "История расчета не найдена: " + id);
        }
    }

    private Double parseFiniteDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isInRange(Double value, double min, double max) {
        return value != null && value >= min && value <= max;
    }

    private boolean isPositive(Double value, double max) {
        return value != null && value > 0 && value <= max;
    }

    private boolean isNonNegative(Double value, double max) {
        return value != null && value >= 0 && value <= max;
    }
}
