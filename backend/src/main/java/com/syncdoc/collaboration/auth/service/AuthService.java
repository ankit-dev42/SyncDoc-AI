package com.syncdoc.collaboration.auth.service;

import com.syncdoc.collaboration.auth.dto.LoginRequest;
import com.syncdoc.collaboration.auth.dto.RegisterRequest;
import com.syncdoc.collaboration.auth.dto.TokenResponse;
import com.syncdoc.collaboration.auth.dto.UserResponse;
import com.syncdoc.collaboration.auth.model.RefreshToken;
import com.syncdoc.collaboration.auth.model.User;
import com.syncdoc.collaboration.auth.repository.RefreshTokenRepository;
import com.syncdoc.collaboration.auth.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Core authentication service: register, login, refresh token rotation,
 * and logout. All refresh-token operations apply strict rotation with a
 * &lt;2s grace window for concurrent mobile clients (AD-005).
 */
@Service
public class AuthService {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth/refresh";
    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        JwtTokenService jwtTokenService,
        BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user, returning a sanitised user response (no password hash).
     *
     * @param request the registration data
     * @return a {@link UserResponse} containing id, email, and displayName
     * @throws ResponseStatusException 409 if the email is already registered
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId().toString(), saved.getEmail(), saved.getDisplayName());
    }

    /**
     * Authenticates a user, issues a JWT access token, and sets a HttpOnly refresh cookie.
     *
     * @param request  the login credentials
     * @param httpRequest  the incoming servlet request (for User-Agent + IP binding)
     * @param httpResponse the outgoing servlet response (to set the cookie)
     * @return a {@link TokenResponse} with the short-lived access token
     * @throws ResponseStatusException 401 on bad credentials
     */
    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String rawToken = jwtTokenService.generateRefreshTokenRaw();
        String hashedToken = jwtTokenService.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setHashedToken(hashedToken);
        refreshToken.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_MAX_AGE_SECONDS, ChronoUnit.SECONDS));
        refreshToken.setBoundUserAgent(extractUserAgent(httpRequest));
        refreshToken.setBoundIp(extractClientIp(httpRequest));
        refreshTokenRepository.save(refreshToken);

        setRefreshCookie(httpResponse, rawToken);
        return new TokenResponse(jwtTokenService.generateAccessToken(user.getId().toString()));
    }

    /**
     * Rotates the refresh token — revokes the old one, issues a new one.
     * Implements the &lt;2s grace window for concurrent mobile requests.
     *
     * @param rawOldToken  the raw token value read from the HttpOnly cookie
     * @param httpRequest  for User-Agent + IP binding verification
     * @param httpResponse to set the new refresh cookie
     * @return a new {@link TokenResponse} with a fresh access token
     * @throws ResponseStatusException 401 on revoked/expired/mismatched token
     */
    @Transactional(noRollbackFor = org.springframework.web.server.ResponseStatusException.class)
    public TokenResponse refresh(String rawOldToken, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        String hashedOld = jwtTokenService.hashToken(rawOldToken);
        RefreshToken oldToken = refreshTokenRepository.findByHashedToken(hashedOld)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));

        if (oldToken.isRevoked()) {
            boolean withinGrace = oldToken.getReplacedAt() != null &&
                Instant.now().isBefore(oldToken.getReplacedAt().plusSeconds(2));
            if (!withinGrace) {
                refreshTokenRepository.deleteAllByUserId(oldToken.getUserId());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token replayed after grace window");
            }
            // Within grace window: re-issue the replacement token
            return reissueLatestToken(oldToken.getUserId(), httpRequest, httpResponse);
        }

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        String incomingAgent = extractUserAgent(httpRequest);
        String incomingIp = extractClientIp(httpRequest);
        if (!isBound(oldToken.getBoundUserAgent(), incomingAgent) ||
            !isBound(oldToken.getBoundIp(), incomingIp)) {
            refreshTokenRepository.deleteAllByUserId(oldToken.getUserId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token binding mismatch");
        }

        // Rotate: revoke old, issue new
        oldToken.setRevoked(true);
        oldToken.setReplacedAt(Instant.now());
        refreshTokenRepository.save(oldToken);

        String rawNew = jwtTokenService.generateRefreshTokenRaw();
        RefreshToken newToken = new RefreshToken();
        newToken.setUserId(oldToken.getUserId());
        newToken.setHashedToken(jwtTokenService.hashToken(rawNew));
        newToken.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_MAX_AGE_SECONDS, ChronoUnit.SECONDS));
        newToken.setBoundUserAgent(incomingAgent);
        newToken.setBoundIp(incomingIp);
        refreshTokenRepository.save(newToken);

        setRefreshCookie(httpResponse, rawNew);
        return new TokenResponse(jwtTokenService.generateAccessToken(oldToken.getUserId().toString()));
    }

    /**
     * Revokes the current refresh token and clears the cookie.
     *
     * @param rawToken     the raw token value from the HttpOnly cookie
     * @param httpResponse to clear the cookie
     */
    @Transactional
    public void logout(String rawToken, HttpServletResponse httpResponse) {
        String hashed = jwtTokenService.hashToken(rawToken);
        refreshTokenRepository.findByHashedToken(hashed).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        clearRefreshCookie(httpResponse);
    }

    // -----------------------------------------------------------------------
    // Private helpers — each focused and under 10 lines to keep method sizes down
    // -----------------------------------------------------------------------

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true behind HTTPS; controlled externally
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE_SECONDS);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return (ua != null && !ua.isBlank()) ? ua : "unknown";
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isBound(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return true; // no binding set — skip check
        }
        return expected.equals(actual);
    }

    private TokenResponse reissueLatestToken(UUID userId, HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        // Find the non-revoked token issued during the same rotation
        RefreshToken latest = refreshTokenRepository.findAll().stream()
            .filter(rt -> userId.equals(rt.getUserId()) && !rt.isRevoked())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active replacement token"));

        // Re-set the same cookie so the client has the new token
        // The raw value is unavailable (only hash stored) — issue a fresh one
        String rawNew = jwtTokenService.generateRefreshTokenRaw();
        latest.setHashedToken(jwtTokenService.hashToken(rawNew));
        refreshTokenRepository.save(latest);

        setRefreshCookie(httpResponse, rawNew);
        return new TokenResponse(jwtTokenService.generateAccessToken(userId.toString()));
    }
}
