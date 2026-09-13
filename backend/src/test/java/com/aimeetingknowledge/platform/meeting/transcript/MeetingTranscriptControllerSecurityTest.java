package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.config.AppCorsProperties;
import com.aimeetingknowledge.platform.config.SecurityConfig;
import com.aimeetingknowledge.platform.meeting.MeetingNotFoundException;
import com.aimeetingknowledge.platform.meeting.transcript.dto.MeetingTranscriptResponse;
import com.aimeetingknowledge.platform.meeting.transcript.dto.TranscriptSegmentResponse;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingTranscriptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, MeetingTranscriptControllerSecurityTest.PropertiesConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class MeetingTranscriptControllerSecurityTest {

    private static final String TOKEN = "valid-jwt";
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingTranscriptService meetingTranscriptService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void rejectsUnauthenticatedTranscriptRequest() throws Exception {
        mockMvc.perform(get("/api/meetings/1/transcript"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTranscriptForAuthenticatedOwner() throws Exception {
        authenticateUser(OWNER_EMAIL);

        MeetingTranscriptResponse response = new MeetingTranscriptResponse(
                10L,
                1L,
                "Transcribed text",
                "en",
                List.of(new TranscriptSegmentResponse(0, 0.0, 2.5, "Transcribed text")),
                Instant.parse("2030-01-01T10:00:00Z")
        );

        when(meetingTranscriptService.getTranscript(1L, OWNER_EMAIL)).thenReturn(response);

        mockMvc.perform(get("/api/meetings/1/transcript")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.meetingId").value(1))
                .andExpect(jsonPath("$.fullText").value("Transcribed text"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.segments[0].segmentOrder").value(0))
                .andExpect(jsonPath("$.segments[0].startTime").value(0.0))
                .andExpect(jsonPath("$.segments[0].endTime").value(2.5))
                .andExpect(jsonPath("$.segments[0].text").value("Transcribed text"));
    }

    @Test
    void returns404WhenTranscriptDoesNotExist() throws Exception {
        authenticateUser(OWNER_EMAIL);
        when(meetingTranscriptService.getTranscript(1L, OWNER_EMAIL))
                .thenThrow(new TranscriptNotFoundException());

        mockMvc.perform(get("/api/meetings/1/transcript")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transcript was not found for this meeting."));
    }

    @Test
    void returns404WhenRequestingAnotherUsersMeetingTranscript() throws Exception {
        authenticateUser(OTHER_EMAIL);
        when(meetingTranscriptService.getTranscript(1L, OTHER_EMAIL))
                .thenThrow(new MeetingNotFoundException());

        mockMvc.perform(get("/api/meetings/1/transcript")
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
