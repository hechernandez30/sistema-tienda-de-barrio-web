package com.tiendadebarrio.auth.service;

import com.tiendadebarrio.auth.dto.LoginRequest;
import com.tiendadebarrio.auth.dto.LoginResponse;
import com.tiendadebarrio.auth.dto.UserProfileResponse;
import com.tiendadebarrio.auth.mapper.UserMapper;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.security.JwtService;
import com.tiendadebarrio.security.UserPrincipal;
import com.tiendadebarrio.users.entity.AppUser;
import com.tiendadebarrio.users.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final UserMapper userMapper;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        AppUser user = appUserRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ApiException(
                        "Usuario no encontrado",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));

        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);

        String token = jwtService.generateToken(principal);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(userMapper.toUserSummary(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AppUser user = appUserRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ApiException(
                        "Usuario no encontrado",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));
        return userMapper.toUserProfile(user);
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(
                    "Usuario no autenticado",
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED"
            );
        }

        return principal;
    }
}
