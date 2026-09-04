package com.aimeetingknowledge.platform.meeting;

import com.aimeetingknowledge.platform.config.AppCorsProperties;
import com.aimeetingknowledge.platform.config.SecurityConfig;
import com.aimeetingknowledge.platform.meeting.dto.CreateMeetingRequest;
import com.aimeetingknowledge.platform.meeting.dto.MeetingResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, MeetingControllerSecurityTest.PropertiesConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class MeetingControllerSecurityTest {

    private static final String TOKEN = "valid-jwt";
    private static final String EMAIL = "owner@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void rejectsUnauthenticatedMeetingRequest() throws Exception {
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateMeeting() throws Exception {
        authenticateUser();
        MeetingResponse response = new MeetingResponse(
                1L, "Team sync", "Weekly planning", Instant.parse("2030-01-15T10:00:00Z"),
                MeetingStatus.CREATED, Instant.parse("2030-01-01T10:00:00Z")
        );
        when(meetingService.createMeeting(any(CreateMeetingRequest.class), eq(EMAIL))).thenReturn(response);

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Team sync","description":"Weekly planning","meetingDate":"2030-01-15T10:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void returnsValidationErrorForInvalidMeeting() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","meetingDate":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required."))
                .andExpect(jsonPath("$.fieldErrors.meetingDate").value("Meeting date is required."));
    }

    private void authenticateUser() {
        User user = new User("Owner", EMAIL, "hash");
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @TestConfiguration
    @EnableConfigurationProperties(AppCorsProperties.class)
    static class PropertiesConfig {
    }
}
