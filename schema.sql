-- ROGUE-RELOAD 데이터베이스 스키마 (MariaDB)

-- 1. 사용자 테이블 (users)
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(45) NOT NULL,
    level INT DEFAULT 0,
    key_config TEXT
);

-- 2. 맵/방 테이블 (maps)
CREATE TABLE IF NOT EXISTS maps (
    map_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nano_id VARCHAR(16) NOT NULL UNIQUE,
    map_owner BIGINT NOT NULL,
    map_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stage INT DEFAULT 0,
    kill_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_maps_owner_users FOREIGN KEY (map_owner) REFERENCES users(user_id)
);

-- 3. 캐릭터 테이블 (characters)
CREATE TABLE IF NOT EXISTS characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    map_id BIGINT NOT NULL,
    job ENUM('FIREFIGHTER', 'SOLDIER', 'DOCTOR', 'REPORTER') NOT NULL DEFAULT 'FIREFIGHTER',
    level INT DEFAULT 0,
    exp INT DEFAULT 0,
    pos_x FLOAT DEFAULT 0.0,
    pos_y FLOAT DEFAULT 0.0,
    hp FLOAT NOT NULL,
    max_hp FLOAT NOT NULL,
    status LONGTEXT,
    angle FLOAT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_end BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_char_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_char_map FOREIGN KEY (map_id) REFERENCES maps(map_id)
);

-- 4. 몬스터 테이블 (monsters)
CREATE TABLE IF NOT EXISTS monsters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_id BIGINT NOT NULL,
    type ENUM('NORMAL', 'NAMED', 'BOSS') NOT NULL DEFAULT 'NORMAL',
    name ENUM('SLOW', 'RUNNER', 'RANGER', 'HEALTH', 'FAST_RANGER', 'INVISIBLE', 'BOMBER') NOT NULL DEFAULT 'SLOW',
    pos_x FLOAT NOT NULL,
    pos_y FLOAT NOT NULL,
    hp FLOAT DEFAULT 100.0,
    max_hp FLOAT DEFAULT 100.0,
    damage FLOAT DEFAULT 5.0,
    attack_speed FLOAT DEFAULT 10.0,
    speed FLOAT DEFAULT 10.0,
    `range` FLOAT DEFAULT 10.0,
    exp INT DEFAULT 0,
    CONSTRAINT fk_monster_map FOREIGN KEY (map_id) REFERENCES maps(map_id)
);

-- 5. 오브젝트 테이블 (objects)
CREATE TABLE IF NOT EXISTS objects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_id BIGINT NOT NULL,
    object_name ENUM('BOX', 'BOMB') NOT NULL DEFAULT 'BOX',
    pos_x INT NOT NULL,
    pos_y INT NOT NULL,
    width FLOAT DEFAULT 10.0,
    height FLOAT DEFAULT 10.0,
    is_break BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_object_map FOREIGN KEY (map_id) REFERENCES maps(map_id)
);

-- 6. 무기 테이블 (weapons)
CREATE TABLE IF NOT EXISTS weapons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_id BIGINT,
    character_id BIGINT,
    name VARCHAR(50),
    rarity ENUM('COMMON', 'RARE', 'EPIC', 'LEGENDARY') NOT NULL DEFAULT 'COMMON',
    damage FLOAT DEFAULT 1.0,
    attack_speed FLOAT DEFAULT 1.0,
    reload_speed FLOAT DEFAULT 1.0,
    type ENUM('GUN', 'MELEE') NOT NULL DEFAULT 'GUN',
    ammo INT DEFAULT 0,
    CONSTRAINT fk_weapon_object FOREIGN KEY (object_id) REFERENCES objects(id),
    CONSTRAINT fk_weapon_character FOREIGN KEY (character_id) REFERENCES characters(id)
);

-- 7. 친구 테이블 (friends - 복합 PK)
CREATE TABLE IF NOT EXISTS friends (
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_blocked BOOLEAN DEFAULT FALSE,
    blocked_at DATETIME,
    PRIMARY KEY (user_id, friend_id),
    CONSTRAINT fk_friends_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES users(user_id)
);

-- 8. 그룹 멤버 테이블 (group_members - 복합 PK)
CREATE TABLE IF NOT EXISTS group_members (
    map_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (map_id, user_id),
    CONSTRAINT fk_gm_map FOREIGN KEY (map_id) REFERENCES maps(map_id),
    CONSTRAINT fk_gm_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 9. 메시지 테이블 (messages)
CREATE TABLE IF NOT EXISTS messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msg_map FOREIGN KEY (group_id) REFERENCES maps(map_id),
    CONSTRAINT fk_msg_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 10. 보스 클리어 기록 테이블 (boss_clear_record)
CREATE TABLE IF NOT EXISTS boss_clear_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nano_id VARCHAR(255),
    nickname VARCHAR(255),
    time_taken_seconds BIGINT,
    cleared_at DATETIME
);
