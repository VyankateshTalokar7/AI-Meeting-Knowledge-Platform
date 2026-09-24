package com.aimeetingknowledge.platform.health;

import com.aimeetingknowledge.platform.config.AppCorsProperties;
import com.aimeetingknowledge.platform.config.SecurityConfig;
import com.aimeetingknowledge.platform.security.JwtAuthenticationFilter;
import com.aimeetingknowledge.platform.security.JwtService;
import com.aimeetingknowledge.platform.security.RestAuthenticationEntryPoint;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, HealthControllerTest.PropertiesConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void returnsHealthStatusWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @TestConfiguration
    @EnableConfigurationProperties(AppCorsProperties.class)
    static class PropertiesConfig {
    }
}
