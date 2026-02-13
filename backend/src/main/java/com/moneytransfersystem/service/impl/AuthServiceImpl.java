package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.config.JwtTokenProvider;
import com.moneytransfersystem.domain.dtos.LoginRequest;
import com.moneytransfersystem.domain.dtos.LoginResponse;
import com.moneytransfersystem.domain.dtos.RegisterRequest;
import com.moneytransfersystem.domain.dtos.RegisterResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.domain.exceptions.UsernameAlreadyExistsException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.UserRepository;
import com.moneytransfersystem.service.AuthService;
import com.moneytransfersystem.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt | method=login | username={}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsernameWithAccounts(request.getUsername())
                .orElseThrow(() -> {
                    logger.error("Login failed | method=login | username={} | reason=USER_NOT_FOUND",
                            request.getUsername());
                    return new UsernameNotFoundException("User not found: " + request.getUsername());
                });

        String token = jwtTokenProvider.generateToken(authentication);

        String accountId = null;
        try {
            List<Account> accounts = user.getAccounts();
            if (accounts != null && !accounts.isEmpty()) {
                accountId = accounts.get(0).getId();
            }
        } catch (Exception e) {
            logger.warn("Login partial | method=login | username={} | reason=ACCOUNT_RESOLUTION_FAILED | error={}",
                    request.getUsername(), e.getMessage());
        }

        logger.info("Login success | method=login | username={} | role={}",
                user.getUsername(), user.getRole().name());

        return new LoginResponse(token, user.getUsername(), user.getRole().name(), accountId);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        logger.info("Registration attempt | method=register | username={}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            logger.error("Registration failed | method=register | username={} | reason=USERNAME_EXISTS",
                    request.getUsername());
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.ROLE_USER);
        newUser.setEnabled(true);

        User savedUser = userRepository.save(newUser);

        String accountId = IdGenerator.generateAccountId();
        Account newAccount = new Account(
                accountId,
                savedUser.getUsername(),
                BigDecimal.valueOf(500.00),
                AccountStatus.ACTIVE
        );
        newAccount.setUser(savedUser);
        accountRepository.save(newAccount);

        logger.info("Registration success | method=register | username={} | accountId={}",
                savedUser.getUsername(), accountId);

        return new RegisterResponse(
                "User registered successfully",
                savedUser.getUsername(),
                savedUser.getRole().name()
        );
    }
}
