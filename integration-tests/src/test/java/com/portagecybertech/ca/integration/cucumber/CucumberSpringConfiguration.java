package com.portagecybertech.ca.integration.cucumber;

import com.portagecybertech.ca.authorization.AuthorizationServerApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = AuthorizationServerApplication.class)
@AutoConfigureMockMvc
/**
 * 
 * Connects the Cucumber integration-test engine to the Authorization Server Spring test context and MockMvc. It supplies a reusable Spring context for HTTP-level acceptance steps.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class CucumberSpringConfiguration {
}