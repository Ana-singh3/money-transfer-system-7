package com.moneytransfersystem.config;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupDataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        List<Account> all = accountRepository.findAll();
        List<Account> toSave = new ArrayList<>();
        for (Account a : all) {
            if (a.getBalance() == null || a.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                a.setBalance(BigDecimal.valueOf(500).setScale(2, java.math.RoundingMode.HALF_UP));
                toSave.add(a);
            }
        }
        if (!toSave.isEmpty()) {
            accountRepository.saveAll(toSave);
        }
    }
}
