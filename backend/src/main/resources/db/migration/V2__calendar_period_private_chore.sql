CREATE TABLE IF NOT EXISTS chore_task_assignee (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_chore_task_assignee_task_member (task_id, member_id),
    INDEX idx_chore_task_assignee_family_member (family_id, member_id),
    INDEX idx_chore_task_assignee_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS period_profile (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    cycle_days INT NOT NULL DEFAULT 28,
    period_days INT NOT NULL DEFAULT 5,
    last_period_start DATE NULL,
    reminder_enabled TINYINT(1) NOT NULL DEFAULT 1,
    reminder_time VARCHAR(10) NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_period_profile_member (member_id),
    INDEX idx_period_profile_family_member (family_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS period_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    start_on DATE NOT NULL,
    end_on DATE NULL,
    is_predicted TINYINT(1) NOT NULL DEFAULT 0,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_period_record_member_start (member_id, start_on),
    INDEX idx_period_record_family_start (family_id, start_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @ddl = (
    SELECT IF(COUNT(1) = 0,
        'ALTER TABLE private_message ADD COLUMN message_date DATE NULL AFTER content',
        'DO 0')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'private_message' AND COLUMN_NAME = 'message_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
