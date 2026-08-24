package lumi.insert.app.core.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import lumi.insert.app.core.entity.Memo;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.dto.response.MemoResponse;

/**
 * Repository for {@link Memo} entity.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Repository
public interface MemoRepository extends JpaRepository<Memo, Long>{
     
    /**
     * Fetch active memos base on role and updated time.
     * @param id
     * @param role
     * @param time
     * @return Slices of {@link MemoResponse}
     */
    @Query("SELECT m.id as id, m.title as title, m.body as body, m.images as images, m.role as role, " + 
       "(CASE WHEN mv.id IS NULL THEN false ELSE true END) as isRead " + 
       "FROM memos m " + 
       "LEFT JOIN memo_views mv ON mv.memo = m AND mv.employee.id = :employeeId " + 
       "WHERE m.isActive = true " +
       "AND (m.role = :role OR m.role IS NULL) " +
       "AND m.updatedAt > :time ORDER BY m.updatedAt DESC")
    Slice<MemoResponse> findActiveMemosByRoleOrPublic(
                                            @Param("employeeId") UUID id,
                                            @Param("role") EmployeeRole role, 
                                            @Param("time") LocalDateTime time
    );

    /**
     * Fetch active memos.
     * @param id
     * @param role
     * @param time
     * @return Slices of {@link MemoResponse}
     */
    @Query("SELECT m.id as id, m.title as title, m.body as body, m.images as images, m.role as role, " +
            "(CASE WHEN mv.id IS NULL THEN false ELSE true END) as isRead " +
            "FROM memos m " +
            "LEFT JOIN memo_views mv ON mv.memo = m AND mv.employee.id = :employeeId " +
            "WHERE m.isActive = true " +
            "AND m.updatedAt > :time ORDER BY m.updatedAt DESC")
    Slice<MemoResponse> findActiveMemos(
            @Param("employeeId") UUID id,
            @Param("role") EmployeeRole role,
            @Param("time") LocalDateTime time
    );

    @Modifying
    @Query("UPDATE memos m SET m.isActive = false WHERE m.id = :id")
    int updateIsActiveFalse(@Param("id") Long id);

}
