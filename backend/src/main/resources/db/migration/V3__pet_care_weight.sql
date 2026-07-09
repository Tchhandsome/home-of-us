CREATE TABLE IF NOT EXISTS pet_care_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    care_type VARCHAR(40) NOT NULL,
    recorded_at DATETIME NOT NULL,
    description VARCHAR(500) NULL,
    next_due_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_pet_care_pet_time (pet_id, recorded_at),
    INDEX idx_pet_care_family_time (family_id, recorded_at),
    INDEX idx_pet_care_family_due (family_id, next_due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pet_weight_record (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    weight_kg DECIMAL(6,2) NOT NULL,
    recorded_on DATE NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_pet_weight_pet_date (pet_id, recorded_on),
    INDEX idx_pet_weight_family_date (family_id, recorded_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
