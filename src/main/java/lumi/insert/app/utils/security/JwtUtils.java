package lumi.insert.app.utils.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import lumi.insert.app.core.entity.Employee;

/**
 * Utilities of Pdf's related.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    public String jwtSecret;

    Algorithm algorithm;
    
    String issuer = "LUMI-INSERT";

    JWTVerifier jwtVerifier;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(jwtSecret);
        this.jwtVerifier = JWT.require(algorithm)
            .withIssuer(issuer).build();
    }
    /**
     * Create Token based on employee credentials with expired: 15 minutes.
     * @param employee
     * @return token
     */
    public String getAccessToken(Employee employee){
        String accessToken = JWT.create()
        .withIssuer(issuer)
        .withIssuedAt(Instant.now())
        .withClaim("id", employee.getId().toString())
        .withClaim("username", employee.getUsername())
        .withClaim("role", employee.getRole().toString()) 
        .withExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
        .sign(algorithm);

        return accessToken;
    }

    /**
     * Parse, decode and verify created token by {@link #getAccessToken(Employee)}. 
     * @param accessToken
     * @return decoded Token
     */
    public DecodedJWT parseAccessToken(String accessToken){
        return jwtVerifier.verify(accessToken);
    }

}
