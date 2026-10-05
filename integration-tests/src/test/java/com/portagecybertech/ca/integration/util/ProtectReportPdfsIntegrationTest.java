package com.portagecybertech.ca.integration.util;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 
 * Integration test for recursive discovery and protection of PDF files located in multiple nested report-language directories.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class ProtectReportPdfsIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    /**
     * 
     * Creates French and English report fixtures in nested paths, runs recursive protection once, and verifies both resulting PDFs.
     * @throws Exception when the operation cannot complete its contract
     */
    void protectsPdfFilesInNestedReportDirectories() throws Exception {
        String password = UUID.randomUUID().toString();
        Path frenchReport = PdfProtectionTestSupport.createPdf(
                temporaryDirectory.resolve("fr/rapport.pdf"));
        Path englishReport = PdfProtectionTestSupport.createPdf(
                temporaryDirectory.resolve("en/report.pdf"));

        PdfProtectionTestSupport.protectReports(temporaryDirectory, password);

        PdfProtectionTestSupport.assertProtected(frenchReport, password);
        PdfProtectionTestSupport.assertProtected(englishReport, password);
    }
}
