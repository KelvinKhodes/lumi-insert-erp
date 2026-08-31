package lumi.insert.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Response containing authentication tokens and user profile information upon successful login")
public record DeleteTokenResponse(
    
    @Schema(description = "Token used to obtain a new access token without re-authenticating", example = "d8e9f0a1-b2c3-4d5e-6f7g-8h9i0j1k2l3m")
    String refreshToken

) implements Identifiable {

    @Override
    public String getId() {
        return refreshToken;
    }

}