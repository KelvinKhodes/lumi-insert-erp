package lumi.insert.app.controller.memo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import lumi.insert.app.core.entity.MemoView;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.dto.request.MemoCreateRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;

public class MemoControllerCreateTest extends BaseMemoControllerTest{
    
    @Test
    void createMemoAPI_validRequest_returnCreatedAndDTO() throws Exception{ 
        when(memoService.createMemo(any(MemoCreateRequest.class))).thenReturn(memoResponse);

        mockMvc.perform(
            post("/api/memos")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .param("title", memoResponse.title())
            .param("body", memoResponse.body())
            .param("role", memoResponse.role().toString())    
        )
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.title").value(memoResponse.title()));

        verify(memoService, times(1)).createMemo(argThat(arg -> arg.getRole().equals(memoResponse.role().toString())));
    }

    @Test
    void createMemoAPI_invalidRole_returnBadRequest() throws Exception{  
        mockMvc.perform(
            post("/api/memos")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .param("title", memoResponse.title())
            .param("body", memoResponse.body())
            .param("role", "SUPERMAN")    
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").value("check documentation for role specification"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createMemoAPI_emptyBody_returnBadRequest() throws Exception{  
        mockMvc.perform(
            post("/api/memos")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .param("title", memoResponse.title())
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").value("body cannot be empty"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createMemoViewAPI_validRequest_returnTrue() throws Exception{ 
        when(memoService.createMemoView(any(EmployeeLogin.class), anyLong())).thenReturn(new MemoView("id", null, null));



        mockMvc.perform(
            post("/api/memos/1/read")
            .accept(MediaType.APPLICATION_JSON_VALUE)
                .with(request -> {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    TestSecurityContextHolder.setContext(context);
                    return request;
                })
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data").value(true));


        verify(memoService, times(1)).createMemoView(argThat(arg -> arg.getUsername().equals("lumi") && arg.getRole() == EmployeeRole.FINANCE), eq(1L));
    }

    @Test
    void createMemoViewAPI_alreadyRead_returnFalse() throws Exception{ 
        when(memoService.createMemoView(any(EmployeeLogin.class), anyLong())).thenReturn(new MemoView("id", null, null));

        mockMvc.perform(
            post("/api/memos/1/read")
            .accept(MediaType.APPLICATION_JSON_VALUE)

        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data").isNotEmpty());
        verify(memoService, times(1)).createMemoView(argThat(arg -> arg.getUsername().equals("lumi") && arg.getRole() == EmployeeRole.FINANCE), eq(1L));
    }

}
