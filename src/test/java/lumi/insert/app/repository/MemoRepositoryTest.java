package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.entity.Memo;
import lumi.insert.app.core.entity.MemoView;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.EmployeeRepository;
import lumi.insert.app.core.repository.MemoRepository;
import lumi.insert.app.core.repository.MemoViewRepository;
import lumi.insert.app.dto.response.MemoResponse;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
@Import({AuditorAwareImpl.class})
public class MemoRepositoryTest  extends TestContainerTest {
    
    @Autowired
    MemoRepository memoRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    MemoViewRepository memoViewRepository;

    @BeforeEach
    void setup(){
        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    
    @Test
    void getMemo_roleALL_returnALL(){
        Memo cashierMemo = Memo.builder() 
        .title("Cashier")
        .body("Cashier body")
        .role(EmployeeRole.CASHIER)
        .build();

        Memo financeMemo = Memo.builder()
        .title("FINANCE")
        .body("FINANCE body")
        .role(EmployeeRole.FINANCE)
        .build();

        Memo allMemo = Memo.builder()
        .title("ALL")
        .body("ALL body") 
        .build();

        memoRepository.saveAllAndFlush(List.of(cashierMemo, financeMemo, allMemo));

        Slice<MemoResponse> activeMemosByRoleOrPublic = memoRepository.findActiveMemosByRoleOrPublic(null, EmployeeRole.FINANCE, LocalDateTime.now().minusHours(1));

        assertEquals(2, activeMemosByRoleOrPublic.getNumberOfElements());
        assertEquals(financeMemo.getBody(), activeMemosByRoleOrPublic.getContent().getLast().body());
        assertEquals(allMemo.getBody(), activeMemosByRoleOrPublic.getContent().getFirst().body());
    }

    @Test
    void getMemo_isActive_returnALL(){
        Memo cashierMemo = Memo.builder() 
        .title("Cashier")
        .isActive(false)
        .body("Cashier body")
        .role(EmployeeRole.CASHIER)
        .build();


        Memo allMemo = Memo.builder()
        .title("ALL")
        .body("ALL body") 
        .build();

        memoRepository.saveAllAndFlush(List.of(cashierMemo, allMemo));

        Slice<MemoResponse> activeMemosByRoleOrPublic = memoRepository.findActiveMemosByRoleOrPublic(null, EmployeeRole.CASHIER, LocalDateTime.now().minusHours(1));

        assertEquals(1, activeMemosByRoleOrPublic.getNumberOfElements()); 
        assertEquals(allMemo.getBody(), activeMemosByRoleOrPublic.getContent().getLast().body());
        assertFalse(activeMemosByRoleOrPublic.getContent().getFirst().isRead());
    }

    @Test
    void updateIsActiveFalse_isActive_returnALL(){
        Memo cashierMemo = Memo.builder() 
        .title("Cashier") 
        .body("Cashier body")
        .role(EmployeeRole.CASHIER)
        .build(); 

        Memo saveAndFlush = memoRepository.saveAndFlush(cashierMemo);;

        int updateIsActiveFalse = memoRepository.updateIsActiveFalse(saveAndFlush.getId());

        assertEquals(1, updateIsActiveFalse);
    }

    @Test
    void findActiveMemosByRoleOrPublic_isReadTest_returnTrue(){
        Memo cashierMemo = Memo.builder() 
        .title("Cashier") 
        .body("Cashier body")
        .role(EmployeeRole.CASHIER)
        .build();
 
        Memo savedMemo = memoRepository.saveAndFlush(cashierMemo);;

        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        Employee savedEmployee = employeeRepository.saveAndFlush(employee);

        memoViewRepository.saveAndFlush(new MemoView(savedMemo, savedEmployee));

        Slice<MemoResponse> activeMemosByRoleOrPublic = memoRepository.findActiveMemosByRoleOrPublic(savedEmployee.getId(), EmployeeRole.CASHIER, LocalDateTime.now().minusHours(1));

        assertEquals(1, activeMemosByRoleOrPublic.getNumberOfElements());  
        assertTrue(activeMemosByRoleOrPublic.getContent().getFirst().isRead());
    }
} 
