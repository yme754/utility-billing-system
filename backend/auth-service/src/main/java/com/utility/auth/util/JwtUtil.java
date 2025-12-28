package com.utility.auth.util;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.utility.auth.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	@Value("${jwt.secret}")
	private String secret;
	@Value("${jwt.expiration}")
	private Long expirationTime;
	
	public String generateToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("roles", user.getRoles());
		return createToken(claims, user.getUsername());
	}
	
	public String createToken(Map<String, Object>claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + expirationTime))
				.signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
	}
	public Boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public String extractUsername(String token) {
		return extractAllClaims(token).getSubject();
	}
	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
	}
	
	private Key getSignKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
