package com.seniorcare.Seniorcare.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm; // <-- Dùng import này
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys; // <-- Đổi lại import này
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

// import javax.crypto.spec.SecretKeySpec; // Bỏ import này

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String SECRET_KEY;

    @Value("${application.security.jwt.expiration}")
    private long EXPIRATION_TIME;

    // Trích xuất username (số điện thoại) từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Trích xuất một "claim" (thông tin) cụ thể từ token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Tạo token chỉ từ UserDetails
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // --- SỬ DỤNG CÚ PHÁP 0.11.5 CŨ (DEPRECATED) ---
    // (Hàm này SẼ CÓ LỖI VÀNG, nhưng CHẠY ĐƯỢC)
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // <-- Dùng cú pháp cũ
                .compact();
    }

    // Kiểm tra xem token có hợp lệ không (đúng user và chưa hết hạn)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Kiểm tra token đã hết hạn chưa
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Lấy ngày hết hạn từ token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // --- SỬA LỖI: ĐÃ THÊM .build() ---
    // (Hàm này SẼ CÓ LỖI VÀNG, nhưng HẾT LỖI ĐỎ)
    private Claims extractAllClaims(String token) {
        return Jwts.parser() // <-- Dùng parser()
                .setSigningKey(getSignInKey())
                .build() // <-- THÊM BƯỚC NÀY ĐỂ TẠO RA JwtParser
                .parseClaimsJws(token) // <-- Hàm này thuộc về JwtParser
                .getBody();
    }

    // --- SỬ DỤNG CÚ PHÁP 0.11.5 ---
    // Lấy khóa bí mật (secret key)
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        // Quay lại dùng Keys.hmacShaKeyFor
        return Keys.hmacShaKeyFor(keyBytes);
    }
}