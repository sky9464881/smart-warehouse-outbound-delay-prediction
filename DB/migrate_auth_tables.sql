USE warehouse_db;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_factory_access (
    user_id BIGINT NOT NULL,
    layout_id VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, layout_id),
    CONSTRAINT fk_user_factory_user FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_factory_layout FOREIGN KEY (layout_id)
        REFERENCES warehouse_layout(layout_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auth_token (
    token VARCHAR(96) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,
    INDEX idx_auth_token_user (user_id),
    INDEX idx_auth_token_expires (expires_at)
);

