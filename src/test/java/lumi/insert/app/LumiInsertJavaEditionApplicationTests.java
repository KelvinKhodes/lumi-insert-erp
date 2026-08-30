package lumi.insert.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@EnableCaching
class LumiInsertJavaEditionApplicationTests extends TestContainerTest{

	@Test
	void contextLoads() {
	}

}
