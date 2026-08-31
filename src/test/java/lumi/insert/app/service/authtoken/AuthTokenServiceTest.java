package lumi.insert.app.service.authtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;

import lumi.insert.app.core.entity.AuthToken;
import lumi.insert.app.dto.request.AuthTokenCreateRequest;
import lumi.insert.app.dto.response.AuthTokenResponse;
import lumi.insert.app.exception.AuthenticationTokenException; 

public class AuthTokenServiceTest extends BaseAuthTokenServiceTest{
    
    @Test
    @DisplayName("Should return authtoken DTO when saved successfully")
    void createAuthToken_validRequest_returnAuthTokenResponseDTO(){
        when(employeeRepository.findByUsername(setupEmployee.getUsername())).thenReturn(Optional.of(setupEmployee));
        when(passwordEncoder.matches("rawPassword", setupEmployee.getPassword())).thenReturn(true);
        when(jwtUtils.getAccessToken(setupEmployee)).thenReturn("someAccessToken");
        when(authTokenRepository.save(any(AuthToken.class))).then(arg -> arg.getArgument(0));
        
        when(authMapper.createDtoResponseFromEntity(eq("someAccessToken"), any(AuthToken.class))).thenReturn(authTokenResponse);

        AuthTokenCreateRequest request = AuthTokenCreateRequest.builder()
        .username(setupEmployee.getUsername())
        .password("rawPassword")
        .build();

        AuthTokenResponse authToken = authTokenServiceMock.createAuthToken(request);
        assertEquals("someAccessToken", authToken.accessToken());
        assertNotNull(authToken.refreshToken());
        assertEquals(setupEmployee.getUsername(), authToken.employee().username());
        assertTrue(authToken.expiredAt().getDayOfMonth() == LocalDate.now().plus(7, ChronoUnit.DAYS).getDayOfMonth());

        verify(authTokenRepository, times(1)).deleteByEmployeeId(setupEmployee.getId());
    }

    @Test
    @DisplayName("Should thrown notfoundexc when username isn't valid")
    void createAuthToken_wrongUsername_throwNotFoundExc(){
        when(employeeRepository.findByUsername(setupEmployee.getUsername())).thenReturn(Optional.empty());

        AuthTokenCreateRequest request = AuthTokenCreateRequest.builder()
        .username(setupEmployee.getUsername())
        .password("rawPassword")
        .build();

        assertThrows(AuthenticationTokenException.class, () -> authTokenServiceMock.createAuthToken(request)); 

        verify(employeeRepository, times(1)).findByUsername(setupEmployee.getUsername());
    }

    @Test
    @DisplayName("Should thrown AccountExpiredException when account inactive")
    void createAuthToken_accountInactive_throwAccountExpiredException(){
        setupEmployee.setActive(false);
        when(employeeRepository.findByUsername(setupEmployee.getUsername())).thenReturn(Optional.of(setupEmployee));

        AuthTokenCreateRequest request = AuthTokenCreateRequest.builder()
        .username(setupEmployee.getUsername())
        .password("rawPassword")
        .build();

        assertThrows(AccountExpiredException.class, () -> authTokenServiceMock.createAuthToken(request)); 
 
    }

    @Test
    @DisplayName("Should thrown BadCredentialsException when password isn't valid")
    void createAuthToken_wrongPassword_throwBadCrendetialsExc(){
        when(employeeRepository.findByUsername(setupEmployee.getUsername())).thenReturn(Optional.of(setupEmployee));
        when(passwordEncoder.matches("rawPassword", setupEmployee.getPassword())).thenReturn(false);

        AuthTokenCreateRequest request = AuthTokenCreateRequest.builder()
        .username(setupEmployee.getUsername())
        .password("rawPassword")
        .build();

        assertThrows(BadCredentialsException.class, () -> authTokenServiceMock.createAuthToken(request)); 

        verify(passwordEncoder, times(1)).matches("rawPassword", setupEmployee.getPassword());
    }

    @Test
    @DisplayName("Should return authtoken DTO when auth token valid")
    void refreshAuthToken_validRequest_returnAuthTokenResponseDTO(){
        when(authTokenRepository.findByRefreshToken(setupAuthToken.getRefreshToken())).thenReturn(Optional.of(setupAuthToken)); 
        when(jwtUtils.getAccessToken(setupEmployee)).thenReturn("someAccessToken"); 
        
        when(authMapper.createDtoResponseFromEntity(eq("someAccessToken"), any(AuthToken.class))).thenReturn(authTokenResponse);
 

        AuthTokenResponse authToken = authTokenServiceMock.refreshAuthToken(setupAuthToken.getRefreshToken());
        assertEquals("someAccessToken", authToken.accessToken());
        assertNotNull(authToken.refreshToken());
        assertEquals(setupEmployee.getUsername(), authToken.employee().username());
        assertTrue(authToken.expiredAt().getDayOfMonth() == LocalDate.now().plus(7, ChronoUnit.DAYS).getDayOfMonth());

        verify(authTokenRepository, times(0)).delete(setupAuthToken);;
    }

    @Test
    @DisplayName("Should throw AuthenticationTokenException when auth token expired")
    void refreshAuthToken_expiredToken_throwAuthenticationTokenException(){
        setupAuthToken.setExpiredAt(LocalDateTime.now().minusDays(2));
        when(authTokenRepository.findByRefreshToken(setupAuthToken.getRefreshToken())).thenReturn(Optional.of(setupAuthToken));   

        assertThrows(AuthenticationTokenException.class, () -> authTokenServiceMock.refreshAuthToken(setupAuthToken.getRefreshToken())); 

        verify(authTokenRepository, times(1)).delete(setupAuthToken);
    }

    @Test
    @DisplayName("Should throw NotFound when auth token is not valid")
    void refreshAuthToken_invalidToken_throwNotGoundExc(){ 
        when(authTokenRepository.findByRefreshToken(setupAuthToken.getRefreshToken())).thenReturn(Optional.empty());   

        assertThrows(AuthenticationTokenException.class, () -> authTokenServiceMock.refreshAuthToken(setupAuthToken.getRefreshToken())); 
 
    }

    @Test
    @DisplayName("Should called deleteByRefreshToken ")
    void deleteAuthToken_verifyCall1Time(){ 
        authTokenServiceMock.deleteRefreshToken("someAccess", setupAuthToken.getRefreshToken());

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set("blacklist::someAccess", true);
        verify(redisTemplate, times(1)).expire("blacklist::someAccess", 15, TimeUnit.MINUTES);
        verify(authTokenRepository, times(1)).deleteByRefreshToken(argThat(arg -> arg.equals(setupAuthToken.getRefreshToken())));
    }



}
