package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.response.UserResponse;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = findUser(userId);
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String keyword) {
        String safeKeyword = normalize(keyword);

        if (safeKeyword == null) {
            return getAllUsers();
        }

        return userRepository.searchUsers(safeKeyword)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, String role) {
        User user = findUser(userId);
        User.Role newRole = parseRole(role);
        User.Role currentRole = user.getRole();

        if (currentRole != newRole) {
            List<User> admins = userRepository.findByRoleForUpdate(User.Role.ADMIN);

            if (newRole == User.Role.ADMIN && currentRole != User.Role.ADMIN) {
                throw new BadRequestException(
                        "Hệ thống chỉ cho phép một tài khoản ADMIN");
            }

            if (currentRole == User.Role.ADMIN
                    && newRole != User.Role.ADMIN
                    && admins.size() <= 1) {
                throw new BadRequestException(
                        "Không thể đổi role của admin duy nhất trong hệ thống");
            }
        }

        user.setRole(newRole);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);

        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository
                    .findByRoleForUpdate(User.Role.ADMIN)
                    .size();

            if (adminCount <= 1) {
                throw new BadRequestException(
                        "Không thể xóa admin duy nhất trong hệ thống");
            }
        }

        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User.Role parseRole(String role) {
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Role không hợp lệ. Role hợp lệ: ADMIN, LECTURER, STUDENT, RESEARCHER");
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
