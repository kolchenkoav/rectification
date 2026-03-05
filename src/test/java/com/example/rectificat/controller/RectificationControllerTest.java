package com.example.rectificat.controller;

import com.example.rectificat.model.InData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.repository.DetailRepository;
import com.example.rectificat.repository.RectificationHistoryRepository;
import com.example.rectificat.services.RectificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RectificationController.class)
class RectificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RectificationService service;

    @MockBean
    private RectificationHistoryRepository historyRepository;

    @MockBean
    private DetailRepository detailRepository;

    @Test
    void index_shouldReturnHistoryView() throws Exception {
        when(historyRepository.findAllByOrderByCalculationDateDesc()).thenReturn(Arrays.asList());
        
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("History"))
                .andExpect(model().attributeExists("history"));
    }

    @Test
    void newData_shouldReturnInDataView() throws Exception {
        mockMvc.perform(get("/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("InData"))
                .andExpect(model().attributeExists("inData"));
    }

    @Test
    void info_shouldCalculateAndReturnOutDataView() throws Exception {
        // given
        com.example.rectificat.model.OutData outData = new com.example.rectificat.model.OutData();
        outData.setAbsoluteAlcohol(7600);
        outData.setHeadFactions(608);
        outData.setHeads(228);
        outData.setCommercialAlcohol(4940);
        outData.setTails(266);
        outData.setHeadsAndCommercialAlcohol(380);

        List<String> resultList = Arrays.asList("Результат 1", "Результат 2");

        when(service.calc(any(InData.class))).thenReturn(outData);
        when(service.resultToStringForHtml()).thenReturn(resultList);

        // when & then
        mockMvc.perform(post("/info")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("amountOfRawAlcohol", "19")
                        .param("alcoholStrength", "40")
                        .param("power", "0.6")
                        .param("water", "25"))
                .andExpect(status().isOk())
                .andExpect(view().name("OutData"))
                .andExpect(model().attributeExists("outData"))
                .andExpect(model().attributeExists("result"));
    }

    @Test
    void info_shouldPassInDataToService() throws Exception {
        // given
        com.example.rectificat.model.OutData outData = new com.example.rectificat.model.OutData();
        outData.setAbsoluteAlcohol(5000);

        when(service.calc(any(InData.class))).thenReturn(outData);
        when(service.resultToStringForHtml()).thenReturn(Arrays.asList("test"));

        // when & then
        mockMvc.perform(post("/info")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("amountOfRawAlcohol", "10")
                        .param("alcoholStrength", "50")
                        .param("power", "1.0")
                        .param("water", "100"))
                .andExpect(status().isOk());
    }
}
