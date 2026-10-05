package com.portagecybertech.ca.integration.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.Assumptions;

/**
 * 
 * Shared test fixture for creating temporary PDF inputs, invoking the packaged PowerShell protection runner, and verifying password enforcement and every configured PDF permission without touching repository reports.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
final class PdfProtectionTestSupport {
    private static final String PASSWORD_ENV = "PORTAGE_PDF_TEST_PASSWORD";
    private static final String ROOT_ENV = "PORTAGE_PDF_TEST_REPORTS";
    private static final String CLASSPATH_ENV = "PORTAGE_PDF_TEST_CLASSPATH";
    private static final String SCRIPT_ENV = "PORTAGE_PDF_TEST_SCRIPT";
    private static final String SOURCE_ENV = "PORTAGE_PDF_TEST_SOURCE";
    private static final String COMMAND = "$password = ConvertTo-SecureString "
            + "$env:PORTAGE_PDF_TEST_PASSWORD -AsPlainText -Force; "
            + "& $env:PORTAGE_PDF_TEST_SCRIPT -ReportsRoot $env:PORTAGE_PDF_TEST_REPORTS "
            + "-JavaClassPath $env:PORTAGE_PDF_TEST_CLASSPATH "
            + "-JavaSource $env:PORTAGE_PDF_TEST_SOURCE -OpeningPassword $password";

    /**
     * 
     * Initializes the PdfProtectionTestSupport instance with the supplied collaborators and configuration.
     */
    private PdfProtectionTestSupport() {
    }

    /**
     * 
     * Creates parent directories and saves a valid one-page PDF fixture at the requested temporary destination.
     * @param path Relative HTTP endpoint path appended to the local service base URL.
     * @return the result described above.
     * @throws IOException when the operation cannot complete its contract
     */
    static Path createPdf(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(path.toFile());
        }
        return path;
    }

    /**
     * 
     * Resolves the packaged PowerShell resource, prepares an isolated process environment, supplies a test-only password without putting it on the command line, and fails with captured process output if protection does not complete successfully.
     * @param reportsRoot Existing directory tree whose PDF files are in scope for protection; the utility traverses its descendants recursively.
     * @param password Non-empty test-only or operator-provided PDF opening password; never log or persist this value.
     * @throws IOException when the operation cannot complete its contract
     * @throws InterruptedException when the operation cannot complete its contract
     * @throws URISyntaxException when the operation cannot complete its contract
     */
    static void protectReports(Path reportsRoot, String password)
            throws IOException, InterruptedException, URISyntaxException {
        String shell = isWindows() ? "powershell.exe" : "pwsh";
        Assumptions.assumeTrue(isExecutableAvailable(shell),
                "PDF protection tests require PowerShell");
        Path script = Path.of(PdfProtectionTestSupport.class
                .getClassLoader().getResource("scripts/Protect-Report-Pdfs.ps1").toURI());
        Path source = Path.of("src/test/java/com/portagecybertech/ca/integration/util/ProtectReportPdfs.java")
                .toAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder(shell, "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-Command", COMMAND);
        builder.redirectErrorStream(true);
        builder.environment().put(PASSWORD_ENV, password);
        builder.environment().put(ROOT_ENV, reportsRoot.toAbsolutePath().toString());
        builder.environment().put(CLASSPATH_ENV, System.getProperty("java.class.path"));
        builder.environment().put(SCRIPT_ENV, script.toString());
        builder.environment().put(SOURCE_ENV, source.toString());

        Process process = builder.start();
        byte[] output;
        try (var stream = process.getInputStream()) {
            if (!process.waitFor(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("PDF protection test process timed out");
            }
            output = stream.readAllBytes();
        }
        if (process.exitValue() != 0) {
            throw new AssertionError("PDF protection script failed: "
                    + new String(output, StandardCharsets.UTF_8));
        }
    }

    /**
     * 
     * Verifies that an empty-password open is rejected and that the supplied password opens the PDF with every prohibited permission disabled.
     * @param pdf PDF file whose encryption state and user permissions are verified.
     * @param password Non-empty test-only or operator-provided PDF opening password; never log or persist this value.
     * @throws IOException when the operation cannot complete its contract
     */
    static void assertProtected(Path pdf, String password) throws IOException {
        try (PDDocument ignored = Loader.loadPDF(pdf.toFile())) {
            throw new AssertionError("Expected PDF to require an opening password: " + pdf);
        } catch (InvalidPasswordException expected) {
            // Password-protected PDFs must reject an empty-password open.
        }
        try (PDDocument document = Loader.loadPDF(pdf.toFile(), password)) {
            AccessPermission permissions = document.getCurrentAccessPermission();
            if (!document.isEncrypted() || permissions.canPrint() || permissions.canPrintFaithful()
                    || permissions.canExtractContent() || permissions.canExtractForAccessibility()
                    || permissions.canModify() || permissions.canModifyAnnotations()
                    || permissions.canFillInForm() || permissions.canAssembleDocument()) {
                throw new AssertionError("PDF permissions were not restricted: " + pdf);
            }
        }
    }

    /**
     * 
     * Selects the Windows PowerShell executable when the current JVM runs on Windows.
     * @return the result described above.
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 
     * Checks whether the selected PowerShell host starts successfully within a bounded wait, restoring the interrupt flag when the probe is interrupted.
     * @param executable PowerShell executable name checked before starting the protection test.
     * @return the result described above.
     */
    private static boolean isExecutableAvailable(String executable) {
        try {
            Process process = new ProcessBuilder(executable, "-NoProfile",
                    "-ExecutionPolicy", "Bypass", "-Command", "exit 0")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException | InterruptedException unavailable) {
            if (unavailable instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
