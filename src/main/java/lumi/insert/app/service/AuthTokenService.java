package lumi.insert.app.service;

import lumi.insert.app.dto.request.AuthTokenCreateRequest;
import lumi.insert.app.dto.response.AuthTokenResponse;
import lumi.insert.app.dto.response.DeleteTokenResponse;

public interface AuthTokenService {
    
    AuthTokenResponse createAuthToken(AuthTokenCreateRequest request);

    AuthTokenResponse refreshAuthToken(String refreshToken);

    DeleteTokenResponse deleteRefreshToken(String accessToken, String refreshToken);
    
}
