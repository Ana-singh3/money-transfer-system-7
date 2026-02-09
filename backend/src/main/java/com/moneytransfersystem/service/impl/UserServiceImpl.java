package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.repository.UserRepository;
import com.moneytransfersystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("User not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
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

