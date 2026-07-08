CREATE TABLE IF NOT EXISTS family (
    id BIGINT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS family_member (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role_code VARCHAR(40) NOT NULL,
    avatar_color VARCHAR(20) NULL,
    avatar_url VARCHAR(1000) NULL,
    bio VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_family_member_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    family_member_id BIGINT NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    session_token VARCHAR(128) NULL,
    last_login_at DATETIME NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_app_user_username (username),
    INDEX idx_app_user_family_id (family_id),
    INDEX idx_app_user_token (session_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS quick_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    raw_text TEXT NOT NULL,
    record_type VARCHAR(40) NOT NULL,
    linked_type VARCHAR(40) NULL,
    linked_id BIGINT NULL,
    happened_on DATE NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_quick_record_family_created (family_id, created_at),
    INDEX idx_quick_record_linked (linked_type, linked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reminder (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NULL,
    due_at DATETIME NOT NULL,
    repeat_rule VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_reminder_family_due (family_id, due_at),
    INDEX idx_reminder_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS plant (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    variety VARCHAR(80) NULL,
    flower_color VARCHAR(40) NULL,
    location VARCHAR(80) NULL,
    status VARCHAR(40) NOT NULL,
    care_preference VARCHAR(500) NULL,
    acquired_on DATE NULL,
    cover_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_plant_family_name (family_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS plant_care_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    plant_id BIGINT NOT NULL,
    care_type VARCHAR(40) NOT NULL,
    care_date DATE NOT NULL,
    detail VARCHAR(500) NULL,
    raw_text TEXT NULL,
    next_care_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_plant_care_plant_date (plant_id, care_date),
    INDEX idx_plant_care_family_date (family_id, care_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chore_task (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    assignee_id BIGINT NULL,
    task_type VARCHAR(40) NOT NULL,
    cycle_rule VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL,
    due_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_chore_family_status (family_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS shopping_item (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NULL,
    channel VARCHAR(60) NULL,
    quantity VARCHAR(40) NULL,
    status VARCHAR(30) NOT NULL,
    actual_amount DECIMAL(12,2) NULL,
    purchased_at DATETIME NULL,
    buyer_id BIGINT NULL,
    finance_record_id BIGINT NULL,
    checked_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_shopping_family_status (family_id, status),
    INDEX idx_shopping_finance_record_id (finance_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS inventory_item (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NULL,
    quantity DECIMAL(12,2) NOT NULL DEFAULT 0,
    unit VARCHAR(20) NULL,
    low_stock_threshold DECIMAL(12,2) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_inventory_family_status (family_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS finance_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    category VARCHAR(60) NULL,
    owner_id BIGINT NULL,
    occurred_on DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_finance_family_occurred (family_id, occurred_on),
    INDEX idx_finance_direction (direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attachment_file (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    file_type VARCHAR(60) NULL,
    storage_path VARCHAR(500) NOT NULL,
    linked_type VARCHAR(40) NULL,
    linked_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_attachment_family_created (family_id, created_at),
    INDEX idx_attachment_linked (linked_type, linked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    sender_member_id BIGINT NOT NULL,
    receiver_member_id BIGINT NULL,
    visibility VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_private_message_family_created (family_id, created_at),
    INDEX idx_private_message_receiver (receiver_member_id),
    INDEX idx_private_message_sender (sender_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS album_photo (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    description VARCHAR(500) NULL,
    taken_on DATE NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_album_photo_family_created (family_id, created_at),
    INDEX idx_album_photo_taken_on (taken_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pet (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    species VARCHAR(40) NOT NULL,
    breed VARCHAR(80) NULL,
    gender VARCHAR(20) NULL,
    birthday DATE NULL,
    avatar_url VARCHAR(1000) NULL,
    note VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_pet_family_status (family_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pet_photo (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    description VARCHAR(500) NULL,
    taken_on DATE NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_pet_photo_pet_created (pet_id, created_at),
    INDEX idx_pet_photo_family_created (family_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pet_medical_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    record_type VARCHAR(40) NOT NULL,
    record_date DATE NOT NULL,
    hospital VARCHAR(120) NULL,
    medicine VARCHAR(120) NULL,
    description VARCHAR(500) NULL,
    next_due_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_pet_medical_pet_date (pet_id, record_date),
    INDEX idx_pet_medical_family_date (family_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE family_member ADD COLUMN avatar_url VARCHAR(1000) NULL AFTER avatar_color',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'family_member' AND COLUMN_NAME = 'avatar_url'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE family_member ADD COLUMN bio VARCHAR(500) NULL AFTER avatar_url',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'family_member' AND COLUMN_NAME = 'bio'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO family (id, name, description, created_at, updated_at, created_by, updated_by, deleted)
SELECT 1, '我们的小家', 'Home Of Us 默认家庭空间', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM family WHERE id = 1);

INSERT INTO family_member (id, family_id, display_name, role_code, avatar_color, avatar_url, bio, created_at, updated_at, created_by, updated_by, deleted)
SELECT 1001, 1, '小谭', 'OWNER', '#2F6B4F', NULL, NULL, NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM family_member WHERE id = 1001);

INSERT INTO family_member (id, family_id, display_name, role_code, avatar_color, avatar_url, bio, created_at, updated_at, created_by, updated_by, deleted)
SELECT 1002, 1, '丹丹', 'PARTNER', '#D46A6A', NULL, NULL, NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM family_member WHERE id = 1002);

UPDATE family_member
SET display_name = '小谭', role_code = 'OWNER', avatar_color = '#2F6B4F', updated_at = NOW(), updated_by = 1001,
    deleted = 0
WHERE id = 1001;

UPDATE family_member
SET display_name = '丹丹', role_code = 'PARTNER', avatar_color = '#D46A6A', updated_at = NOW(), updated_by = 1001,
    deleted = 0
WHERE id = 1002;

UPDATE family_member
SET display_name = CONCAT(display_name, '（旧）'), updated_at = NOW(), updated_by = 1001, deleted = 1
WHERE id NOT IN (1001, 1002) AND display_name IN ('我', '她', '小谭', '丹丹');

UPDATE app_user
SET username = CONCAT(username, '_old_', id), status = 'DELETED', updated_at = NOW(), updated_by = 1001, deleted = 1
WHERE id NOT IN (1001, 1002) AND username IN ('me', 'her', '小谭', '丹丹');

INSERT INTO app_user (id, family_id, family_member_id, username, password_hash, display_name, session_token,
    last_login_at, status, created_at, updated_at, created_by, updated_by, deleted)
SELECT 1001, 1, 1001, '小谭', SHA2(CONCAT('home-of-us:', '123456'), 256), '小谭', NULL,
    NULL, 'ACTIVE', NOW(), NOW(), 1001, 1001, 0
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE id = 1001);

INSERT INTO app_user (id, family_id, family_member_id, username, password_hash, display_name, session_token,
    last_login_at, status, created_at, updated_at, created_by, updated_by, deleted)
SELECT 1002, 1, 1002, '丹丹', SHA2(CONCAT('home-of-us:', '123456'), 256), '丹丹', NULL,
    NULL, 'ACTIVE', NOW(), NOW(), 1001, 1001, 0
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE id = 1002);

UPDATE app_user
SET family_id = 1, family_member_id = 1001, username = '小谭',
    password_hash = SHA2(CONCAT('home-of-us:', '123456'), 256), display_name = '小谭',
    status = 'ACTIVE', updated_at = NOW(), updated_by = 1001, deleted = 0
WHERE id = 1001;

UPDATE app_user
SET family_id = 1, family_member_id = 1002, username = '丹丹',
    password_hash = SHA2(CONCAT('home-of-us:', '123456'), 256), display_name = '丹丹',
    status = 'ACTIVE', updated_at = NOW(), updated_by = 1001, deleted = 0
WHERE id = 1002;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE family_member ADD COLUMN avatar_url VARCHAR(1000) NULL AFTER avatar_color',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'family_member' AND COLUMN_NAME = 'avatar_url'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE family_member ADD COLUMN bio VARCHAR(500) NULL AFTER avatar_url',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'family_member' AND COLUMN_NAME = 'bio'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE shopping_item ADD COLUMN actual_amount DECIMAL(12,2) NULL AFTER status',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shopping_item' AND COLUMN_NAME = 'actual_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE shopping_item ADD COLUMN purchased_at DATETIME NULL AFTER actual_amount',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shopping_item' AND COLUMN_NAME = 'purchased_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE shopping_item ADD COLUMN buyer_id BIGINT NULL AFTER purchased_at',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shopping_item' AND COLUMN_NAME = 'buyer_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE shopping_item ADD COLUMN finance_record_id BIGINT NULL AFTER buyer_id',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shopping_item' AND COLUMN_NAME = 'finance_record_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
