package com.portagecybertech.ca.integration.util;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 
 * Microservice-oriented test verifying the shared report-protection workflow against separate Authorization Server and Resource Server report directories.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class ProtectReportPdfsMicroserviceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    /**
     * 
     * Exercises separate Authorization Server and Resource Server report folders in one protection run and verifies both outputs.
     * @throws Exception when the operation cannot complete its contract
     */
    void protectsAuthorizationAndResourceServiceReports() throws Exception {
        String password = UUID.randomUUID().toString();
        Path authorizationReport = PdfProtectionTestSupport.createPdf(
                temporaryDirectory.resolve("authorization-server/contract-report.pdf"));
        Path resourceReport = PdfProtectionTestSupport.createPdf(
                temporaryDirectory.resolve("resource-server/contract-report.pdf"));

        PdfProtectionTestSupport.protectReports(temporaryDirectory, password);

        PdfProtectionTestSupport.assertProtected(authorizationReport, password);
        PdfProtectionTestSupport.assertProtected(resourceReport, password);
    }
}
