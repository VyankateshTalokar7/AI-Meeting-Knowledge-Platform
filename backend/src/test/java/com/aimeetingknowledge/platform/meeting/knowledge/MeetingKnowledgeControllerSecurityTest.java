package com.aimeetingknowledge.platform.meeting.knowledge;

import com.aimeetingknowledge.platform.config.AppCorsProperties;
import com.aimeetingknowledge.platform.config.SecurityConfig;
import com.aimeetingknowledge.platform.meeting.MeetingNotFoundException;
import com.aimeetingknowledge.platform.meeting.knowledge.dto.ActionItemResponse;
import com.aimeetingknowledge.platform.meeting.knowledge.dto.MeetingKnowledgeResponse;
import com.aimeetingknowledge.platform.security.JwtAuthenticationFilter;
import com.aimeetingknowledge.platform.security.JwtService;
import com.aimeetingknowledge.platform.security.RestAuthenticationEntryPoint;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingKnowledgeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, MeetingKnowledgeControllerSecurityTest.PropertiesConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class MeetingKnowledgeControllerSecurityTest {

    private static final String TOKEN = "valid-jwt";
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingKnowledgeService meetingKnowledgeService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void rejectsUnauthenticatedKnowledgeRequest() throws Exception {
        mockMvc.perform(get("/api/meetings/1/knowledge"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsKnowledgeForAuthenticatedOwner() throws Exception {
        authenticateUser(OWNER_EMAIL);

        MeetingKnowledgeResponse response = new MeetingKnowledgeResponse(
                5L,
                1L,
                "Meeting summary",
                List.of("Topic 1"),
                List.of("Decision 1"),
                List.of(new ActionItemResponse(1L, "Task 1", "John", LocalDate.parse("2026-09-20"), 0)),
                Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T10:00:00Z")
        );

        when(meetingKnowledgeService.getKnowledge(1L, OWNER_EMAIL)).thenReturn(response);

        mockMvc.perform(get("/api/meetings/1/knowledge")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.meetingId").value(1))
                .andExpect(jsonPath("$.summary").value("Meeting summary"))
                .andExpect(jsonPath("$.keyTopics[0]").value("Topic 1"))
                .andExpect(jsonPath("$.decisions[0]").value("Decision 1"))
                .andExpect(jsonPath("$.actionItems[0].task").value("Task 1"))
                .andExpect(jsonPath("$.actionItems[0].assignee").value("John"))
                .andExpect(jsonPath("$.actionItems[0].dueDate").value("2026-09-20"));
    }

    @Test
    void returns404WhenKnowledgeDoesNotExist() throws Exception {
        authenticateUser(OWNER_EMAIL);
        when(meetingKnowledgeService.getKnowledge(1L, OWNER_EMAIL))
                .thenThrow(new MeetingKnowledgeNotFoundException());

        mockMvc.perform(get("/api/meetings/1/knowledge")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meeting knowledge was not found for this meeting."));
    }

    @Test
    void returns404WhenRequestingAnotherUsersMeetingKnowledge() throws Exception {
        authenticateUser(OTHER_EMAIL);
        when(meetingKnowledgeService.getKnowledge(1L, OTHER_EMAIL))
                .thenThrow(new MeetingNotFoundException());

        mockMvc.perform(get("/api/meetings/1/knowledge")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meeting was not found."));
    }

    private void authenticateUser(String email) {
        User user = new User("User", email, "hash");
        when(jwtService.extractEmail(TOKEN)).thenReturn(email);
        when(jwtService.isTokenValid(TOKEN, email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @TestConfiguration
    @EnableConfigurationProperties(AppCorsProperties.class)
    static class PropertiesConfig {
    }
}
