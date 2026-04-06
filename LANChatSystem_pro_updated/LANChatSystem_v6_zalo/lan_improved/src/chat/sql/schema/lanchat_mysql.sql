-- ============================================================
--  LAN Chat System — CSDL MySQL (chạy trên XAMPP)
--  Import file này vào phpMyAdmin hoặc chạy qua MySQL CLI
-- ============================================================

CREATE DATABASE IF NOT EXISTS lanchat
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lanchat;

-- ── Bảng người dùng ─────────────────────────────────────────
-- roles: admin | truong_phong | pho_phong | nhan_vien | user
CREATE TABLE IF NOT EXISTS users (
    username   VARCHAR(50)  PRIMARY KEY,
    password   VARCHAR(64)  NOT NULL,          -- SHA-256 hex
    role       VARCHAR(20)  DEFAULT 'nhan_vien',
    fullname   VARCHAR(100) DEFAULT NULL,
    email      VARCHAR(100) DEFAULT NULL,
    emp_code   VARCHAR(20)  DEFAULT NULL,
    banned     TINYINT(1)   DEFAULT 0,
    status     VARCHAR(10)  DEFAULT 'offline',
    last_seen  DATETIME     DEFAULT NULL,
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Bảng phiên đăng nhập ────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
    session_id    VARCHAR(64)  PRIMARY KEY,
    username      VARCHAR(50),
    ip_address    VARCHAR(45),
    is_online     TINYINT(1)   DEFAULT 1,
    last_activity DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Bảng cấm ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bans (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50),
    banned_by    VARCHAR(50),
    reason       TEXT,
    is_permanent TINYINT(1) DEFAULT 1,
    banned_at    DATETIME   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Trigger cập nhật banned sau khi insert bans
DELIMITER //
CREATE TRIGGER IF NOT EXISTS after_ban_insert
AFTER INSERT ON bans
FOR EACH ROW
BEGIN
    UPDATE users SET banned=1 WHERE username=NEW.username;
END//

CREATE TRIGGER IF NOT EXISTS after_ban_delete
AFTER DELETE ON bans
FOR EACH ROW
BEGIN
    UPDATE users SET banned=0 WHERE username=OLD.username;
END//
DELIMITER ;

-- ── Bảng tin nhắn ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id        INT          AUTO_INCREMENT PRIMARY KEY,
    sender    VARCHAR(50),
    target    VARCHAR(50),
    content   TEXT,
    ts        VARCHAR(20),
    type      VARCHAR(20)  DEFAULT 'broadcast',
    created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender_target (sender, target)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Bảng nhóm ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rooms (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_by VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS room_members (
    room_id   INT,
    username  VARCHAR(50),
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, username),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Seed tài khoản admin mặc định ───────────────────────────
-- password = SHA-256("admin123")
INSERT IGNORE INTO users (username, password, role, fullname)
VALUES (
    'admin',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'admin',
    'Quản trị viên'
);
