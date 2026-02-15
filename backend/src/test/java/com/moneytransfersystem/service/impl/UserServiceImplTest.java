package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @InjectMocks private UserServiceImpl userService;

    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        normalUser = new User();
        normalUser.setId(1L);
        normalUser.setUsername("user1");
        normalUser.setRole(Role.ROLE_USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ROLE_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("returns user from repository")
            void returnsUser() {
                when(securityContext.getAuthentication()).thenReturn(authentication);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("user1");
                when(userRepository.findByUsername("user1")).thenReturn(Optional.of(normalUser));

                User result = userService.getCurrentUser();
                assertThat(result.getUsername()).isEqualTo("user1");
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("throws when authentication is null")
            void authenticationNull() {
                when(securityContext.getAuthentication()).thenReturn(null);
                assertThatThrownBy(() -> userService.getCurrentUser())
                        .isInstanceOf(UsernameNotFoundException.class)
                        .hasMessageContaining("not authenticated");
            }

            @Test
            @DisplayName("throws when not authenticated")
            void notAuthenticated() {
                when(securityContext.getAuthentication()).thenReturn(authentication);
                when(authentication.isAuthenticated()).thenReturn(false);
                assertThatThrownBy(() -> userService.getCurrentUser())
                        .isInstanceOf(UsernameNotFoundException.class);
            }

            @Test
            @DisplayName("throws when user not in repository")
            void userMissing() {
                when(securityContext.getAuthentication()).thenReturn(authentication);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("ghost");
                when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> userService.getCurrentUser())
                        .isInstanceOf(UsernameNotFoundException.class)
                        .hasMessageContaining("ghost");
            }
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("returns true for ROLE_ADMIN")
            void adminIsAdmin() {
                assertThat(userService.isAdmin(adminUser)).isTrue();
            }

            @Test
            @DisplayName("returns false for ROLE_USER")
            void userIsNotAdmin() {
                assertThat(userService.isAdmin(normalUser)).isFalse();
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {
            @Test
            @DisplayName("isAdmin() delegates to getCurrentUser")
            void parameterlessVersion() {
                when(securityContext.getAuthentication()).thenReturn(authentication);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("admin");
                when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

                assertThat(userService.isAdmin()).isTrue();
            }
        }
    }
}

