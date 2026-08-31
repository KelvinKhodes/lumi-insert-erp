package lumi.insert.app.service.implement;

import java.time.LocalDateTime;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.entity.Memo;
import lumi.insert.app.core.entity.MemoView;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.EmployeeRepository;
import lumi.insert.app.core.repository.MemoRepository;
import lumi.insert.app.core.repository.MemoViewRepository;
import lumi.insert.app.dto.request.MemoCreateRequest;
import lumi.insert.app.dto.request.MemoUpdateRequest;
import lumi.insert.app.dto.response.MemoResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.mapper.MemoMapper;
import lumi.insert.app.service.MemoService;

/**
 * Implementation of {@link MemoService} for corporate internal communication management.
 * <p>
 * This service manages the lifecycle of internal memos, including role-based visibility 
 * filtering and tracking of memo views by employees.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Slf4j
@Transactional
public class MemoServiceImpl implements MemoService{

    @Autowired
    MemoRepository memoRepository;

    @Autowired
    MemoViewRepository memoViewRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    MemoMapper mapper;

    /**
     * Creates a new internal memo.
     * <p>Memos can be targeted to specific {@link EmployeeRole} or left public.</p>
     *
     * @param request the memo creation details including title, body, and optional role target.
     * @return the mapped {@link MemoResponse} of the created entity.
     */
    @Override
    @ActivityLogger(
        entityName = "memos",
        action = ActivityAction.MEMO_CREATED,
        actionMessage = "New memo created"
    )
    public MemoResponse createMemo(MemoCreateRequest request) {
        log.info("Creating memo title={}", request.getTitle());
        Memo memo = Memo.builder()
        .title(request.getTitle())
        .body(request.getBody())
        .build();

        if(request.getRole() != null) memo.setRole(EmployeeRole.valueOf(request.getRole()));
        Memo savedMemo = memoRepository.save(memo);
        log.info("Memo created id={}", savedMemo.getId());

        if(!(request.getImages() == null)){
            // Call method to process the image
        }

        return mapper.createDtoResponseFromMemo(savedMemo);
    }

    /**
     * Updates an existing memo and resets its "read" status for all employees.
     * <p>Critical: This operation deletes all existing {@link MemoView} records for this memo 
     * to ensure employees are notified of the changes.</p>
     *
     * @param id      the unique identifier of the memo.
     * @param request the update data.
     * @return the updated {@link MemoResponse}.
     * @throws NotFoundEntityException if the memo ID is invalid.
     */
    @Override
    @ActivityLogger(
        entityName = "memos",
        action = ActivityAction.MEMO_UPDATED,
        actionMessage = "Memo updated"
    )
    public MemoResponse updateMemo(Long id, MemoUpdateRequest request) {
        log.info("Updating memo id={}", id);
        Memo memo = memoRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Memo update failed, not found id={}", id);
                return new NotFoundEntityException("Memo with ID " + id + " was not found");
            });
        
        mapper.updateEntityFromDto(request, memo);

        memoViewRepository.deleteMemoView(id);
        log.info("Memo updated id={}", id);

        return mapper.createDtoResponseFromMemo(memo);
    }

    /**
     * Deactivates a memo to hide it from general listings.
     *
     * @param id the unique identifier of the memo to archive.
     * @return the archived {@link MemoResponse}.
     * @throws NotFoundEntityException if the memo does not exist.
     */
    @Override
    @ActivityLogger(
        entityName = "memos",
        action = ActivityAction.MEMO_UPDATED,
        actionMessage = "Memo is archieved"
    )
    public MemoResponse archiveMemo(Long id) {
        log.info("Archiving memo id={}", id);
        Memo memo = memoRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Memo archive failed, not found id={}", id);
                return new NotFoundEntityException("Memo with ID " + id + " is not found");
            });

        memo.setIsActive(false);
        log.info("Memo archived id={}", id);
        return mapper.createDtoResponseFromMemo(memo);
    }

    /**
     * Retrieves a single memo by its ID.
     *
     * @param id the unique identifier.
     * @return the found {@link MemoResponse}.
     * @throws NotFoundEntityException if no memo matches the given ID.
     */
    @Override
    public MemoResponse getMemo(Long id) {
        log.info("Retrieving memo id={}", id);
        Memo memo = memoRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Memo not found id={}", id);
                return new NotFoundEntityException("Memo with ID " + id + " is not found");
            });
 
        return mapper.createDtoResponseFromMemo(memo);
    }

    /**
     * Retrieves a slice of active memos relevant to the logged-in employee.
     * <p>Filters memos based on the employee's role and the memo's publication time.</p>
     *
     * @param login the current employee's session details.
     * @param time  the reference timestamp for visibility filtering.
     * @return a {@link Slice} of accessible {@link MemoResponse} objects.
     */
    @Override
    public Slice<MemoResponse> getMemos(EmployeeLogin login, LocalDateTime time) {
        if (login.getRole() == EmployeeRole.OWNER) return memoRepository.findActiveMemos(login.getId(), login.getRole(), time);
        return memoRepository.findActiveMemosByRoleOrPublic(login.getId(), login.getRole(), time);
    }

    /**
     * Records an interaction when an employee reads a memo.
     * <p>Uses {@code getReferenceById} for performance optimization when linking entities.</p>
     *
     * @param login the employee who is reading the memo.
     * @param id    the identifier of the memo being read.
     * @return the created {@link MemoView} or an empty object if the operation fails.
     */
    @Override
    @ActivityLogger(
        entityName = "memo_views",
        action = ActivityAction.MEMO_READ,
        actionMessage = "Memo read"
    )
    public MemoView createMemoView(EmployeeLogin login, Long id) {
        log.info("Creating memo view for memoId={} employeeId={}", id, login.getId());
        try {
            Memo memo = memoRepository.getReferenceById(id);
            Employee employee = employeeRepository.getReferenceById(login.getId());
            MemoView memoView = new MemoView(memo, employee);
            MemoView result = memoViewRepository.save(memoView); 
            log.info("Memo view created memoId={} employeeId={}", id, login.getId());
            return result;
        } catch (Exception e) {
            log.error("Failed to create memo view memoId={} employeeId={} message={}", id, login.getId(), e.getLocalizedMessage());
            log.debug("Memo view exception", e);
            return new MemoView();
        }
    }
    
}
