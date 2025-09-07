package com.sportsaura.user_service;

import org.springframework.stereotype.Service;

import com.sportsaura.user_service.entity.User;
import com.sportsaura.user_service.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User saveUser(User user) {
        return repository.save(user);
    }

    public User getUserByEmail(String email) {
        return repository.findByEmail(email);
    }
}
