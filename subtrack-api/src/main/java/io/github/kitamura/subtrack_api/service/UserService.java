package io.github.kitamura.subtrack_api.service;

import io.github.kitamura.subtrack_api.dto.UserDto;
import io.github.kitamura.subtrack_api.entity.User;
import io.github.kitamura.subtrack_api.exception.CustomException;
import io.github.kitamura.subtrack_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // =====================================================
    // ユーザー登録
    // =====================================================
    @Transactional
    public UserDto createUser(String email, String name, String rawPassword) {
        log.info("[UserService] Creating user: {}", email);

        userRepository.findActiveByEmail(email)
                .ifPresent(u -> {
                    throw new CustomException(HttpStatus.CONFLICT, "User already exists with email: " + email);
                });

        User user = User.builder()
                .email(email)
                .name(name)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isDeleted(false)
                .build();

        try {
            User saved = userRepository.save(user);
            return toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("[UserService] DataIntegrityViolation on create for email={}", email, e);
            throw new CustomException(HttpStatus.CONFLICT, "Failed to create user: constraint violation");
        } catch (Exception e) {
            log.error("[UserService] Unexpected error on create for email={}", email, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create user");
        }
    }

    // =====================================================
    // ログイン認証（メールアドレス + パスワード）
    // =====================================================
    @Transactional(readOnly = true)
    public UserDto authenticate(String email, String rawPassword) {
        log.info("[UserService] Authenticating user: {}", email);

        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return toDto(user);
    }

    // =====================================================
    // ID指定取得（有効ユーザー）
    // =====================================================
    @Transactional(readOnly = true)
    public UserDto getActiveUserById(Long id) {
        log.info("[UserService] Fetching active user by id={}", id);
        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new CustomException("User not found or deleted: " + id));
        return toDto(user);
    }

    // =====================================================
    // 論理削除
    // =====================================================
    @Transactional
    public void deleteUser(Long id) {
        log.info("[UserService] Soft deleting user id={}", id);
        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new CustomException("User not found or already deleted: " + id));

        user.setDeleted(true);
        userRepository.save(user);
    }

    // =====================================================
    // Helper: User -> UserDto
    // =====================================================
    private UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .build(); // deleted フィールドは返さない
    }
}