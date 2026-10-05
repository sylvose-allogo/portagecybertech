package com.portagecybertech.ca.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
/**
 * 
 * Bootstraps the protected Resource Server Spring Boot service and its application lifecycle. HTTP authorization, token decoding, and endpoint behaviour are configured by dedicated components.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class ResourceServerApplication {

        /**
         * 
         * Starts the protected Resource Server Spring Boot application and delegates process configuration and shutdown handling to SpringApplication.
         * @param args Command-line arguments; ProtectReportPdfs requires exactly one argument containing the existing reports-root directory.
         */
        public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }
}