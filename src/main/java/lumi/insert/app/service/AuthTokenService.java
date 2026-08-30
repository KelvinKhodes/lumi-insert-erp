package lumi.insert.app.service;

import lumi.insert.app.dto.request.AuthTokenCreateRequest;
import lumi.insert.app.dto.response.AuthTokenResponse;

public interface AuthTokenService {
    
    AuthTokenResponse createAuthToken(AuthTokenCreateRequest request);

    AuthTokenResponse refreshAuthToken(String refreshToken);

    void deleteRefreshToken(String accessToken, String refreshToken);
    
}
