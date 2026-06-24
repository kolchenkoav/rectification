package com.example.rectification.services;

import com.example.rectification.model.InData;
import com.example.rectification.model.OutData;
import com.example.rectification.model.RectificationHistory;
import com.example.rectification.repository.DetailRepository;
import com.example.rectification.repository.RectificationHistoryRepository;
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
        service = new RectificationServiceImpl(historyRepository, detailRepository, new RectificationCalculator());
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
    void calc_shouldCalculateHeadFractions() {
        // given
        InData inData = new InData(19, 40.0, 0.6, 25);

        // when
        OutData result = service.calc(inData);

        // then: головные фракции = 7600 * 0.08 = 608
        assertEquals(608, result.getHeadFractions());
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
        assertEquals(0, result.getHeadFractions());
        assertEquals(0, result.getHeads());
        assertEquals(0, result.getCommercialAlcohol());
        assertEquals(0, result.getTails());
    }

    @Test
    void saveCalculation_shouldSaveHistoryWithSnapshotValues() {
        // given
        InData inData = new InData(10, 50.0, 1.0, 100);
        when(historyRepository.save(any(RectificationHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        RectificationHistory result = service.saveCalculation(inData);

        // then
        assertNotNull(result);
        assertEquals(5000.0, result.getAbsoluteAlcohol());
        assertEquals(150.0, result.getHeads());
        assertEquals(3250, result.getCommercialAlcohol());
        assertEquals(175.0, result.getTails());
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

    @Test
    void addDetail_whenHistoryExists_shouldSaveDetailAndReturnTrue() {
        RectificationHistory history = new RectificationHistory(19, 40.0, 0.6, 25);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        boolean result = service.addDetail(1L, 78.5, 77.1, 22.3, 18.0);

        assertTrue(result);
        assertEquals(1, history.getDetails().size());
        verify(historyRepository).save(history);
    }

    @Test
    void addDetail_whenHistoryMissing_shouldNotSaveAndReturnFalse() {
        when(historyRepository.findById(404L)).thenReturn(Optional.empty());

        boolean result = service.addDetail(404L, 78.5, 77.1, 22.3, 18.0);

        assertFalse(result);
        verify(historyRepository, never()).save(any(RectificationHistory.class));
    }

    @Test
    void saveActualData_withPartialValues_shouldPersistOnlyProvidedFieldsAndReturnTrue() {
        RectificationHistory history = new RectificationHistory(19, 40.0, 0.6, 25);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        boolean result = service.saveActualData(1L, 4900.0, null, 260.0);

        assertTrue(result);
        assertEquals(4900.0, history.getActualCommercialAlcohol());
        assertNull(history.getActualHeads());
        assertEquals(260.0, history.getActualTails());
        assertTrue(history.hasActualData());
        verify(historyRepository).save(history);
    }

    @Test
    void saveActualData_withNullValues_shouldClearExistingActualFieldsAndReturnTrue() {
        RectificationHistory history = new RectificationHistory(19, 40.0, 0.6, 25);
        history.setActualData(4900.0, 230.0, 260.0);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        boolean result = service.saveActualData(1L, null, null, null);

        assertTrue(result);
        assertNull(history.getActualCommercialAlcohol());
        assertNull(history.getActualHeads());
        assertNull(history.getActualTails());
        assertFalse(history.hasActualData());
        verify(historyRepository).save(history);
    }

    @Test
    void saveActualData_whenHistoryMissing_shouldNotSaveAndReturnFalse() {
        when(historyRepository.findById(404L)).thenReturn(Optional.empty());

        boolean result = service.saveActualData(404L, 4900.0, 230.0, 260.0);

        assertFalse(result);
        verify(historyRepository, never()).save(any(RectificationHistory.class));
    }

    @Test
    void deleteDetail_whenDetailBelongsToHistory_shouldDeleteAndReturnTrue() {
        when(detailRepository.deleteByIdAndHistoryId(2L, 1L)).thenReturn(1L);

        boolean result = service.deleteDetail(1L, 2L);

        assertTrue(result);
        verify(detailRepository).deleteByIdAndHistoryId(2L, 1L);
        verify(detailRepository, never()).deleteById(2L);
    }

    @Test
    void deleteDetail_whenDetailMissingOrMismatched_shouldNotDeleteAndReturnFalse() {
        when(detailRepository.deleteByIdAndHistoryId(2L, 1L)).thenReturn(0L);

        boolean result = service.deleteDetail(1L, 2L);

        assertFalse(result);
        verify(detailRepository).deleteByIdAndHistoryId(2L, 1L);
        verify(detailRepository, never()).deleteById(2L);
    }
}
