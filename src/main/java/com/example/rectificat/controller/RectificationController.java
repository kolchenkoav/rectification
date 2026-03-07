package com.example.rectificat.controller;

import com.example.rectificat.model.Detail;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.repository.DetailRepository;
import com.example.rectificat.repository.RectificationHistoryRepository;
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
    private final RectificationHistoryRepository historyRepository;
    private final DetailRepository detailRepository;
    private final Environment environment;

    InData inData;
    OutData outData;
    List<String> value;

    public RectificationController(RectificationService service,
                                   RectificationHistoryRepository historyRepository,
                                   DetailRepository detailRepository,
                                   Environment environment) {
        this.service = service;
        this.historyRepository = historyRepository;
        this.detailRepository = detailRepository;
        this.environment = environment;
        inData = new InData(19, 40, 0.6, 25);
        outData = new OutData();
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("history", historyRepository.findAllByOrderByCalculationDateDesc());
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
        historyRepository.deleteById(id);
        return "redirect:/";
    }

    @PostMapping("/clear")
    public String clearHistory() {
        historyRepository.deleteAll();
        return "redirect:/";
    }

    @GetMapping("/view/{id}")
    public String viewHistory(@PathVariable Long id, Model model) {
        historyRepository.findById(id).ifPresent(history -> {
            InData data = new InData();
            data.setAmountOfRawAlcohol(history.getAmountOfRawAlcohol());
            data.setAlcoholStrength(history.getAlcoholStrength());
            data.setPower(history.getPower());
            data.setWater(history.getWater());

            OutData out = service.calc(data);
            List<Detail> details = detailRepository.findByHistoryIdOrderByRecordTimeDesc(id);

            model.addAttribute("inData", data);
            model.addAttribute("outData", out);
            model.addAttribute("details", details);
            model.addAttribute("historyId", id);

            // Фактические данные
            model.addAttribute("actualCommercialAlcohol", history.getActualCommercialAlcohol());
            model.addAttribute("actualHeads", history.getActualHeads());
            model.addAttribute("actualTails", history.getActualTails());
            model.addAttribute("hasActualData", history.hasActualData());
        });
        return "OutData";
    }

    @GetMapping("/print/{id}")
    public String printHistory(@PathVariable Long id, Model model) {
        historyRepository.findById(id).ifPresent(history -> {
            InData data = new InData();
            data.setAmountOfRawAlcohol(history.getAmountOfRawAlcohol());
            data.setAlcoholStrength(history.getAlcoholStrength());
            data.setPower(history.getPower());
            data.setWater(history.getWater());

            OutData out = service.calc(data);
            List<Detail> details = detailRepository.findByHistoryIdOrderByRecordTimeDesc(id);

            model.addAttribute("inData", data);
            model.addAttribute("outData", out);
            model.addAttribute("details", details);

            // Фактические данные и рассчитанные значения для отклонений
            model.addAttribute("actualCommercialAlcohol", history.getActualCommercialAlcohol());
            model.addAttribute("actualHeads", history.getActualHeads());
            model.addAttribute("actualTails", history.getActualTails());
            model.addAttribute("hasActualData", history.hasActualData());

            // Рассчитанные значения для вычисления отклонений
            model.addAttribute("calcCommercialAlcohol", out.getCommercialAlcohol() * 100 / 96);
            model.addAttribute("calcHeads", out.getHeads() * 100 / 96 + data.getWater());  // Головы + вода (НА РОЗЖИГ)
            model.addAttribute("calcTails", out.getAbsoluteAlcohol() - out.getCommercialAlcohol() - out.getHeadFactions() - out.getTails());  // АС (ОБОРОТ)

            // Дата расчета
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
        historyRepository.findById(id).ifPresent(history -> {
            Detail detail = new Detail(temperatureCube, temperatureTsar, temperatureAtmosphere, temperatureWater);
            history.addDetail(detail);
            historyRepository.save(history);
            log.info("Добавлена запись деталей для расчета {}", id);
        });
        return "redirect:/view/" + id;
    }

    @PostMapping("/view/{historyId}/detail/{detailId}/delete")
    public String deleteDetail(@PathVariable Long historyId, @PathVariable Long detailId) {
        detailRepository.deleteById(detailId);
        return "redirect:/view/" + historyId;
    }

    @PostMapping("/view/{id}/actual")
    public String saveActualData(@PathVariable Long id,
                                  @RequestParam Double actualCommercialAlcohol,
                                  @RequestParam Double actualHeads,
                                  @RequestParam Double actualTails) {
        historyRepository.findById(id).ifPresent(history -> {
            history.setActualData(actualCommercialAlcohol, actualHeads, actualTails);
            historyRepository.save(history);
            log.info("Сохранены фактические показатели для расчета {}", id);
        });
        return "redirect:/view/" + id;
    }

    @PostMapping("/info")
    public String info(@ModelAttribute InData inData, Model model) {
        outData = service.calc(inData);
        value = service.resultToStringForHtml();
        model.addAttribute("outData", outData);
        model.addAttribute("result", value);

        try {
            RectificationHistory history = new RectificationHistory(
                    inData.getAmountOfRawAlcohol(),
                    inData.getAlcoholStrength(),
                    inData.getPower(),
                    inData.getWater()
            );
            historyRepository.save(history);
            log.info("Расчет сохранен в историю: {} л., крепость {}%, мощность {} кВт",
                    inData.getAmountOfRawAlcohol(), inData.getAlcoholStrength(), inData.getPower());
        } catch (Exception e) {
            log.warn("Не удалось сохранить в БД: {}", e.getMessage());
        }

        return "OutData";
    }
}
