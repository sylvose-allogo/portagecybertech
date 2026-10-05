package com.portagecybertech.ca.integration.util;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 
 * System-level test of complete report-tree processing, including root and nested PDFs and preservation of a non-PDF companion file.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class ProtectReportPdfsSystemTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    /**
     * 
     * Exercises a multi-level report tree and verifies both protected PDFs and byte-preserving treatment of a non-PDF companion.
     * @throws Exception when the operation cannot complete its contract
     */
    void protectsAnEntireReportTreeWithoutChangingNonPdfFiles() throws Exception {
        String password = UUID.randomUUID().toString();
        Path rootReport = PdfProtectionTestSupport.createPdf(
                temporaryDirectory.resolve("overview.pdf"));
        Path nestedReport = PdfProtectionTestSupport.createPdf(
                temporaryDirectory.resolve("archive/nested.pdf"));
        Path textFile = temporaryDirectory.resolve("archive/notes.txt");
        java.nio.file.Files.writeString(textFile, "Not a PDF");

        PdfProtectionTestSupport.protectReports(temporaryDirectory, password);

        PdfProtectionTestSupport.assertProtected(rootReport, password);
        PdfProtectionTestSupport.assertProtected(nestedReport, password);
        org.junit.jupiter.api.Assertions.assertEquals("Not a PDF",
                java.nio.file.Files.readString(textFile));
    }
}
