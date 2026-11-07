CREATE TABLE IF NOT EXISTS units (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(150) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_units_code (code)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    unit_id BIGINT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role),
    KEY idx_users_unit_id (unit_id),
    CONSTRAINT fk_users_unit FOREIGN KEY (unit_id) REFERENCES units(id)
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    available_quantity INT NOT NULL DEFAULT 0,
    minimum_stock_threshold INT NOT NULL DEFAULT 0,
    unit_of_measure VARCHAR(50) NOT NULL,
    storage_location VARCHAR(150) DEFAULT NULL,
    supplier VARCHAR(150) DEFAULT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    item_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_item_id (item_id),
    KEY idx_inventory_category (category),
    KEY idx_inventory_status (item_status)
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inventory_item_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    reference VARCHAR(150) DEFAULT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_inventory_item (inventory_item_id),
    KEY idx_stock_created_at (created_at),
    CONSTRAINT fk_stock_inventory FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
);

CREATE TABLE IF NOT EXISTS supply_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(100) NOT NULL,
    requesting_unit VARCHAR(150) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    requested_quantity INT NOT NULL,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    priority VARCHAR(50) NOT NULL,
    reason TEXT,
    status VARCHAR(50) NOT NULL,
    approved_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supply_request_id (request_id)
);

CREATE TABLE IF NOT EXISTS request_approvals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supply_request_id BIGINT NOT NULL,
    approver VARCHAR(150) NOT NULL,
    action VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_approvals_request (supply_request_id),
    CONSTRAINT fk_approvals_request FOREIGN KEY (supply_request_id) REFERENCES supply_requests(id)
);

CREATE TABLE IF NOT EXISTS equipment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    equipment_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    serial_number VARCHAR(150) NOT NULL,
    assigned_unit VARCHAR(150) DEFAULT NULL,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_information TEXT,
    current_condition VARCHAR(50) NOT NULL,
    service_status VARCHAR(50) NOT NULL,
    last_maintenance_date TIMESTAMP NULL,
    next_maintenance_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_id (equipment_id),
    UNIQUE KEY uk_equipment_serial (serial_number)
);

CREATE TABLE IF NOT EXISTS maintenance_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    equipment_id BIGINT NOT NULL,
    maintenance_type VARCHAR(100) NOT NULL,
    scheduled_date TIMESTAMP NULL,
    completed_date TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL,
    remarks TEXT,
    risk_score DOUBLE DEFAULT 0,
    downtime_hours DOUBLE DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_maintenance_equipment (equipment_id),
    CONSTRAINT fk_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id)
);

CREATE TABLE IF NOT EXISTS consumption_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    consumption_date TIMESTAMP NOT NULL,
    quantity_consumed INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_consumption_item_date (item_name, consumption_date)
);

CREATE TABLE IF NOT EXISTS forecast_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_name VARCHAR(150) NOT NULL,
    forecast_date TIMESTAMP NOT NULL,
    current_stock INT NOT NULL,
    predicted_demand DOUBLE NOT NULL,
    expected_remaining_stock DOUBLE NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    explanation TEXT,
    model_name VARCHAR(100) DEFAULT NULL,
    forecast_horizon INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_forecast_item_date (item_name, forecast_date)
);

CREATE TABLE IF NOT EXISTS resource_allocations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    unit_name VARCHAR(150) NOT NULL,
    resource_category VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    recommended_by VARCHAR(150) DEFAULT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(150) DEFAULT NULL,
    action VARCHAR(150) NOT NULL,
    entity_type VARCHAR(100) DEFAULT NULL,
    entity_id VARCHAR(100) DEFAULT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_created_at (created_at)
);
