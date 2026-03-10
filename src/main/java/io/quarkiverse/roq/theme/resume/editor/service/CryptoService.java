package io.quarkiverse.roq.theme.resume.editor.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.SQLException;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class CryptoService {

    private static final Logger LOG = Logger.getLogger(CryptoService.class);
    private static final String ENCRYPTED_DB_FILE = "resume.db.enc";
    private static final String TEMP_DB_FILE = "resume-tui.db";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    @ConfigProperty(name = "roq.resume.password")
    Optional<String> envPassword;

    @Inject
    Flyway flyway;

    @Inject
    DataSource dataSource;

    private String password;

    void onStart(@Observes StartupEvent ev) {
        // Password handling logic will be triggered by main entry point
        // But we ensure clean state here if needed
    }

    public void initialize(String passwordInput) throws Exception {
        this.password = envPassword.orElse(passwordInput);
        if (this.password == null || this.password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        File encryptedFile = new File(ENCRYPTED_DB_FILE);
        File tempFile = new File(TEMP_DB_FILE);

        if (encryptedFile.exists()) {
            decryptDatabase(encryptedFile, tempFile);
            LOG.info("Database decrypted successfully.");
        } else {
            LOG.info("No encrypted database found. A new one will be created/encrypted on exit.");
        }

        // Manually trigger Flyway migration
        flyway.migrate();
        LOG.info("Database migration completed.");
    }

    public void saveAndEncrypt() throws Exception {
        if (password == null)
            return; // Not initialized

        File encryptedFile = new File(ENCRYPTED_DB_FILE);

        int prefixLength = "jdbc:sqlite:".length();
        int queryParamsIdx = jdbcUrl.indexOf('?');
        int length = (queryParamsIdx != -1) ? queryParamsIdx : jdbcUrl.length();
        String dbFile = jdbcUrl.substring(prefixLength, length);

        var originalDbFilePath = Paths.get(dbFile);
        File tempFile = new File(originalDbFilePath.toUri());
        LOG.info("Starting DB backup for file: " + dbFile);
        var backupDbFilePath = originalDbFilePath.toAbsolutePath().getParent()
                .resolve(originalDbFilePath.getFileName() + "_backup");

        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            // Execute the backup
            stmt.executeUpdate("backup to " + backupDbFilePath);
            // Atomically substitute the DB file with its backup
            Files.move(backupDbFilePath, originalDbFilePath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to backup the database", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create backup files or folders", e);
        }

        LOG.info("Backup of " + dbFile + " completed.");

        if (tempFile.exists()) {
            encryptDatabase(tempFile, encryptedFile);
            LOG.info("Database encrypted and saved.");
            // Secure delete of temp file
            Files.delete(tempFile.toPath());
        }
    }

    private void encryptDatabase(File inputFile, File outputFile) throws Exception {
        // Generate Salt and IV
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey secretKey = getSecretKey(password, salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(salt);
            fos.write(iv);
            try (FileInputStream fis = new FileInputStream(inputFile);
                    CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private void decryptDatabase(File inputFile, File outputFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] salt = new byte[SALT_LENGTH];
            if (fis.read(salt) != SALT_LENGTH)
                throw new IOException("Invalid file format");

            byte[] iv = new byte[IV_LENGTH];
            if (fis.read(iv) != IV_LENGTH)
                throw new IOException("Invalid file format");

            SecretKey secretKey = getSecretKey(password, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private SecretKey getSecretKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public boolean isPasswordSet() {
        return envPassword.isPresent();
    }
}
