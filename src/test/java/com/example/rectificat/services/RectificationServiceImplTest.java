package com.example.rectificat.services;

import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.repository.DetailRepository;
import com.example.rectificat.repository.RectificationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RectificationServiceImplTest {

    @Mock
    private RectificationHistoryRepository historyRepository;

    @Mock
    private DetailRepository detailRepository;

    private RectificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RectificationServiceImpl(historyRepository, detailRepository);
    }

    @Test
    void calc_shouldCalculateAbsoluteAlcohol() {
        // given: 19 литров спирта-сырца при крепости 40%
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: абсолютный спирт = 19 * 40% * 1000 = 7600 мл
        assertEquals(7600, result.getAbsoluteAlcohol());
    }

    @Test
    void calc_shouldCalculateHeadFactions() {
        // given
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: головные фракции = 7600 * 0.08 = 608
        assertEquals(608, result.getHeadFactions());
    }

    @Test
    void calc_shouldCalculateHeads() {
        // given
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: головы = 7600 * 0.03 = 228
        assertEquals(228, result.getHeads());
    }

    @Test
    void calc_shouldCalculateCommercialAlcohol() {
        // given
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: товарный спирт = 7600 * 0.65 = 4940
        assertEquals(4940, result.getCommercialAlcohol());
    }

    @Test
    void calc_shouldCalculateTails() {
        // given
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: хвосты = 7600 * 0.035 = 266
        assertEquals(266, result.getTails());
    }

    @Test
    void calc_shouldCalculateHeadsAndCommercialAlcohol() {
        // given
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: головы + ТС = 7600 * 0.05 = 380
        assertEquals(380, result.getHeadsAndCommercialAlcohol());
    }

    @Test
    void calc_shouldHandleZeroValues() {
        // given
        InData inData = new InData(0, 0.0, 0.0, 0);

        // when
        OutData result = service.calc(inData);

        // then: все значения должны быть 0
        assertEquals(0, result.getAbsoluteAlcohol());
        assertEquals(0, result.getHeadFactions());
        assertEquals(0, result.getHeads());
        assertEquals(0, result.getCommercialAlcohol());
        assertEquals(0, result.getTails());
    }

    @Test
    void resultToString_shouldReturnNonEmptyString() {
        // given
        InData inData = new InData(10, 50.0, 1.0, 100);
        OutData outData = service.calc(inData);

        // when
        String result = service.resultToString(inData, outData);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Абсолютный спирт"));
    }

    @Test
    void resultToStringForHtml_shouldReturnList() {
        // given
        InData inData = new InData(10, 50.0, 1.0, 100);
        OutData outData = service.calc(inData);

        // when
        var result = service.resultToStringForHtml(inData, outData);

        // then
        assertNotNull(result);
        assertTrue(result instanceof java.util.List);
    }

    @Test
    void saveCalculation_shouldSaveHistory() {
        // given
        InData inData = new InData(10, 50.0, 1.0, 100);
        RectificationHistory expectedHistory = new RectificationHistory(10, 50.0, 1.0, 100);
        when(historyRepository.save(any(RectificationHistory.class))).thenReturn(expectedHistory);

        // when
        RectificationHistory result = service.saveCalculation(inData);

        // then
        assertNotNull(result);
        verify(historyRepository, times(1)).save(any(RectificationHistory.class));
    }

    @Test
    void getHistoryWithDetails_shouldReturnHistory() {
        // given
        Long id = 1L;
        RectificationHistory history = new RectificationHistory(10, 50.0, 1.0, 100);
        when(historyRepository.findById(id)).thenReturn(Optional.of(history));

        // when
        Optional<RectificationHistory> result = service.getHistoryWithDetails(id);

        // then
        assertTrue(result.isPresent());
        assertEquals(history, result.get());
    }
}
