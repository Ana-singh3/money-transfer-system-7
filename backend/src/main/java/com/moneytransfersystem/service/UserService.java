package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.entities.User;

public interface UserService {
    User getCurrentUser();
    boolean isAdmin(User user);
    boolean isAdmin();
}

