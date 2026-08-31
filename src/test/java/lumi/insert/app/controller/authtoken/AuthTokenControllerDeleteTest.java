package lumi.insert.app.controller.authtoken;
 
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie; 
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.http.Cookie; 

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@ActiveProfiles("test") 
public class AuthTokenControllerDeleteTest extends BaseAuthTokenControllerTest{
    
    @Test
    @DisplayName("should return Auth DTO when credentials valid")
    public void deleteAuthAPI_validCookieToken_shouldReturn204() throws Exception{ 
        Cookie cookie = new Cookie("refreshToken", "someRefreshToken");
        mockMvc.perform(
            delete("/api/auth/logout")
            .with(csrf())
            .cookie(cookie)  
        )
        .andDo(print())
        .andExpect(status().isNoContent())
        .andExpect(cookie().exists("refreshToken"))
        .andExpect(cookie().maxAge("refreshToken", 0));
        verify(authTokenService, times(1)).deleteRefreshToken(any(),"someRefreshToken");
    }

    @Test
    @DisplayName("should return bad request when cookie is missing")
    public void deleteAuthAPI_emptyCookieToken_shouldReturnBadRequest() throws Exception{  
        mockMvc.perform(
            delete("/api/auth/logout")
            .with(csrf())   
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").value("refreshToken at cookie is missing, try to login first")); 
    }

}
