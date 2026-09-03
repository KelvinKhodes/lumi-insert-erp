package lumi.insert.app.controller.memo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when; 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.dto.request.MemoUpdateRequest;
import lumi.insert.app.exception.NotFoundEntityException;

public class MemoControllerUpdateTest extends BaseMemoControllerTest{
    
    @Test
    void updateMemoAPI_validRequest_returnOKAndUpdatedDTO() throws Exception{
        
        when(memoService.updateMemo(eq(1L), any(MemoUpdateRequest.class))).thenReturn(memoResponse);
        mockMvc.perform(
            patch("/api/memos/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("body", "new body")
            .param("role", "FINANCE")
        )
        .andDo(print())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.title").value(memoResponse.title()));

        verify(memoService, times(1)).updateMemo(eq(1L), argThat(arg -> arg.getBody().equals("new body") && arg.getRole().equals("FINANCE")));
    }

    @Test
    void updateMemoAPI_invalidRole_returnBadRequest() throws Exception{ 
        mockMvc.perform(
            patch("/api/memos/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("role", "SUPERMAN")

        )
        .andDo(print())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("check documentation for role specification"));
    }

    @Test
    void updateMemoAPI_invalidId_returnNotFound() throws Exception{ 
        when(memoService.updateMemo(eq(1L), any(MemoUpdateRequest.class))).thenThrow(new NotFoundEntityException("Memo with ID " + 1L + " was not found"));
        mockMvc.perform(
            patch("/api/memos/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("role", "FINANCE")
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Memo with ID " + 1L + " was not found"));
    }

    @Test
    void archiveMemoAPI_validRequest_returnOKAndUpdatedDTO() throws Exception{
        when(memoService.archiveMemo(1L)).thenReturn(memoResponse);
        mockMvc.perform(
            post("/api/memos/1/archive")
            .accept(MediaType.APPLICATION_JSON_VALUE)  
        )
        .andDo(print())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.title").value(memoResponse.title()));
    }

    @Test
    @WithMockUser(roles = {"FINANCE"})
    void archiveMemoAPI_notOwner_returnForbidden() throws Exception{
        UsernamePasswordAuthenticationToken authFinance = new UsernamePasswordAuthenticationToken(employeeLogin, null, finance);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authFinance);
        TestSecurityContextHolder.setContext(context);

        mockMvc.perform(
            post("/api/memos/1/archive")
            .accept(MediaType.APPLICATION_JSON_VALUE)  
        )
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errors").isNotEmpty())
        .andExpect(jsonPath("$.data").isEmpty());

        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void archiveMemoAPI_notFound_returnNotFound() throws Exception{ 
        when(memoService.archiveMemo(eq(1L))).thenThrow(new NotFoundEntityException("Memo with ID " + 1L + " was not found"));

        mockMvc.perform(
            post("/api/memos/1/archive")
            .accept(MediaType.APPLICATION_JSON_VALUE)   
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors").value("Memo with ID " + 1L + " was not found"))
        .andExpect(jsonPath("$.data").isEmpty());
    }
}
