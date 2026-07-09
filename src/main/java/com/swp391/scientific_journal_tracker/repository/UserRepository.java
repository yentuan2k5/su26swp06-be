package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.entity.User.Role;

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

    @Query("""
            SELECT u
            FROM User u
            WHERE (:keyword IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<User> searchUsers(@Param("keyword") String keyword);
}
