package com.portagecybertech.ca.integration.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

/**
 * 
 * Command-line PDF protection utility used by the integration-test module to scan report trees, apply AES-256 encryption and restrictive permissions, verify the saved output, preserve timestamps, and safely replace each file. It must receive an explicit reports root and a non-empty opening password on standard input.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ProtectReportPdfs {
    /**
     * 
     * Initializes the ProtectReportPdfs instance with the supplied collaborators and configuration.
     */
    private ProtectReportPdfs() {}

    /**
     * 
     * Validates command-line arguments, reads the PDF opening password from standard input, discovers every PDF beneath the requested root, applies AES-256 encryption and denied modification/printing/extraction permissions, verifies the temporary result, then safely replaces each original while preserving its modification timestamp.
     * @param args Command-line arguments; ProtectReportPdfs requires exactly one argument containing the existing reports-root directory.
     * @throws Exception when the operation cannot complete its contract
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Provide the reports directory.");
        }

        String userPassword = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        if (userPassword == null || userPassword.isEmpty()) {
            throw new IllegalArgumentException("The PDF opening password cannot be empty.");
        }

        Path reportsRoot = Path.of(args[0]).toRealPath();
        List<Path> pdfs;
        try (Stream<Path> paths = Files.walk(reportsRoot)) {
            pdfs = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
        if (pdfs.isEmpty()) {
            throw new IllegalStateException("No PDF files were found under " + reportsRoot);
        }

        SecureRandom random = new SecureRandom();
        for (Path pdf : pdfs) {
            if (isAlreadyProtected(pdf, userPassword)) {
                System.out.println("Already protected and verified: " + pdf);
                continue;
            }
            FileTime modified = Files.getLastModifiedTime(pdf);
            Path temporary = Files.createTempFile(pdf.getParent(), pdf.getFileName().toString(), ".protected");
            try {
                try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
                    if (document.isEncrypted()) {
                        throw new IllegalStateException("PDF is already encrypted; refusing to overwrite: " + pdf);
                    }
                    AccessPermission permissions = new AccessPermission();
                    permissions.setCanPrint(false);
                    permissions.setCanPrintFaithful(false);
                    permissions.setCanExtractContent(false);
                    permissions.setCanExtractForAccessibility(false);
                    permissions.setCanModify(false);
                    permissions.setCanModifyAnnotations(false);
                    permissions.setCanFillInForm(false);
                    permissions.setCanAssembleDocument(false);

                    String ownerPassword = randomToken(random);
                    StandardProtectionPolicy policy =
                            new StandardProtectionPolicy(ownerPassword, userPassword, permissions);
                    policy.setEncryptionKeyLength(256);
                    policy.setPreferAES(true);
                    document.protect(policy);
                    document.save(temporary.toFile());
                }

                try (PDDocument protectedDocument = Loader.loadPDF(temporary.toFile(), userPassword)) {
                    verifyPermissions(protectedDocument, pdf);
                }

                Files.setLastModifiedTime(temporary, modified);
                try {
                    Files.move(temporary, pdf, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, pdf, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("Protected and verified: " + pdf);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        System.out.println("Protected " + pdfs.size() + " PDF files.");
    }

    /**
     * 
     * Determines whether the file is already encrypted with the supplied opening password and verifies all required restrictions; it refuses to overwrite a file protected by an unrecognized password.
     * @param pdf PDF file whose encryption state and user permissions are verified.
     * @param userPassword Non-empty PDF user/opening password supplied interactively or through the secure test parameter; it must never be logged or written to a file.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private static boolean isAlreadyProtected(Path pdf, String userPassword) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            if (!document.isEncrypted()) {
                return false;
            }
        } catch (InvalidPasswordException passwordRequired) {
            try (PDDocument document = Loader.loadPDF(pdf.toFile(), userPassword)) {
                verifyPermissions(document, pdf);
                return true;
            } catch (InvalidPasswordException differentPassword) {
                throw new IllegalStateException(
                        "PDF is encrypted with a different password; refusing to overwrite: " + pdf,
                        differentPassword);
            }
        }
        try (PDDocument document = Loader.loadPDF(pdf.toFile(), userPassword)) {
            verifyPermissions(document, pdf);
            return true;
        }
    }

    /**
     * 
     * Checks that PDF encryption is active and that printing, content extraction, accessibility extraction, modification, annotations, form filling, and assembly are all disallowed.
     * @param document PDFBox document currently being inspected or protected.
     * @param pdf PDF file whose encryption state and user permissions are verified.
     */
    private static void verifyPermissions(PDDocument document, Path pdf) {
        AccessPermission actual = document.getCurrentAccessPermission();
        if (!document.isEncrypted() || actual.canPrint() || actual.canPrintFaithful()
                || actual.canExtractContent() || actual.canExtractForAccessibility()
                || actual.canModify() || actual.canModifyAnnotations()
                || actual.canFillInForm() || actual.canAssembleDocument()) {
            throw new IllegalStateException("PDF protection verification failed: " + pdf);
        }
    }

    /**
     * 
     * Generates a cryptographically random hexadecimal owner password for PDF permission administration, independent of the user opening password.
     * @param random Cryptographically secure random-number generator used to create the independent PDF owner password.
     * @return the result described above.
     */
    private static String randomToken(SecureRandom random) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            token.append(String.format("%02x", value & 0xff));
        }
        return token.toString();
    }
}
