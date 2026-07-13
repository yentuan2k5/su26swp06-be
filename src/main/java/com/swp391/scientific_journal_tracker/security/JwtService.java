package com.swp391.scientific_journal_tracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Tên claim dùng để phân biệt access token và refresh token
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    // Giá trị loại token
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration-ms:86400000}")
    private long refreshExpiration;

    /**
     * Tạo access token.
     * Access token được sử dụng để truy cập các API cần đăng nhập.
     */
    public String generateToken(String username) {
        return buildToken(
                username,
                ACCESS_TOKEN_TYPE,
                jwtExpiration);
    }

    /**
     * Tạo refresh token.
     * Refresh token chỉ được sử dụng tại API /refresh-token.
     */
    public String generateRefreshToken(String username) {
        return buildToken(
                username,
                REFRESH_TOKEN_TYPE,
                refreshExpiration);
    }

    /**
     * Hàm dùng chung để tạo JWT.
     */
    private String buildToken(
            String username,
            String tokenType,
            long expirationMs) {
        long currentTime = System.currentTimeMillis();

        return Jwts.builder()
                .subject(username)

                // Gắn loại token vào JWT
                .claim(TOKEN_TYPE_CLAIM, tokenType)

                .issuedAt(new Date(currentTime))
                .expiration(new Date(currentTime + expirationMs))
                .signWith(getSignKey())
                .compact();
    }

    /**
     * Lấy username nằm trong subject của JWT.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Kiểm tra token có phải access token hợp lệ không.
     */
    public boolean isAccessTokenValid(String token) {
        return isTokenValidAndHasType(
                token,
                ACCESS_TOKEN_TYPE);
    }

    /**
     * Kiểm tra token có phải refresh token hợp lệ không.
     */
    public boolean isRefreshTokenValid(String token) {
        return isTokenValidAndHasType(
                token,
                REFRESH_TOKEN_TYPE);
    }

    /**
     * Kiểm tra:
     * 1. Chữ ký JWT.
     * 2. Thời gian hết hạn.
     * 3. Username.
     * 4. Loại token.
     */
    private boolean isTokenValidAndHasType(
            String token,
            String expectedTokenType) {
        try {
            Claims claims = extractAllClaims(token);

            String actualTokenType = claims.get(
                    TOKEN_TYPE_CLAIM,
                    String.class);

            return expectedTokenType.equals(actualTokenType)
                    && claims.getSubject() != null
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (Exception exception) {
            // Token sai chữ ký, hết hạn, sai cấu trúc...
            return false;
        }
    }

    /**
     * Đọc toàn bộ claims trong JWT.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Chuyển jwt.secret từ Base64 thành khóa ký JWT.
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secretKey));
    }
}