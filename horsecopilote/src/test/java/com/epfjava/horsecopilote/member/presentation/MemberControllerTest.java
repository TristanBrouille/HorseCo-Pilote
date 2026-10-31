package com.epfjava.horsecopilote.member.presentation;

import com.epfjava.horsecopilote.common.service.BusinessRuleException;
import com.epfjava.horsecopilote.member.service.MemberService;
import com.epfjava.horsecopilote.security.infrastructure.SecurityConfig;
import com.epfjava.horsecopilote.security.repository.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@WithMockUser(authorities = "ADMIN")
class MemberControllerTest {

    private static final String VALID_REQUEST = """
            {"firstName": "Léa", "lastName": "Petit", "email": "lea.petit@horseco.test",
             "password": "secret123", "galopLevel": 3, "heightCm": 160, "weightKg": 52.5}
            """;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    void createMemberReturns201WithLocation() throws Exception {
        when(memberService.createMember(any(MemberRequestDto.class))).thenReturn(member());

        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/members/13")))
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.galopLevel").value(3))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createMemberReturns400WhenFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName": "Léa", "lastName": "Petit", "email": "not-an-email",
                         "password": "short", "galopLevel": 8, "heightCm": 160, "weightKg": 52.5}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.galopLevel").exists());

        verify(memberService, never()).createMember(any());
    }

    @Test
    void createMemberReturns409WhenEmailIsAlreadyUsed() throws Exception {
        when(memberService.createMember(any(MemberRequestDto.class)))
                .thenThrow(new BusinessRuleException("Email already used: lea.petit@horseco.test"));

        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isConflict());
    }

    @Test
    void getMembersReturnsList() throws Exception {
        when(memberService.getMembers()).thenReturn(List.of(member()));

        mockMvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastName").value("Petit"));
    }

    @Test
    @WithMockUser(authorities = "COACH")
    void nonAdminCannotListOrCreateMembers() throws Exception {
        mockMvc.perform(get("/members")).andExpect(status().isForbidden());
        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    private static MemberResponseDto member() {
        return new MemberResponseDto(13L, "Léa", "Petit", "lea.petit@horseco.test", 3, 160, new BigDecimal("52.50"));
    }
}
