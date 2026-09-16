package com.aimeetingknowledge.platform.search;

import com.aimeetingknowledge.platform.config.AppCorsProperties;
import com.aimeetingknowledge.platform.config.SecurityConfig;
import com.aimeetingknowledge.platform.search.dto.SearchApiResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, SearchControllerSecurityTest.PropertiesConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class SearchControllerSecurityTest {

    private static final String TOKEN = "valid-jwt";
    private static final String USER_EMAIL = "user@example.com";

    @Autowired private MockMvc mockMvc;
    @MockBean private SearchService searchService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtService jwtService;

    @TestConfiguration
    @EnableConfigurationProperties(AppCorsProperties.class)
    static class PropertiesConfig {
    }

    @Test
    void unauthenticatedSearchReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is the status?\",\"topK\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSearchReturnsSearchResponse() throws Exception {
        User user = new User("User", USER_EMAIL, "hash");
        when(jwtService.extractEmail(TOKEN)).thenReturn(USER_EMAIL);

        when(jwtService.isTokenValid(TOKEN, USER_EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));

        SearchApiResponse response = new SearchApiResponse("The answer is X.", List.of());
        when(searchService.search(any(), eq(USER_EMAIL))).thenReturn(response);

        mockMvc.perform(post("/api/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is the status?\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The answer is X."))
                .andExpect(jsonPath("$.references").isArray());
    }
}
