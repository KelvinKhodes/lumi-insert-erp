package lumi.insert.app.controller;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.auth0.jwt.exceptions.JWTVerificationException;

import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.exception.AuthenticationTokenException;
import lumi.insert.app.exception.BoilerplateRequestException;
import lumi.insert.app.exception.DatabaseInternalException;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.StorageActionException;
import lumi.insert.app.exception.TransactionValidationException;

@RestControllerAdvice
public class ErrorController {
    
    @ExceptionHandler(NotFoundEntityException.class)
    public ResponseEntity<WebResponse<String>> notFoundException(NotFoundEntityException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(BoilerplateRequestException.class)
    public ResponseEntity<WebResponse<String>> boilerplateRequestException(BoilerplateRequestException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.NOT_IMPLEMENTED)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<WebResponse<String>> duplicateEntityException(DuplicateEntityException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<String>> methodArgumentNotValidException(MethodArgumentNotValidException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getBindingResult().getAllErrors().get(0).getDefaultMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<WebResponse<String>> handlerMethodValidationException(HandlerMethodValidationException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getAllErrors().getFirst().getDefaultMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<WebResponse<String>> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getParameter().getParameterName() + " must be " + exception.getRequiredType().getSimpleName());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(ForbiddenRequestException.class)
    public ResponseEntity<WebResponse<String>> forbiddenRequestException(ForbiddenRequestException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(TransactionValidationException.class)
    public ResponseEntity<WebResponse<String>> transactionValidationException(TransactionValidationException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<WebResponse<String>> accountExpiredException(AccountExpiredException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<WebResponse<String>> badCredentialsException(BadCredentialsException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<WebResponse<String>> missingRequestCookieException(MissingRequestCookieException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage().split("'")[1] + " at cookie is missing, try to login first");

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(AuthenticationTokenException.class)
    public ResponseEntity<WebResponse<String>> authenticationTokenException(AuthenticationTokenException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", null)
        .maxAge(0)
        .path("/")
        .build();

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<WebResponse<String>> jwtVerificationException(JWTVerificationException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, "Access token invalid, try to login again");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", null)
        .maxAge(0)
        .path("/")
        .build();

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<WebResponse<String>> accessDeniedException(AccessDeniedException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<WebResponse<String>> authorizationDeniedException(AuthorizationDeniedException exception){
        WebResponse<String> webResponse = WebResponse.<String>getWrapper(null,"Access denied, require an authority" );

        ResponseEntity<WebResponse<String>> response = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(webResponse);

        return response;
    }

    @ExceptionHandler(StorageActionException.class)
    public ResponseEntity<WebResponse<String>> storageActionException(StorageActionException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.INSUFFICIENT_STORAGE)
        .body(webResponse);

        return response;
    }

    @ExceptionHandler(DatabaseInternalException.class)
    public ResponseEntity<WebResponse<String>> databaseInternalException(DatabaseInternalException exception){
        WebResponse<String> webResponse = WebResponse.getWrapper(null, exception.getLocalizedMessage());

        ResponseEntity<WebResponse<String>> response = ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(webResponse);

        return response;
    }
}
