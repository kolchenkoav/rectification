package com.example.rectificat.controller;

import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.repository.RectificationHistoryRepository;
import com.example.rectificat.services.RectificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@Slf4j
public class RectificationController {
    private final RectificationService service;
    private final RectificationHistoryRepository historyRepository;

    InData inData;
    OutData outData;
    List<String> value;

    public RectificationController(RectificationService service, RectificationHistoryRepository historyRepository) {
        this.service = service;
        this.historyRepository = historyRepository;
        inData = new InData(19, 40, 0.6, 25);
        outData = new OutData();
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("history", historyRepository.findAllByOrderByCalculationDateDesc());
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
