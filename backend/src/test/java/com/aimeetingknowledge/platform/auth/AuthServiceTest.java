package com.aimeetingknowledge.platform.auth;

import com.aimeetingknowledge.platform.auth.dto.LoginRequest;
import com.aimeetingknowledge.platform.auth.dto.RegisterRequest;
import com.aimeetingknowledge.platform.auth.dto.UserResponse;
import com.aimeetingknowledge.platform.security.JwtService;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersUserWithNormalizedEmailAndHashedPassword() {
        RegisterRequest request = new RegisterRequest(" John Doe ", "John@Example.COM", "Password@123");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("John Doe");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(response.email()).isEqualTo("john@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "Password@123");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void logsInWithCorrectPasswordAndReturnsToken() {
        User user = new User("John Doe", "john@example.com", "bcrypt-hash");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken("john@example.com")).thenReturn("signed-jwt");

        var response = authService.login(new LoginRequest("John@Example.COM", "Password@123"));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.name()).isEqualTo("John Doe");
    }

    @Test
    void rejectsInvalidPasswordWithoutRevealingWhichCredentialFailed() {
        User user = new User("John Doe", "john@example.com", "bcrypt-hash");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword@123", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "WrongPassword@123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }
}
