package main.java.com.sportsaura.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportsaura.user_service.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
