package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.entity.User.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByUsernameContainingIgnoreCase(String keyword);

    Optional<User> findByGoogleId(String providerId);

    Optional<User> findByResetPasswordToken(String resetToken);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);
}
