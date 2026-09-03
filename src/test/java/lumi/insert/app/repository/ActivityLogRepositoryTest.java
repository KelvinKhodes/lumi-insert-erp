package lumi.insert.app.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.activitycore.repository.ActivityLogRepository;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.dto.request.ActivityLogFilterRequest;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({JpaSpecGenerator.class, SimpleCacheManager.class})
@ActiveProfiles("test")
@Log4j2
public class ActivityLogRepositoryTest extends TestContainerTest {

    @Autowired
    ActivityLogRepository activityLogRepository;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    @Test
    @DisplayName("Should return filtered activity log case 1: ID 2L")
    public void findAllCriteria_id2L_returnWithExactId(){
      Set<ActivityLog> pending = new HashSet<>();

      for (int i = 1; i < 3; i++) {
        if(i%2 == 0) {
          ActivityLog activityLog = ActivityLog.builder()
              .id(UuidCreator.getTimeOrderedEpochFast())
              .entityName("products")
              .entityId("1L")
              .ipAddress("0.0.0.0")
              .actionMessage("")
              .action(ActivityAction.PRODUCT_CREATED)
              .build();

          pending.add(activityLog);
        } else {
          ActivityLog activityLog = ActivityLog.builder()
              .id(UuidCreator.getTimeOrderedEpochFast())
              .entityName("products")
              .entityId("2L")
              .ipAddress("0.0.0.0")
              .actionMessage("")
              .action(ActivityAction.PRODUCT_CREATED)
              .build();

          pending.add(activityLog);
        }
      }

      activityLogRepository.saveAllAndFlush(pending);

      ActivityLogFilterRequest request = ActivityLogFilterRequest.builder()
          .entityName("products")
          .entityId("2L")
          .action(ActivityAction.PRODUCT_CREATED)
          .build();

      Pageable pageable = jpaSpecGenerator.pageable(request);

      Specification<ActivityLog> specification = jpaSpecGenerator.activityLogSpecification(request);

      Slice<ActivityLog> activityLogs = activityLogRepository.findAll(specification, pageable);
      assertEquals(1, activityLogs.getNumberOfElements());
      assertEquals("2L", activityLogs.getContent().getFirst().getEntityId());

    }

  @Test
  @DisplayName("Should return filtered activity log case 2: status.PRODUCT_CREATED")
  public void findAllCriteria_combineFilter(){
    Set<ActivityLog> pending = new HashSet<>();

        ActivityLog activityLog = ActivityLog.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .entityName("products")
            .entityId("1L")
            .ipAddress("0.0.0.0")
            .actionMessage("")
            .action(ActivityAction.PRODUCT_CREATED)
            .build();

        pending.add(activityLog);

        ActivityLog activityLog2 = ActivityLog.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .entityName("products")
            .entityId("2L")
            .ipAddress("0.0.0.0")
            .actionMessage("")
            .action(ActivityAction.PRODUCT_CREATED)
            .build();

        pending.add(activityLog2);

      ActivityLog activityLog3 = ActivityLog.builder()
          .id(UuidCreator.getTimeOrderedEpochFast())
          .entityName("products")
          .entityId("2L")
          .ipAddress("0.0.0.0")
          .actionMessage("")
          .action(ActivityAction.PRODUCT_UPDATED)
          .build();

      pending.add(activityLog3);

    activityLogRepository.saveAllAndFlush(pending);

    ActivityLogFilterRequest request = ActivityLogFilterRequest.builder()
        .entityName("products")
        .action(ActivityAction.PRODUCT_CREATED)
        .ipAddress("0.0.0.0")
        .build();

    Pageable pageable = jpaSpecGenerator.pageable(request);

    Specification<ActivityLog> specification = jpaSpecGenerator.activityLogSpecification(request);
    List<ActivityLog> all = activityLogRepository.findAll();
    Slice<ActivityLog> activityLogs = activityLogRepository.findAll(specification, pageable);
    log.info("We get: {}, actual DB: {}", activityLogs.getContent(), all);
    assertEquals(2, activityLogs.getNumberOfElements());

  }

  @Test
  @DisplayName("Should return filtered activity log case 3: status.None")
  public void findAllCriteria_returnNoContent(){
    Set<ActivityLog> pending = new HashSet<>();

    ActivityLog activityLog = ActivityLog.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .entityName("products")
        .entityId("1L")
        .ipAddress("0.0.0.0")
        .actionMessage("")
        .action(ActivityAction.PRODUCT_UPDATED)
        .build();

    pending.add(activityLog);

    ActivityLog activityLog2 = ActivityLog.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .entityName("products")
        .entityId("2L")
        .ipAddress("0.0.0.0")
        .actionMessage("")
        .action(ActivityAction.PRODUCT_UPDATED)
        .build();

    pending.add(activityLog2);


    activityLogRepository.saveAllAndFlush(pending);

    ActivityLogFilterRequest request = ActivityLogFilterRequest.builder()
        .entityName("products")
        .action(ActivityAction.PRODUCT_CREATED)
        .build();

    Pageable pageable = jpaSpecGenerator.pageable(request);

    Specification<ActivityLog> specification = jpaSpecGenerator.activityLogSpecification(request);

    Slice<ActivityLog> activityLogs = activityLogRepository.findAll(specification, pageable);
    assertEquals(0, activityLogs.getNumberOfElements());

  }
}
