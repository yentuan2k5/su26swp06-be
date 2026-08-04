package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void cannotPromoteAnotherUserWhenAdminAlreadyExists() {
        User existingAdmin = user(1L, User.Role.ADMIN);
        User researcher = user(2L, User.Role.RESEARCHER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(researcher));
        when(userRepository.findByRoleForUpdate(User.Role.ADMIN))
                .thenReturn(List.of(existingAdmin));

        assertThrows(
                BadRequestException.class,
                () -> userService.updateUserRole(2L, "ADMIN"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void cannotDemoteTheOnlyAdmin() {
        User admin = user(1L, User.Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByRoleForUpdate(User.Role.ADMIN))
                .thenReturn(List.of(admin));

        assertThrows(
                BadRequestException.class,
                () -> userService.updateUserRole(1L, "RESEARCHER"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void cannotDeleteTheOnlyAdmin() {
        User admin = user(1L, User.Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByRoleForUpdate(User.Role.ADMIN))
                .thenReturn(List.of(admin));

        assertThrows(
                BadRequestException.class,
                () -> userService.deleteUser(1L));

        verify(userRepository, never()).delete(any(User.class));
    }

    private User user(Long userId, User.Role role) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("user-" + userId);
        user.setRole(role);
        return user;
    }
}
