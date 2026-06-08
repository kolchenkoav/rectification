package com.example.rectificat.controller;

import com.example.rectificat.model.Detail;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.services.RectificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@Slf4j
public class RectificationController {
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
        service.getHistoryWithDetails(id).ifPresent(history -> {
            InData data = new InData();
            data.setAmountOfRawAlcohol(history.getAmountOfRawAlcohol());
            data.setAlcoholStrength(history.getAlcoholStrength());
            data.setPower(history.getPower());
            data.setWater(history.getWater());

            OutData out = service.calc(data);
            List<Detail> details = history.getDetails();

            model.addAttribute("inData", data);
            model.addAttribute("outData", out);
            model.addAttribute("details", details);
            model.addAttribute("historyId", id);

            model.addAttribute("actualCommercialAlcohol", history.getActualCommercialAlcohol());
            model.addAttribute("actualHeads", history.getActualHeads());
            model.addAttribute("actualTails", history.getActualTails());
            model.addAttribute("hasActualData", history.hasActualData());
        });
        return "OutData";
    }

    @GetMapping("/print/{id}")
    public String printHistory(@PathVariable Long id, Model model) {
        service.getHistoryWithDetails(id).ifPresent(history -> {
            InData data = new InData();
            data.setAmountOfRawAlcohol(history.getAmountOfRawAlcohol());
            data.setAlcoholStrength(history.getAlcoholStrength());
            data.setPower(history.getPower());
            data.setWater(history.getWater());

            OutData out = service.calc(data);
            List<Detail> details = history.getDetails();

            model.addAttribute("inData", data);
            model.addAttribute("outData", out);
            model.addAttribute("details", details);

            model.addAttribute("actualCommercialAlcohol", history.getActualCommercialAlcohol());
            model.addAttribute("actualHeads", history.getActualHeads());
            model.addAttribute("actualTails", history.getActualTails());
            model.addAttribute("hasActualData", history.hasActualData());

            model.addAttribute("calcCommercialAlcohol", out.getCommercialAlcohol() * 100 / 96);
            model.addAttribute("calcHeads", out.getHeads() * 100 / 96 + data.getWater());
            model.addAttribute("calcTails", out.getAbsoluteAlcohol() - out.getCommercialAlcohol() - out.getHeadFactions() - out.getTails());

            model.addAttribute("calculationDate", history.getCalculationDate());
        });
        return "Print";
    }

    @PostMapping("/view/{id}/detail")
    public String addDetail(@PathVariable Long id,
                           @RequestParam Double temperatureCube,
                           @RequestParam Double temperatureTsar,
                           @RequestParam Double temperatureAtmosphere,
                           @RequestParam Double temperatureWater) {
        service.addDetail(id, temperatureCube, temperatureTsar, temperatureAtmosphere, temperatureWater);
        log.info("Добавлена запись деталей для расчета {}", id);
        return "redirect:/view/" + id;
    }

    @PostMapping("/view/{historyId}/detail/{detailId}/delete")
    public String deleteDetail(@PathVariable Long historyId, @PathVariable Long detailId) {
        service.deleteDetail(detailId);
        return "redirect:/view/" + historyId;
    }

    @PostMapping("/view/{id}/actual")
    public String saveActualData(@PathVariable Long id,
                                   @RequestParam Double actualCommercialAlcohol,
                                   @RequestParam Double actualHeads,
                                   @RequestParam Double actualTails) {
        service.saveActualData(id, actualCommercialAlcohol, actualHeads, actualTails);
        log.info("Сохранены фактические показатели для расчета {}", id);
        return "redirect:/view/" + id;
    }

    @PostMapping("/info")
    public String info(@ModelAttribute InData inData, Model model) {
        OutData outData = service.calc(inData);
        List<String> value = service.resultToStringForHtml(inData, outData);
        model.addAttribute("outData", outData);
        model.addAttribute("result", value);

        try {
            service.saveCalculation(inData);
            log.info("Расчет сохранен в историю: {} л., крепость {}%, мощность {} кВт",
                    inData.getAmountOfRawAlcohol(), inData.getAlcoholStrength(), inData.getPower());
        } catch (Exception e) {
            log.error("Не удалось сохранить в БД: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Не удалось сохранить расчет в историю. Пожалуйста, попробуйте позже.");
        }

        return "OutData";
    }
}
