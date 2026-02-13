package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.repository.UserRepository;
import com.moneytransfersystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Authentication failed | method=getCurrentUser | reason=NOT_AUTHENTICATED");
            throw new UsernameNotFoundException("User not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found | method=getCurrentUser | username={}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

    @Override
    public boolean isAdmin(User user) {
        return user.getRole() == Role.ROLE_ADMIN;
    }

    @Override
    public boolean isAdmin() {
        User currentUser = getCurrentUser();
        return isAdmin(currentUser);
    }
}
