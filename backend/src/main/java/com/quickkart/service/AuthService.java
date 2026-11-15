package com.quickkart.service;

import com.quickkart.dto.AuthDtos;
import com.quickkart.entity.Cart;
import com.quickkart.entity.Role;
import com.quickkart.entity.User;
import com.quickkart.entity.Wishlist;
import com.quickkart.entity.enums.RoleName;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.exception.UnauthorizedException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.CartRepository;
import com.quickkart.repository.RoleRepository;
import com.quickkart.repository.UserRepository;
import com.quickkart.repository.WishlistRepository;
import com.quickkart.security.JwtService;
import com.quickkart.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EntityMapper mapper;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CartRepository cartRepository,
            WishlistRepository wishlistRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EntityMapper mapper,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.emailService = emailService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role not initialized."));

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone() != null ? req.phone().trim() : null);
        user.setActive(true);
        user.setEmailVerified(false);
        user.setRoles(Set.of(customerRole));

        User saved = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(saved);
        cartRepository.save(cart);

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(saved);
        wishlistRepository.save(wishlist);

        UserPrincipal principal = new UserPrincipal(saved);
        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                mapper.toUserProfileDto(saved)
        );
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.password())
        );
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Your account has been deactivated. Contact support.");
        }

        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                mapper.toUserProfileDto(user)
        );
    }

    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshTokenRequest req) {
        String token = req.refreshToken();
        try {
            if (!"refresh".equals(jwtService.extractTokenType(token))) {
                throw new UnauthorizedException("Invalid refresh token.");
            }
            String email = jwtService.extractSubject(token);
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found."));
            if (!Boolean.TRUE.equals(user.getActive()) || !jwtService.isTokenValid(token, email)) {
                throw new UnauthorizedException("Refresh token expired or invalid.");
            }
            UserPrincipal principal = new UserPrincipal(user);
            return new AuthDtos.AuthResponse(
                    jwtService.generateAccessToken(principal),
                    jwtService.generateRefreshToken(principal),
                    mapper.toUserProfileDto(user)
            );
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid refresh token.");
        }
    }

    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest req) {
        userRepository.findByEmailIgnoreCase(req.email().trim()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendEmail(
                    user.getEmail(),
                    "Quick kart Password Reset",
                    "Use this reset token within 30 minutes to reset your Quick kart password: " + token
            );
        });
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        User user = userRepository.findByResetToken(req.token().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    public AuthDtos.UserProfileDto getMyProfile() {
        return mapper.toUserProfileDto(getAuthenticatedUser());
    }

    @Transactional
    public AuthDtos.UserProfileDto updateMyProfile(AuthDtos.UpdateProfileRequest req) {
        User user = getAuthenticatedUser();
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone() != null ? req.phone().trim() : null);
        if (req.avatarUrl() != null) {
            user.setAvatarUrl(req.avatarUrl().trim());
        }
        return mapper.toUserProfileDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest req) {
        User user = getAuthenticatedUser();
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }
}
