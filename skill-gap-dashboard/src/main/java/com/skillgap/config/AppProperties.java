package com.skillgap.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application-level settings bound from application.properties. */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Private directory where uploaded files are stored (never web-accessible). */
    private String uploadDir = "./data/uploads";

    private final Security security = new Security();
    private boolean seedDemoData = true;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public Path uploadPath() {
        return Path.of(uploadDir);
    }

    public Security getSecurity() {
        return security;
    }

    public boolean isSeedDemoData() {
        return seedDemoData;
    }

    public void setSeedDemoData(boolean seedDemoData) {
        this.seedDemoData = seedDemoData;
    }

    public static class Security {
        /** Failed logins allowed per username before temporary lockout. */
        private int loginMaxAttempts = 5;
        private int loginLockMinutes = 5;

        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public int getLoginLockMinutes() {
            return loginLockMinutes;
        }

        public void setLoginLockMinutes(int loginLockMinutes) {
            this.loginLockMinutes = loginLockMinutes;
        }
    }
}
