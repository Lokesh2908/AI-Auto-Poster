package com.aiautoposter.security;

import com.aiautoposter.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    
    @Autowired
    private UserService userService;
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain chain) throws ServletException, IOException {
        
        // Skip JWT processing for static resources
        String requestURI = request.getRequestURI();
        if (isStaticResource(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        System.out.println("JWT Filter - Request URI: " + request.getRequestURI());
        System.out.println("JWT Filter - Authorization header: " + (requestTokenHeader != null ? "Present" : "Missing"));
        
        String username = null;
        String jwtToken = null;
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            System.out.println("JWT Filter - Token extracted: " + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
            try {
                username = getUsernameFromToken(jwtToken);
                System.out.println("JWT Filter - Username from token: " + username);
            } catch (Exception e) {
                System.err.println("JWT Filter - Error extracting username: " + e.getMessage());
                logger.error("Unable to get JWT Token or JWT Token has expired", e);
            }
        } else {
            System.out.println("JWT Filter - Token missing or invalid format");
            logger.warn("JWT Token does not begin with Bearer String");
        }
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("JWT Filter - Loading user details for: " + username);
            UserDetails userDetails = userService.loadUserByUsername(username);
            
            if (validateToken(jwtToken, userDetails)) {
                System.out.println("JWT Filter - Token validated successfully for: " + username);
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                System.err.println("JWT Filter - Token validation failed for: " + username);
            }
        } else if (username != null) {
            System.out.println("JWT Filter - User already authenticated: " + username);
        }
        chain.doFilter(request, response);
    }
    
    private String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    private <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }
    
    private Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    
    private Boolean isTokenExpired(String token) {
        final java.util.Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new java.util.Date());
    }
    
    private java.util.Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    private boolean isStaticResource(String requestURI) {
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/static/") ||
               requestURI.startsWith("/webjars/") ||
               requestURI.equals("/favicon.ico") ||
               requestURI.startsWith("/uploads/");
    }
}
