package com.example.authservice.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String,Object>redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeadar = request.getHeader("Authorization");
        final String jwt;
        final String username;
        if(authHeadar==null || !authHeadar.startsWith("Bearer")){
            filterChain.doFilter(request,response);
            return;
        }
        jwt = authHeadar.substring(7);
        try {
            // check if token is revoked
            Boolean isBlacklisted = redisTemplate.hasKey("blacklist:"+jwt);
            if(Boolean.TRUE.equals(isBlacklisted)){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked");
                return;
            }
            username = jwtService.extractUsername(jwt);
            if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null){
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                String rateLimitKey = "rate:limit:"+ username+ ":"+request.getRequestURI();
                Long requestCount = redisTemplate.opsForValue().increment(rateLimitKey);
                if(requestCount==1){
                    redisTemplate.expire(rateLimitKey,60, TimeUnit.SECONDS);
                }
                if(requestCount!=null && requestCount>100){
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Rate limit exceeded\", \"limit\": 100}");
                    return;
                }
                if(jwtService.isTokenValid(jwt,userDetails)&& !jwtService.isRefreshToken(jwt)){
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    //cache user session
                    String sessionKey = "session:"+ jwtService.extractUserId(jwt);
                    redisTemplate.opsForValue().set(sessionKey,userDetails,1,TimeUnit.HOURS);
                }
            }
        }catch (Exception e){
            log.error("JWT authentication failed: {}",e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authentication failed:"+ e.getMessage());
            return;
        }
        filterChain.doFilter(request,response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        String path = request.getRequestURI();
        return path.startsWith("/api/auth")|| path.startsWith("/actuator");
    }
}
