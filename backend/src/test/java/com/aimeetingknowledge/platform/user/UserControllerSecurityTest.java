package com.aimeetingknowledge.platform.user;

import com.aimeetingknowledge.platform.config.AppCorsProperties;
import com.aimeetingknowledge.platform.config.SecurityConfig;
import com.aimeetingknowledge.platform.security.JwtAuthenticationFilter;
import com.aimeetingknowledge.platform.security.JwtService;
import com.aimeetingknowledge.platform.security.RestAuthenticationEntryPoint;
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

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, UserControllerSecurityTest.PropertiesConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class UserControllerSecurityTest {

    private static final String TOKEN = "valid-jwt";
    private static final String EMAIL = "john@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void rejectsProtectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource."));
    }

    @Test
    void returnsCurrentUserWithValidJwt() throws Exception {
        User user = new User("John Doe", EMAIL, "bcrypt-hash");
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + TOKEN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @TestConfiguration
    @EnableConfigurationProperties(AppCorsProperties.class)
    static class PropertiesConfig {
    }
}
