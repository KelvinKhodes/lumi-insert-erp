package lumi.insert.app.controller.memo;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.controller.BaseControllerTest;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.dto.response.MemoResponse; 
 import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = {"OWNER"} )
public abstract class BaseMemoControllerTest extends BaseControllerTest{
     
    MemoResponse memoResponse = new MemoResponse(1L, "A Title", "A Body", List.of(), EmployeeRole.FINANCE, false);

    List<GrantedAuthority> roles = AuthorityUtils.createAuthorityList("ROLE_OWNER");
    List<GrantedAuthority> finance = AuthorityUtils.createAuthorityList("ROLE_FINANCE");

    EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UUID.randomUUID())
        .username("lumi")
        .role(EmployeeRole.FINANCE)
        .build();

    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, roles);

    @BeforeEach
    void setup(){
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        TestSecurityContextHolder.setContext(context);

    }

    @AfterEach
    void after(){
        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();
    }
}
