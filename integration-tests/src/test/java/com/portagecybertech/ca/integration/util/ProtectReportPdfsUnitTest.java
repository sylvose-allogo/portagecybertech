package com.portagecybertech.ca.integration.util;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 
 * Focused utility-level demonstration test for a single temporary PDF, invoking the actual PowerShell/PDFBox protection workflow and asserting password and permission enforcement.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class ProtectReportPdfsUnitTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    /**
     * 
     * Creates one temporary PDF, runs the packaged protection script, and asserts that password and restrictive permissions were applied.
     * @throws Exception when the operation cannot complete its contract
     */
    void protectsASinglePdfAndRestrictsItsPermissions() throws Exception {
        String password = UUID.randomUUID().toString();
        Path report = PdfProtectionTestSupport.createPdf(temporaryDirectory.resolve("unit.pdf"));

        PdfProtectionTestSupport.protectReports(temporaryDirectory, password);

        PdfProtectionTestSupport.assertProtected(report, password);
    }
}
