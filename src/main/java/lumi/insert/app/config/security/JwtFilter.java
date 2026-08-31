package lumi.insert.app.config.security;

import java.io.IOException; 
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; 
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils; 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.utils.security.JwtUtils;

/**
 * Implementation of request filter.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter{

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    @Qualifier("handlerExceptionResolver") 
    private HandlerExceptionResolver resolver;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    List<String> link = List.of(
        "/auth/login",
        "/auth/refresh",
        "swagger",
        "api-docs", 
        "actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {   

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String requestId = UuidCreator.getTimeOrderedEpochFast().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);  

        try {
            String token = parseBearer(request); 
        
            // Bearer check, /auth/login endpoint allow null token.
            // Otherwise, null token result to BadCredentialsException | 403       
            if(token == null){ 
                String servletPath = request.getServletPath();
                log.debug("Check allowed path, path: {}", servletPath);
                if(link.stream().anyMatch(lk -> servletPath.contains(lk))) { 
                    log.debug("Path is allowed without auth, continue...");
                    filterChain.doFilter(request, response);
                    return;
                }
                log.debug("Endpoint path required auth, return Unauthorized");
                resolver.resolveException(request, response, null, new BadCredentialsException("Missing access token, try to request token!"));
            } 

            DecodedJWT accessToken; 

            try {
                if (redisTemplate.opsForValue().get("blacklist::" + token) != null) {
                    resolver.resolveException(request, response, null, new BadCredentialsException("Access token is invalid, try to login again" + redisTemplate.opsForValue().get("blacklist::" + token)));
                    return;
                }
                accessToken = jwtUtils.parseAccessToken(token);    
            } catch (JWTVerificationException e) { 
                if(e instanceof TokenExpiredException){
                    resolver.resolveException(request, response, null, new BadCredentialsException("Access token is expired, try to request token"));
                    return;
                }
                resolver.resolveException(request, response, null, e);
                return;
            }
            
            //Registering Auth Credentials to SecurityContext
            List<GrantedAuthority> roles = AuthorityUtils.createAuthorityList("ROLE_" + accessToken.getClaim("role").asString());
    
            EmployeeLogin employeeLogin = EmployeeLogin.builder()
            .id(UUID.fromString(accessToken.getClaim("id").asString()))
            .username(accessToken.getClaim("username").asString())
            .role(EmployeeRole.valueOf(accessToken.getClaim("role").asString()))
            .ipAddress(request.getRemoteAddr())
            .build();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, roles);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } finally {
            stopWatch.stop(); 
            // log.info("Execution time: {} ms", stopWatch.getTotalTimeMillis());
            MDC.clear();
        } 
    }

    /**
     * Parse raw request to Formatted JWT
     * <p>Example: Header: Bearer someJWT. Returned value: someJWT</p>
     * @param request
     * @return parsed Token
     */
    private String parseBearer(HttpServletRequest request){
         String header = request.getHeader("Authorization");

         if(header == null) return null;
         String token = header.split("Bearer ")[1];;
         if(token.length() < 1) return null;
         return token;
    }

    
}
