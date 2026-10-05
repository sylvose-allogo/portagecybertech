package com.portagecybertech.ca.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})

/**
 * 
 * Bootstraps the OAuth 2.0 Authorization Server Spring Boot service. It assembles the authorization endpoints, signing-key publication, application configuration, and service lifecycle; it does not itself authenticate an end user.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class AuthorizationServerApplication {


    /**
     * 
     * Starts the Authorization Server Spring Boot application and delegates process configuration and shutdown handling to SpringApplication.
     * @param args Command-line arguments; ProtectReportPdfs requires exactly one argument containing the existing reports-root directory.
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }
}
