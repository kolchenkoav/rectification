package com.example.rectificat.controller;

import com.example.rectificat.SecurityConfiguration;
import com.example.rectificat.model.Detail;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;
import com.example.rectificat.model.RectificationHistory;
import com.example.rectificat.services.RectificationService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RectificationController.class)
@Import({SecurityConfiguration.class, RectificationControllerTest.TestSecurityUserConfiguration.class})
class RectificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RectificationService service;

    @TestConfiguration
    static class TestSecurityUserConfiguration {

        @Bean
        UserDetailsService testUserDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("test-user")
                            .password("{noop}test-password")
                            .roles("USER")
                            .build()
            );
        }
    }

    private static Stream<MockHttpServletRequestBuilder> mutatingPostRequests() {
        return Stream.of(
                postInfoRequest(),
                post("/delete/1"),
                post("/clear"),
                postAddDetailRequest(),
                post("/view/1/detail/2/delete"),
                postActualRequest()
        );
    }

    private static MockHttpServletRequestBuilder postInfoRequest() {
        return postInfoRequest("19", "40", "0.6", "25");
    }

    private static MockHttpServletRequestBuilder postInfoRequest(String amountOfRawAlcohol,
                                                                String alcoholStrength,
                                                                String power,
                                                                String water) {
        return post("/info")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("amountOfRawAlcohol", amountOfRawAlcohol)
                .param("alcoholStrength", alcoholStrength)
                .param("power", power)
                .param("water", water);
    }

    private static MockHttpServletRequestBuilder postInfoRequestWithoutWater() {
        return post("/info")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("amountOfRawAlcohol", "19")
                .param("alcoholStrength", "40")
                .param("power", "0.6");
    }

    private static Stream<Arguments> invalidInfoRequests() {
        return Stream.of(
                Arguments.of("0", "40", "0.6", "25", "amountOfRawAlcohol"),
                Arguments.of("-1", "40", "0.6", "25", "amountOfRawAlcohol"),
                Arguments.of("1001", "40", "0.6", "25", "amountOfRawAlcohol"),
                Arguments.of("19.5", "40", "0.6", "25", "amountOfRawAlcohol"),
                Arguments.of("19", "0", "0.6", "25", "alcoholStrength"),
                Arguments.of("19", "0.09", "0.6", "25", "alcoholStrength"),
                Arguments.of("19", "101", "0.6", "25", "alcoholStrength"),
                Arguments.of("19", "NaN", "0.6", "25", "alcoholStrength"),
                Arguments.of("19", "Infinity", "0.6", "25", "alcoholStrength"),
                Arguments.of("19", "40", "0", "25", "power"),
                Arguments.of("19", "40", "0.09", "25", "power"),
                Arguments.of("19", "40", "-0.1", "25", "power"),
                Arguments.of("19", "40", "NaN", "25", "power"),
                Arguments.of("19", "40", "Infinity", "25", "power"),
                Arguments.of("19", "40", "0.6", "-1", "water"),
                Arguments.of("19", "40", "0.6", "10001", "water")
        );
    }

    private static Stream<Arguments> invalidDetailTemperatureRequests() {
        return Stream.of(
                Arguments.of("NaN", "77.1", "22.3", "18.0"),
                Arguments.of("78.5", "Infinity", "22.3", "18.0"),
                Arguments.of("78.5", "77.1", "151", "18.0"),
                Arguments.of("78.5", "77.1", "22.3", "-51")
        );
    }

    private static Stream<Arguments> invalidActualRequests() {
        return Stream.of(
                Arguments.of("0", "230", "260"),
                Arguments.of("-1", "230", "260"),
                Arguments.of("NaN", "230", "260"),
                Arguments.of("Infinity", "230", "260"),
                Arguments.of("100001", "230", "260"),
                Arguments.of("4900", "-1", "260"),
                Arguments.of("4900", "NaN", "260"),
                Arguments.of("4900", "100001", "260"),
                Arguments.of("4900", "230", "-1"),
                Arguments.of("4900", "230", "Infinity"),
                Arguments.of("4900", "230", "100001")
        );
    }

    private static MockHttpServletRequestBuilder postAddDetailRequest() {
        return postAddDetailRequest("78.5", "77.1", "22.3", "18.0");
    }

    private static MockHttpServletRequestBuilder postAddDetailRequest(String temperatureCube,
                                                                     String temperatureTsar,
                                                                     String temperatureAtmosphere,
                                                                     String temperatureWater) {
        return post("/view/1/detail")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("temperatureCube", temperatureCube)
                .param("temperatureTsar", temperatureTsar)
                .param("temperatureAtmosphere", temperatureAtmosphere)
                .param("temperatureWater", temperatureWater);
    }

    private static MockHttpServletRequestBuilder postActualRequest() {
        return postActualRequest("4900", "230", "260");
    }

    private static MockHttpServletRequestBuilder postActualRequest(String actualCommercialAlcohol,
                                                                  String actualHeads,
                                                                  String actualTails) {
        return post("/view/1/actual")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("actualCommercialAlcohol", actualCommercialAlcohol)
                .param("actualHeads", actualHeads)
                .param("actualTails", actualTails);
    }

    private static RectificationHistory sampleHistory() {
        RectificationHistory history = new RectificationHistory(19, 40.0, 0.6, 25);
        history.setId(1L);
        history.setResultSnapshot(sampleOutData());
        return history;
    }

    private static RectificationHistory sampleHistoryWithDriftedSnapshot() {
        RectificationHistory history = new RectificationHistory(1, 1.0, 1.0, 0);
        history.setId(1L);
        history.setResultSnapshot(sampleOutData());
        return history;
    }

    private static RectificationHistory sampleHistoryWithDetail() {
        RectificationHistory history = sampleHistory();
        Detail detail = new Detail(78.5, 77.1, 22.3, 18.0);
        detail.setId(2L);
        history.setDetails(List.of(detail));
        return history;
    }

    private static RectificationHistory sampleHistoryWithPartialActuals() {
        RectificationHistory history = sampleHistory();
        history.setActualData(null, 250.0, null);
        return history;
    }

    private static RectificationHistory sampleHistoryWithTwoPartialActuals() {
        RectificationHistory history = sampleHistory();
        history.setActualData(510.0, 250.0, null);
        return history;
    }

    private static RectificationHistory sampleHistoryWithCommercialAlcoholActual() {
        RectificationHistory history = sampleHistory();
        history.setActualData(5146.0, null, null);
        return history;
    }

    private static RectificationHistory sampleHistoryWithActualTails() {
        RectificationHistory history = sampleHistory();
        history.setActualData(null, null, 300.0);
        return history;
    }

    private static OutData sampleOutData() {
        OutData outData = new OutData();
        outData.setAbsoluteAlcohol(7600);
        outData.setHeadFactions(608);
        outData.setHeads(228);
        outData.setCommercialAlcohol(4940);
        outData.setTails(266);
        outData.setHeadsAndCommercialAlcohol(380);
        return outData;
    }

    private static void assertPostFormsHaveCsrfTokens(String html, int expectedPostForms) {
        int postFormCount = 0;
        int index = 0;
        while ((index = html.indexOf("<form", index)) != -1) {
            int formStartTagEnd = html.indexOf(">", index);
            int formEnd = html.indexOf("</form>", formStartTagEnd);
            assertTrue(formStartTagEnd > index && formEnd > formStartTagEnd);

            String formStartTag = html.substring(index, formStartTagEnd).toLowerCase();
            String formBody = html.substring(formStartTagEnd, formEnd);
            if (formStartTag.contains("method=\"post\"") || formStartTag.contains("method=\"POST\"".toLowerCase())) {
                postFormCount++;
                assertTrue(formBody.contains("name=\"_csrf\""));
                assertTrue(formBody.matches("(?s).*<input[^>]*name=\"_csrf\"[^>]*value=\"[^\"]+\"[^>]*>.*"));
            }
            index = formEnd + "</form>".length();
        }
        assertEquals(expectedPostForms, postFormCount);
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }

    @ParameterizedTest
    @MethodSource("mutatingPostRequests")
    void mutatingPost_shouldRedirectAnonymousUserToLogin(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @MethodSource("mutatingPostRequests")
    void authenticatedMutatingPostWithoutCsrf_shouldReturnForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(user("tester")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/new", "/info", "/view/1", "/print/1"})
    void applicationGetRequests_shouldRedirectAnonymousUserToLogin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginPage_shouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void loginPostWithCsrf_shouldAuthenticateConfiguredTestUser() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "test-user")
                        .param("password", "test-password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("test-user"));
    }

    @Test
    void logoutPostWithCsrf_shouldRedirectToLoginLogoutPage() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("test-user"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void staticResource_shouldRemainPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/grafik.png"))
                .andExpect(status().isOk());
    }

    @Test
    void missingStaticResource_shouldReturnNotFoundWithoutLoginRedirect() throws Exception {
        mockMvc.perform(get("/css/missing.css"))
                .andExpect(status().isNotFound());
    }

    @Test
    void errorDispatch_shouldBePermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        }))
                .andExpect(status().isNotFound());
    }

    @Test
    void forwardDispatch_shouldBePermittedWithoutAuthentication() throws Exception {
        when(service.getAllHistory()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/")
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.FORWARD);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(view().name("History"));
    }

    @Test
    @WithMockUser
    void index_shouldReturnHistoryView() throws Exception {
        when(service.getAllHistory()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("History"))
                .andExpect(model().attributeExists("history"));
    }

    @Test
    @WithMockUser
    void newData_shouldReturnInDataView() throws Exception {
        mockMvc.perform(get("/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("InData"))
                .andExpect(model().attributeExists("inData"));
    }

    @Test
    @WithMockUser
    void newDataRenderedHtml_shouldIncludeCsrfFieldForInfoForm() throws Exception {
        MvcResult result = mockMvc.perform(get("/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/info\"")))
                .andExpect(content().string(containsString("Спирт сырец (целые литры)")))
                .andExpect(content().string(containsString("step=\"1\"")))
                .andExpect(content().string(containsString("min=\"1\"")))
                .andExpect(content().string(containsString("max=\"1000\"")))
                .andReturn();

        assertPostFormsHaveCsrfTokens(result.getResponse().getContentAsString(), 1);
    }

    @Test
    @WithMockUser
    void historyRenderedHtml_shouldIncludeCsrfFieldsForClearAndDeleteForms() throws Exception {
        when(service.getAllHistory()).thenReturn(List.of(sampleHistory()));

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/clear\"")))
                .andExpect(content().string(containsString("action=\"/delete/1\"")))
                .andReturn();

        assertPostFormsHaveCsrfTokens(result.getResponse().getContentAsString(), 2);
    }

    @Test
    @WithMockUser
    void getInfo_shouldRedirectToNewDataForm() throws Exception {
        mockMvc.perform(get("/info"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/new"));
    }

    @Test
    @WithMockUser
    void viewHistory_shouldReturnOutDataViewUsingPersistedSnapshot() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithDriftedSnapshot()));

        MvcResult result = mockMvc.perform(get("/view/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("OutData"))
                .andExpect(model().attributeExists("inData"))
                .andExpect(model().attributeExists("outData"))
                .andExpect(model().attributeExists("details"))
                .andExpect(model().attribute("historyId", 1L))
                .andReturn();

        OutData outData = (OutData) result.getModelAndView().getModel().get("outData");
        assertEquals(7600.0, outData.getAbsoluteAlcohol());
        assertEquals(228.0, outData.getHeads());
        assertEquals(4940, outData.getCommercialAlcohol());
        assertEquals(266.0, outData.getTails());
        verify(service).getHistoryWithDetails(1L);
        verify(service, never()).calc(any(InData.class));
    }

    @Test
    @WithMockUser
    void viewHistory_withMissingId_shouldReturnNotFound() throws Exception {
        when(service.getHistoryWithDetails(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/view/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void viewHistoryRenderedHtml_shouldIncludeCsrfFieldsForActualDetailAndDeleteForms() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithDetail()));

        MvcResult result = mockMvc.perform(get("/view/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/view/1/actual\"")))
                .andExpect(content().string(containsString("action=\"/view/1/detail\"")))
                .andExpect(content().string(containsString("action=\"/view/1/detail/2/delete\"")))
                .andReturn();

        assertPostFormsHaveCsrfTokens(result.getResponse().getContentAsString(), 3);
    }

    @Test
    @WithMockUser
    void viewHistoryRenderedHtml_shouldShowValidationErrorFlashMessage() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistory()));

        mockMvc.perform(get("/view/1")
                        .flashAttr("errorMessage", "Температуры должны быть конечными числами от -50 до 150 °C."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert alert-danger")))
                .andExpect(content().string(containsString("Температуры должны быть конечными числами")));
    }

    @Test
    @WithMockUser
    void printHistory_shouldReturnPrintViewUsingPersistedSnapshot() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithDriftedSnapshot()));

        MvcResult result = mockMvc.perform(get("/print/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("Print"))
                .andExpect(model().attributeExists("inData"))
                .andExpect(model().attributeExists("outData"))
                .andExpect(model().attributeExists("details"))
                .andExpect(model().attribute("calcCommercialAlcohol", 5145.833333333333))
                .andExpect(model().attribute("calcHeads", 237.5))
                .andExpect(model().attribute("calcTails", 266.0))
                .andExpect(model().attributeExists("calculationDate"))
                .andReturn();

        OutData outData = (OutData) result.getModelAndView().getModel().get("outData");
        assertEquals(7600.0, outData.getAbsoluteAlcohol());
        assertEquals(228.0, outData.getHeads());
        assertEquals(4940, outData.getCommercialAlcohol());
        assertEquals(266.0, outData.getTails());
        verify(service).getHistoryWithDetails(1L);
        verify(service, never()).calc(any(InData.class));
    }

    @Test
    @WithMockUser
    void printHistory_withMissingId_shouldReturnNotFound() throws Exception {
        when(service.getHistoryWithDetails(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/print/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void printHistory_withPartialActuals_shouldRenderOnlyPresentActualFields() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithPartialActuals()));

        MvcResult result = mockMvc.perform(get("/print/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ФАКТ: 250 мл")))
                .andReturn();

        assertEquals(1, countOccurrences(result.getResponse().getContentAsString(), "ФАКТ:"));
    }

    @Test
    @WithMockUser
    void printHistory_withTwoPartialActuals_shouldRenderOnlyThoseTwoFields() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithTwoPartialActuals()));

        MvcResult result = mockMvc.perform(get("/print/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ФАКТ: 510 мл")))
                .andExpect(content().string(containsString("ФАКТ: 250 мл")))
                .andReturn();

        assertEquals(2, countOccurrences(result.getResponse().getContentAsString(), "ФАКТ:"));
    }

    @Test
    @WithMockUser
    void printHistory_shouldCompareActualCommercialAlcoholUsingDoubleArithmetic() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithCommercialAlcoholActual()));

        mockMvc.perform(get("/print/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("calcCommercialAlcohol", 5145.833333333333))
                .andExpect(content().string(containsString("Товарный спирт")))
                .andExpect(content().string(containsString("ОТКЛОНЕНИЕ: 0 мл")));
    }

    @Test
    @WithMockUser
    void printHistory_shouldCompareActualTailsToCalculatedTails() throws Exception {
        when(service.getHistoryWithDetails(1L)).thenReturn(Optional.of(sampleHistoryWithActualTails()));

        mockMvc.perform(get("/print/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("calcTails", 266.0))
                .andExpect(content().string(containsString("Хвосты (ВЫЛИТЬ):")))
                .andExpect(content().string(containsString("ОТКЛОНЕНИЕ: 34 мл")));
    }

    @Test
    @WithMockUser
    void info_shouldCalculateAndReturnOutDataView() throws Exception {
        // given
        OutData outData = new OutData();
        outData.setAbsoluteAlcohol(7600);
        outData.setHeadFactions(608);
        outData.setHeads(228);
        outData.setCommercialAlcohol(4940);
        outData.setTails(266);
        outData.setHeadsAndCommercialAlcohol(380);

        List<String> resultList = Arrays.asList("Результат 1", "Результат 2");

        RectificationHistory savedHistory = new RectificationHistory(19, 40.0, 0.6, 25);
        savedHistory.setResultSnapshot(outData);
        when(service.resultToStringForHtml(any(InData.class), any(OutData.class))).thenReturn(resultList);
        when(service.saveCalculation(any(InData.class))).thenReturn(savedHistory);

        // when & then
        MvcResult result = mockMvc.perform(postInfoRequest().with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("OutData"))
                .andExpect(model().attributeExists("outData"))
                .andExpect(model().attributeExists("result"))
                .andReturn();

        OutData modelOutData = (OutData) result.getModelAndView().getModel().get("outData");
        assertEquals(7600.0, modelOutData.getAbsoluteAlcohol());
        verify(service).saveCalculation(any(InData.class));
        verify(service, never()).calc(any(InData.class));
    }

    @Test
    @WithMockUser
    void info_shouldPassInDataToService() throws Exception {
        // given
        OutData outData = new OutData();
        outData.setAbsoluteAlcohol(5000);

        RectificationHistory savedHistory = new RectificationHistory(10, 50.0, 1.0, 100);
        savedHistory.setResultSnapshot(outData);
        when(service.resultToStringForHtml(any(InData.class), any(OutData.class))).thenReturn(Arrays.asList("test"));
        when(service.saveCalculation(any(InData.class))).thenReturn(savedHistory);

        // when & then
        mockMvc.perform(post("/info")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("amountOfRawAlcohol", "10")
                        .param("alcoholStrength", "50")
                        .param("power", "1.0")
                        .param("water", "100")
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(service).saveCalculation(any(InData.class));
    }

    @ParameterizedTest
    @MethodSource("invalidInfoRequests")
    @WithMockUser
    void info_withInvalidInput_shouldReturnFormAndNotCallServices(String amountOfRawAlcohol,
                                                                   String alcoholStrength,
                                                                   String power,
                                                                   String water,
                                                                   String invalidField) throws Exception {
        mockMvc.perform(postInfoRequest(amountOfRawAlcohol, alcoholStrength, power, water).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("InData"))
                .andExpect(model().attributeHasFieldErrors("inData", invalidField))
                .andExpect(content().string(containsString("Проверьте значения")));

        verify(service, never()).calc(any(InData.class));
        verify(service, never()).saveCalculation(any(InData.class));
    }

    @Test
    @WithMockUser
    void info_withDecimalRawAlcohol_shouldShowIntegerOnlyErrorMessageAndNotCallServices() throws Exception {
        mockMvc.perform(postInfoRequest("19.5", "40", "0.6", "25").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("InData"))
                .andExpect(model().attributeHasFieldErrors("inData", "amountOfRawAlcohol"))
                .andExpect(content().string(containsString("Спирт-сырец должен быть целым числом литров.")));

        verify(service, never()).calc(any(InData.class));
        verify(service, never()).saveCalculation(any(InData.class));
    }

    @Test
    @WithMockUser
    void info_withMissingWater_shouldReturnFormAndNotCallServices() throws Exception {
        mockMvc.perform(postInfoRequestWithoutWater().with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("InData"))
                .andExpect(model().attributeHasFieldErrors("inData", "water"))
                .andExpect(content().string(containsString("Вода обязательна")));

        verify(service, never()).calc(any(InData.class));
        verify(service, never()).saveCalculation(any(InData.class));
    }

    @Test
    @WithMockUser
    void info_shouldAcceptBoundaryInputValues() throws Exception {
        OutData outData = new OutData();
        outData.setAbsoluteAlcohol(1000);

        RectificationHistory savedHistory = new RectificationHistory(1000, 0.1, 0.1, 10000);
        savedHistory.setResultSnapshot(outData);
        when(service.resultToStringForHtml(any(InData.class), any(OutData.class))).thenReturn(Arrays.asList("test"));
        when(service.saveCalculation(any(InData.class))).thenReturn(savedHistory);

        mockMvc.perform(postInfoRequest("1000", "0.1", "0.1", "10000").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("OutData"));

        verify(service).saveCalculation(any(InData.class));
    }

    @Test
    @WithMockUser
    void info_shouldAllowZeroWaterBecauseWaterCanBeAbsent() throws Exception {
        // Water in the take-off node is optional in the current UI/domain, so 0 ml is valid.
        OutData outData = new OutData();
        outData.setAbsoluteAlcohol(5000);

        RectificationHistory savedHistory = new RectificationHistory(10, 50.0, 1.0, 0);
        savedHistory.setResultSnapshot(outData);
        when(service.resultToStringForHtml(any(InData.class), any(OutData.class))).thenReturn(Arrays.asList("test"));
        when(service.saveCalculation(any(InData.class))).thenReturn(savedHistory);

        mockMvc.perform(postInfoRequest("10", "50", "1.0", "0").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("OutData"));

        verify(service).saveCalculation(any(InData.class));
    }

    @Test
    @WithMockUser
    void clear_shouldReachControllerWithAuthenticatedUserAndCsrf() throws Exception {
        mockMvc.perform(post("/clear").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        verify(service).clearAllHistory();
    }

    @Test
    @WithMockUser
    void deleteHistory_shouldReachControllerWithAuthenticatedUserAndCsrf() throws Exception {
        mockMvc.perform(post("/delete/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        verify(service).deleteHistory(1L);
    }

    @Test
    @WithMockUser
    void addDetail_shouldReachControllerWithAuthenticatedUserAndCsrf() throws Exception {
        when(service.historyExists(1L)).thenReturn(true);
        when(service.addDetail(1L, 78.5, 77.1, 22.3, 18.0)).thenReturn(true);

        mockMvc.perform(postAddDetailRequest().with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"));
        verify(service).addDetail(1L, 78.5, 77.1, 22.3, 18.0);
    }

    @Test
    @WithMockUser
    void addDetail_whenHistoryMissing_shouldReturnNotFound() throws Exception {
        when(service.historyExists(404L)).thenReturn(false);

        mockMvc.perform(post("/view/404/detail")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("temperatureCube", "78.5")
                        .param("temperatureTsar", "77.1")
                        .param("temperatureAtmosphere", "22.3")
                        .param("temperatureWater", "18.0")
                        .with(csrf()))
                .andExpect(status().isNotFound());
        verify(service, never()).addDetail(any(Long.class), any(Double.class), any(Double.class), any(Double.class), any(Double.class));
    }

    @Test
    @WithMockUser
    void addDetail_whenHistoryMissingAndPayloadInvalid_shouldReturnNotFound() throws Exception {
        when(service.historyExists(404L)).thenReturn(false);

        mockMvc.perform(post("/view/404/detail")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("temperatureCube", "")
                        .param("temperatureTsar", "NaN")
                        .param("temperatureAtmosphere", "151")
                        .param("temperatureWater", "-51")
                        .with(csrf()))
                .andExpect(status().isNotFound());
        verify(service, never()).addDetail(any(Long.class), any(Double.class), any(Double.class), any(Double.class), any(Double.class));
    }

    @ParameterizedTest
    @MethodSource("invalidDetailTemperatureRequests")
    @WithMockUser
    void addDetail_withInvalidTemperature_shouldRedirectWithErrorAndNotCallService(String temperatureCube,
                                                                                   String temperatureTsar,
                                                                                   String temperatureAtmosphere,
                                                                                   String temperatureWater) throws Exception {
        when(service.historyExists(1L)).thenReturn(true);

        mockMvc.perform(postAddDetailRequest(temperatureCube, temperatureTsar, temperatureAtmosphere, temperatureWater)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"))
                .andExpect(flash().attribute("errorMessage", containsString("Температуры")));

        verify(service, never()).addDetail(any(Long.class), any(Double.class), any(Double.class), any(Double.class), any(Double.class));
    }

    @Test
    @WithMockUser
    void deleteDetail_shouldReachControllerWithAuthenticatedUserAndCsrf() throws Exception {
        when(service.deleteDetail(1L, 2L)).thenReturn(true);

        mockMvc.perform(post("/view/1/detail/2/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"));
        verify(service).deleteDetail(1L, 2L);
    }

    @Test
    @WithMockUser
    void deleteDetail_whenDetailMissingOrMismatched_shouldReturnNotFound() throws Exception {
        when(service.deleteDetail(1L, 2L)).thenReturn(false);

        mockMvc.perform(post("/view/1/detail/2/delete").with(csrf()))
                .andExpect(status().isNotFound());
        verify(service).deleteDetail(1L, 2L);
    }

    @Test
    @WithMockUser
    void saveActualData_shouldReachControllerWithAuthenticatedUserAndCsrf() throws Exception {
        when(service.historyExists(1L)).thenReturn(true);
        when(service.saveActualData(1L, 4900.0, 230.0, 260.0)).thenReturn(true);

        mockMvc.perform(postActualRequest().with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"));
        verify(service).saveActualData(1L, 4900.0, 230.0, 260.0);
    }

    @Test
    @WithMockUser
    void saveActualData_whenHistoryMissing_shouldReturnNotFound() throws Exception {
        when(service.historyExists(404L)).thenReturn(false);

        mockMvc.perform(post("/view/404/actual")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("actualCommercialAlcohol", "4900")
                        .param("actualHeads", "230")
                        .param("actualTails", "260")
                        .with(csrf()))
                .andExpect(status().isNotFound());
        verify(service, never()).saveActualData(any(Long.class), any(Double.class), any(Double.class), any(Double.class));
    }

    @Test
    @WithMockUser
    void saveActualData_whenHistoryMissingAndPayloadInvalid_shouldReturnNotFound() throws Exception {
        when(service.historyExists(404L)).thenReturn(false);

        mockMvc.perform(post("/view/404/actual")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("actualCommercialAlcohol", "0")
                        .param("actualHeads", "NaN")
                        .param("actualTails", "Infinity")
                        .with(csrf()))
                .andExpect(status().isNotFound());
        verify(service, never()).saveActualData(any(Long.class), any(Double.class), any(Double.class), any(Double.class));
    }

    @Test
    @WithMockUser
    void saveActualData_withPartialValues_shouldSaveNullsForBlankFields() throws Exception {
        when(service.historyExists(1L)).thenReturn(true);
        when(service.saveActualData(1L, null, 230.0, null)).thenReturn(true);

        mockMvc.perform(postActualRequest("", "230", "").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"));
        verify(service).saveActualData(1L, null, 230.0, null);
    }

    @Test
    @WithMockUser
    void saveActualData_withTwoPartialValues_shouldSaveOnlyProvidedFields() throws Exception {
        when(service.historyExists(1L)).thenReturn(true);
        when(service.saveActualData(1L, 4900.0, null, 260.0)).thenReturn(true);

        mockMvc.perform(postActualRequest("4900", "", "260").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"));
        verify(service).saveActualData(1L, 4900.0, null, 260.0);
    }

    @Test
    @WithMockUser
    void saveActualData_withNoValues_shouldClearActualValues() throws Exception {
        when(service.historyExists(1L)).thenReturn(true);
        when(service.saveActualData(1L, null, null, null)).thenReturn(true);

        mockMvc.perform(postActualRequest("", "", "").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"));
        verify(service).saveActualData(1L, null, null, null);
    }

    @ParameterizedTest
    @MethodSource("invalidActualRequests")
    @WithMockUser
    void saveActualData_withInvalidValues_shouldRedirectWithErrorAndNotCallService(String actualCommercialAlcohol,
                                                                                   String actualHeads,
                                                                                   String actualTails) throws Exception {
        when(service.historyExists(1L)).thenReturn(true);

        mockMvc.perform(postActualRequest(actualCommercialAlcohol, actualHeads, actualTails)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/1"))
                .andExpect(flash().attribute("errorMessage", containsString("Фактический товарный спирт")));

        verify(service, never()).saveActualData(any(Long.class), any(Double.class), any(Double.class), any(Double.class));
    }
}
