package com.warehouse.demo.controller;

import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.security.configuration.SecurityConfiguration;
import com.warehouse.demo.security.jwt.JWTAuthenticationFilter;
import com.warehouse.demo.service.UnitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UnitController.class)
@Import({SecurityConfiguration.class, UnitControllerTest.PassThroughJwtConfig.class})
class UnitControllerTest {

    private static final long UNIT_ID = 1L;
    private static final long MISSING_UNIT_ID = 9L;
    private static final String PIECE_NAME = "Piece";
    private static final String PIECE_CODE = "796";
    private static final String BLANK_NAME = " ";
    private static final String NAME_REQUIRED_MESSAGE = "Не заполнено имя единицы измерения";
    private static final String EDIT_FORBIDDEN_MESSAGE = "Editing a system-defined unit is forbidden";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnitService unitService;

    @Test
    @DisplayName("Listing units without authentication returns unauthorized")
    void givenAnonymousUser_whenFindAll_thenUnauthorized() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(get("/units/"));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "UNITS_READ")
    @DisplayName("Read authority can list units")
    void givenReadAuthority_whenFindAll_thenOk() throws Exception {
        // given
        when(unitService.findAll()).thenReturn(new UnitListResponse(List.of()));

        // when
        ResultActions result = mockMvc.perform(get("/units/"));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "UNITS_READ")
    @DisplayName("Read authority cannot create a unit")
    void givenReadAuthority_whenSave_thenForbidden() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(post("/units/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitJson(PIECE_NAME, PIECE_CODE)));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "UNITS_WRITE")
    @DisplayName("Write authority creates a unit and returns created")
    void givenWriteAuthority_whenSave_thenCreated() throws Exception {
        // given
        when(unitService.save(any())).thenReturn(new UnitResponse(UNIT_ID, PIECE_NAME, PIECE_CODE, null, false, false));

        // when
        ResultActions result = mockMvc.perform(post("/units/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitJson(PIECE_NAME, PIECE_CODE)));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "FULL_ACCESS")
    @DisplayName("Full access deletes a unit and returns no content")
    void givenFullAccess_whenDelete_thenNoContent() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(delete("/units/" + UNIT_ID));

        // then
        result.andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "UNITS_WRITE")
    @DisplayName("A blank unit name returns bad request")
    void givenBlankName_whenSave_thenBadRequest() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(post("/units/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitJson(BLANK_NAME, PIECE_CODE)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(NAME_REQUIRED_MESSAGE)));
    }

    @Test
    @WithMockUser(authorities = "UNITS_READ")
    @DisplayName("A missing unit returns not found")
    void givenMissingUnit_whenFindById_thenNotFound() throws Exception {
        // given
        when(unitService.findById(MISSING_UNIT_ID)).thenThrow(new EntityNotFoundException("missing unit"));

        // when
        ResultActions result = mockMvc.perform(get("/units/" + MISSING_UNIT_ID));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "UNITS_WRITE")
    @DisplayName("Marking a system-defined unit deleted returns forbidden")
    void givenSystemDefinedUnit_whenMarkDeleted_thenForbidden() throws Exception {
        // given
        when(unitService.markAsDeleted(UNIT_ID)).thenThrow(new BusinessLogicException(EDIT_FORBIDDEN_MESSAGE));

        // when
        ResultActions result = mockMvc.perform(patch("/units/" + UNIT_ID + "/mark-deleted"));

        // then
        result.andExpect(status().isForbidden());
    }

    private static String unitJson(String name, String code) {
        return """
                {"name":"%s","code":"%s"}
                """.formatted(name, code);
    }

    @TestConfiguration
    static class PassThroughJwtConfig {

        @Bean
        JWTAuthenticationFilter jwtAuthenticationFilter() {
            return new JWTAuthenticationFilter(null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}
