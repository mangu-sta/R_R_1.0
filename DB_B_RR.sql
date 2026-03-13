-- --------------------------------------------------------
-- 호스트:                          127.0.0.1
-- 서버 버전:                        12.0.2-MariaDB - mariadb.org binary distribution
-- 서버 OS:                        Win64
-- HeidiSQL 버전:                  12.11.0.7065
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- rr 데이터베이스 구조 내보내기
CREATE DATABASE IF NOT EXISTS `rr` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */;
USE `rr`;

-- 테이블 rr.boss_clear_record 구조 내보내기
CREATE TABLE IF NOT EXISTS `boss_clear_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cleared_at` datetime(6) DEFAULT NULL,
  `nano_id` varchar(255) DEFAULT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `time_taken_seconds` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.boss_clear_record:~0 rows (대략적) 내보내기

-- 테이블 rr.characters 구조 내보내기
CREATE TABLE IF NOT EXISTS `characters` (
  `exp` int(11) DEFAULT NULL,
  `hp` float DEFAULT NULL,
  `is_end` bit(1) DEFAULT NULL,
  `level` int(11) DEFAULT NULL,
  `max_hp` float DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `map_id` bigint(20) NOT NULL,
  `pos_x` float DEFAULT NULL,
  `pos_y` float DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `status` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`status`)),
  `job` enum('DOCTOR','FIREFIGHTER','REPORTER','SOLDIER') NOT NULL,
  `angle` float NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_char_map` (`map_id`),
  KEY `fk_char_user` (`user_id`),
  CONSTRAINT `fk_char_map` FOREIGN KEY (`map_id`) REFERENCES `maps` (`map_id`),
  CONSTRAINT `fk_char_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.characters:~11 rows (대략적) 내보내기
INSERT INTO `characters` (`exp`, `hp`, `is_end`, `level`, `max_hp`, `created_at`, `id`, `map_id`, `pos_x`, `pos_y`, `user_id`, `status`, `job`, `angle`) VALUES
	(0, 108, b'0', 0, 100, '2025-12-09 16:12:28.456771', 16, 4, 1550.2, 173.615, 14, '{"AGILITY":30,"RELOAD":8,"STRENGTH":4,"HEALTH":4}', 'SOLDIER', 0),
	(0, 108, b'0', 0, 100, '2025-12-09 16:15:44.035604', 17, 3, 0, 0, 13, '{"AGILITY":4,"RELOAD":8,"STRENGTH":4,"HEALTH":4}', 'SOLDIER', 0),
	(0, 116, b'0', 0, 100, '2025-12-10 13:43:44.849646', 18, 5, 0, 0, 15, '{"AGILITY":4,"RELOAD":4,"STRENGTH":4,"HEALTH":8}', 'DOCTOR', 0),
	(0, 108, b'0', 0, 100, '2025-12-10 16:24:35.548153', 19, 6, 0, 0, 16, '{"AGILITY":4,"RELOAD":4,"STRENGTH":8,"HEALTH":4}', 'FIREFIGHTER', 0),
	(0, 108, b'0', 0, 100, '2025-12-16 16:36:54.760371', 20, 7, 877.409, 265.884, 17, '{"AGILITY":30,"RELOAD":4,"STRENGTH":4,"HEALTH":4}', 'REPORTER', 0),
	(0, 108, b'0', 0, 100, '2025-12-18 15:26:18.943095', 21, 8, 0, 0, 18, '{"AGILITY":8,"RELOAD":4,"STRENGTH":4,"HEALTH":4}', 'REPORTER', 0),
	(0, 108, b'0', 0, 100, '2025-12-24 15:10:58.935935', 22, 9, 0, 0, 19, '{"AGILITY":4,"RELOAD":20,"STRENGTH":4,"HEALTH":4}', 'SOLDIER', 0),
	(0, 115, b'0', 0, 100, '2025-12-24 15:12:03.296440', 23, 10, 899.304, 334.608, 20, '{"AGILITY":4,"RELOAD":4,"STRENGTH":4,"HEALTH":20}', 'DOCTOR', 0),
	(0, 108, b'0', 0, 100, '2025-12-24 15:12:31.463431', 24, 11, 0, 0, 21, '{"AGILITY":4,"RELOAD":4,"STRENGTH":20,"HEALTH":4}', 'FIREFIGHTER', 0),
	(0, 108, b'0', 0, 108, '2026-01-05 11:33:58.075479', 25, 12, 0, 0, 22, '{"AGILITY":4,"RELOAD":4,"STRENGTH":8,"HEALTH":4}', 'FIREFIGHTER', 0),
	(0, 108, b'0', 0, 108, '2026-01-05 16:04:36.660488', 26, 13, 0, 0, 23, '{"AGILITY":4,"RELOAD":4,"STRENGTH":8,"HEALTH":4}', 'FIREFIGHTER', 0);

-- 테이블 rr.friends 구조 내보내기
CREATE TABLE IF NOT EXISTS `friends` (
  `is_blocked` bit(1) DEFAULT NULL,
  `blocked_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `friend_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`friend_id`,`user_id`),
  KEY `fk_friends_user` (`user_id`),
  CONSTRAINT `fk_friends_friend` FOREIGN KEY (`friend_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_friends_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.friends:~24 rows (대략적) 내보내기
INSERT INTO `friends` (`is_blocked`, `blocked_at`, `created_at`, `friend_id`, `user_id`) VALUES
	(b'0', NULL, '2025-12-10 16:23:00.944249', 13, 14),
	(b'0', NULL, '2025-12-16 09:27:52.689637', 13, 15),
	(b'0', NULL, '2025-12-10 16:24:50.768589', 13, 16),
	(b'0', NULL, '2025-12-22 13:21:59.434074', 13, 17),
	(b'0', NULL, '2025-12-18 15:26:34.317739', 13, 18),
	(b'0', NULL, '2025-12-10 16:23:00.939084', 14, 13),
	(b'0', NULL, '2025-12-12 13:36:55.734377', 14, 15),
	(b'0', NULL, '2025-12-09 16:16:40.400780', 14, 16),
	(b'0', NULL, '2025-12-30 10:39:13.939734', 14, 17),
	(b'0', NULL, '2025-12-24 15:16:46.779037', 14, 19),
	(b'0', NULL, '2026-01-05 16:04:48.420809', 14, 23),
	(b'0', NULL, '2025-12-16 09:27:52.696662', 15, 13),
	(b'0', NULL, '2025-12-12 13:36:55.739703', 15, 14),
	(b'0', NULL, '2025-12-10 16:27:05.354716', 15, 16),
	(b'0', NULL, '2025-12-10 16:24:50.763850', 16, 13),
	(b'0', NULL, '2025-12-09 16:16:40.404779', 16, 14),
	(b'0', NULL, '2025-12-10 16:27:05.350658', 16, 15),
	(b'0', NULL, '2025-12-22 13:21:59.429774', 17, 13),
	(b'0', NULL, '2025-12-30 10:39:13.948897', 17, 14),
	(b'0', NULL, '2025-12-30 15:06:06.094151', 17, 20),
	(b'0', NULL, '2025-12-18 15:26:34.314743', 18, 13),
	(b'0', NULL, '2025-12-24 15:16:46.786258', 19, 14),
	(b'0', NULL, '2025-12-30 15:06:06.088551', 20, 17),
	(b'0', NULL, '2026-01-05 16:04:48.416764', 23, 14);

-- 테이블 rr.friend_requests 구조 내보내기
CREATE TABLE IF NOT EXISTS `friend_requests` (
  `created_at` datetime(6) NOT NULL,
  `receiver_id` bigint(20) NOT NULL,
  `request_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sender_id` bigint(20) NOT NULL,
  PRIMARY KEY (`request_id`),
  KEY `fk_fr_receiver` (`receiver_id`),
  KEY `fk_fr_sender` (`sender_id`),
  CONSTRAINT `fk_fr_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_fr_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.friend_requests:~0 rows (대략적) 내보내기
INSERT INTO `friend_requests` (`created_at`, `receiver_id`, `request_id`, `sender_id`) VALUES
	('2025-12-18 15:35:06.203624', 13, 31, 17);

-- 테이블 rr.group_members 구조 내보내기
CREATE TABLE IF NOT EXISTS `group_members` (
  `joined_at` datetime(6) NOT NULL,
  `map_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`map_id`,`user_id`),
  KEY `fk_gm_user` (`user_id`),
  CONSTRAINT `fk_gm_map` FOREIGN KEY (`map_id`) REFERENCES `maps` (`map_id`),
  CONSTRAINT `fk_gm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.group_members:~0 rows (대략적) 내보내기

-- 테이블 rr.maps 구조 내보내기
CREATE TABLE IF NOT EXISTS `maps` (
  `stage` int(11) DEFAULT NULL,
  `kill_count` int(11) NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `map_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nano_id` varchar(16) NOT NULL,
  `map_owner` bigint(20) NOT NULL,
  `map_name` varchar(64) NOT NULL,
  PRIMARY KEY (`map_id`),
  UNIQUE KEY `nano_id` (`nano_id`),
  KEY `fk_maps_owner_users` (`map_owner`),
  CONSTRAINT `fk_maps_owner_users` FOREIGN KEY (`map_owner`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.maps:~12 rows (대략적) 내보내기
INSERT INTO `maps` (`stage`, `kill_count`, `created_at`, `map_id`, `nano_id`, `map_owner`, `map_name`) VALUES
	(0, 0, '2025-12-09 15:58:52.006798', 3, 'A9a-lpQj_kWoAbdO', 13, 'test123의 맵'),
	(0, 0, '2025-12-09 15:59:31.864329', 4, '7OyZZPgdE0mnODWM', 14, '망구망구망구의 맵'),
	(0, 0, '2025-12-09 15:59:35.388557', 5, 'hMNOAdqGKtN9qReb', 15, 'abs123의 맵'),
	(0, 0, '2025-12-09 16:16:05.704576', 6, 'UVB0fCJMZV_OwKNl', 16, 'agumon의 맵'),
	(0, 0, '2025-12-16 16:36:42.027911', 7, 'Ldq1nGKSQzGuEmwy', 17, 'reporter의 맵'),
	(0, 0, '2025-12-18 15:25:51.495220', 8, 'm0ELznY_25w3rpJA', 18, 'faker의 맵'),
	(0, 0, '2025-12-24 15:10:51.459007', 9, 'AMw4AWoOdY9o7rDX', 19, 'soldier의 맵'),
	(0, 0, '2025-12-24 15:11:54.595342', 10, 'UnDPwh3NWTlExXc8', 20, 'docter의 맵'),
	(0, 0, '2025-12-24 15:12:25.398579', 11, 'VFRc2RS0Ho5n_YSB', 21, 'firefighter의 맵'),
	(0, 0, '2026-01-05 11:33:38.843868', 12, 'iYhrOyIPAKcV2ZvR', 22, 'hong의 맵'),
	(0, 0, '2026-01-05 16:04:24.486463', 13, 'UzCsDiGSP2mK9iLv', 23, '망구망구의 맵'),
	(0, 0, '2026-01-20 13:19:27.521753', 14, 'EQ_d0_4S7yNWsHCs', 24, '망구의 맵');

-- 테이블 rr.messages 구조 내보내기
CREATE TABLE IF NOT EXISTS `messages` (
  `group_id` bigint(20) NOT NULL,
  `message_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sent_at` datetime(6) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `content` text DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `fk_msg_map` (`group_id`),
  KEY `fk_msg_user` (`user_id`),
  CONSTRAINT `fk_msg_map` FOREIGN KEY (`group_id`) REFERENCES `maps` (`map_id`),
  CONSTRAINT `fk_msg_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.messages:~0 rows (대략적) 내보내기

-- 테이블 rr.monsters 구조 내보내기
CREATE TABLE IF NOT EXISTS `monsters` (
  `attack_speed` float DEFAULT NULL,
  `damage` float DEFAULT NULL,
  `hp` float DEFAULT NULL,
  `max_hp` float DEFAULT NULL,
  `pos_x` float NOT NULL DEFAULT 0,
  `pos_y` float NOT NULL DEFAULT 0,
  `range` float DEFAULT NULL,
  `speed` float DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `map_id` bigint(20) NOT NULL,
  `name` enum('BOMBER','FAST_RANGER','HEALTH','INVISIBLE','RANGER','RUNNER','SLOW') NOT NULL,
  `type` enum('BOSS','NAMED','NORMAL') NOT NULL,
  `exp` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_monster_map` (`map_id`),
  CONSTRAINT `fk_monster_map` FOREIGN KEY (`map_id`) REFERENCES `maps` (`map_id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.monsters:~12 rows (대략적) 내보내기
INSERT INTO `monsters` (`attack_speed`, `damage`, `hp`, `max_hp`, `pos_x`, `pos_y`, `range`, `speed`, `id`, `map_id`, `name`, `type`, `exp`) VALUES
	(1, 5, 120, 120, 915.5, 380.25, 50, 45, 37, 7, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1300, 800, 50, 45, 38, 7, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1500, 1200, 50, 45, 39, 7, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1300, 800, 50, 45, 40, 4, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1500, 1200, 50, 45, 41, 4, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 915.5, 380.25, 50, 45, 42, 4, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 915.5, 380.25, 50, 45, 43, 13, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1500, 1200, 50, 45, 44, 13, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1300, 800, 50, 45, 45, 13, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1500, 1200, 50, 45, 46, 10, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 915.5, 380.25, 50, 45, 47, 10, 'SLOW', 'NORMAL', 0),
	(1, 5, 120, 120, 1300, 800, 50, 45, 48, 10, 'SLOW', 'NORMAL', 0);

-- 테이블 rr.objects 구조 내보내기
CREATE TABLE IF NOT EXISTS `objects` (
  `height` float DEFAULT NULL,
  `is_break` bit(1) DEFAULT NULL,
  `pos_x` int(11) NOT NULL,
  `pos_y` int(11) NOT NULL,
  `width` float DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `map_id` bigint(20) NOT NULL,
  `object_name` enum('BOMB','BOX') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_object_map` (`map_id`),
  CONSTRAINT `fk_object_map` FOREIGN KEY (`map_id`) REFERENCES `maps` (`map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.objects:~0 rows (대략적) 내보내기

-- 테이블 rr.users 구조 내보내기
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(20) NOT NULL,
  `level` int(11) DEFAULT NULL,
  `registered_at` datetime(6) NOT NULL,
  `ip` varchar(45) NOT NULL,
  `key_config` text DEFAULT NULL COMMENT '유저 키 설정 JSON',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_nickname` (`nickname`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.users:~11 rows (대략적) 내보내기
INSERT INTO `users` (`user_id`, `password`, `nickname`, `level`, `registered_at`, `ip`, `key_config`) VALUES
	(13, '$2a$10$bXgldF5CeYQVLMpb7anwdey4R3cvGrhPAQjvHy2fgvNMwvrSZZw9K', 'test123', 0, '2025-12-09 15:58:51.971506', '192.168.0.48', NULL),
	(14, '$2a$10$qBqzW/cPxcC3ry5CMAal9OEvhfKx0fTuh1SH7uIeeDlXgi47mLmUi', '망구망구망구', 0, '2025-12-09 15:59:31.859631', '192.168.0.48', NULL),
	(15, '$2a$10$67Jn69YvnttZqkIXH7qZ1efI2GcESSMcHfHNYa1qa/wm2oK2bbgv6', 'abs123', 0, '2025-12-09 15:59:35.383812', '192.168.0.48', NULL),
	(16, '$2a$10$.77tXpUMWiwp9sBoG18kP.JYZCRGuysbGx5LFeGBXdjCUNPJLdW2S', 'agumon', 0, '2025-12-09 16:16:05.690917', '192.168.0.48', NULL),
	(17, '$2a$10$zKGyYPOBfT1c/teVR/Hzv.NBxz3F3d4YOeeiBi39GWW3aZ5Sj/deS', 'reporter', 0, '2025-12-16 16:36:41.984311', '192.168.0.48', '{"moveUp":"W","moveDown":"S","moveLeft":"A","moveRight":"D","fire":"LeftClick","aim":"RightClick","run":"Shift","reload":"R","chat":"Enter","skill":"Q","melee":"V"}'),
	(18, '$2a$10$I0qzhmaRZG/TWyG.szfv6.4Pktke.QJrm1dsCZIbB9.jKeBYYYexu', 'faker', 0, '2025-12-18 15:25:51.488154', '192.168.0.48', NULL),
	(19, '$2a$10$wf1TulZB8h7EZxWx4rMbeuWezBhpUSXZ/.TUQ0WSU6lhSBlLsopqC', 'soldier', 0, '2025-12-24 15:10:51.417465', '192.168.0.48', '{"moveUp":"W","moveDown":"S","moveLeft":"A","moveRight":"D","fire":"LeftClick","aim":"RightClick","run":"Shift","reload":"R","chat":"Enter","skill":"Q","melee":"V"}'),
	(20, '$2a$10$xz5n7hxRV33r8C7R7dDfV.9Jxl60aAvLmPJQ90/hHIkfKdXTHXEw6', 'docter', 0, '2025-12-24 15:11:54.583875', '192.168.0.48', NULL),
	(21, '$2a$10$e7rtNqziSiBfoaczpJtanue.LTbhKWDTLgF05LlH6an4bsahW5o0.', 'firefighter', 0, '2025-12-24 15:12:25.393643', '192.168.0.48', NULL),
	(22, '$2a$10$0.yPvH1P1mW4BroWE8qySuR58fi.kOtkjJ0z5ji2R/byUqomMDhd6', 'hong', 0, '2026-01-05 11:33:38.801476', '192.168.0.48', NULL),
	(23, '$2a$10$DBAmvGU8QS9OHSBYs2ha1.TcYBd.Fx.S78921X6qSR7FvpVxXiVrG', '망구망구', 0, '2026-01-05 16:04:24.477127', '192.168.0.48', NULL),
	(24, '$2a$10$XRathczBZqsdXsn.kjbN0.mx1djBVznzUDX9yff/QCIEq3hVRKtnu', '망구', 0, '2026-01-20 13:19:27.493632', '192.168.0.103', NULL);

-- 테이블 rr.weapons 구조 내보내기
CREATE TABLE IF NOT EXISTS `weapons` (
  `ammo` int(11) DEFAULT NULL,
  `attack_speed` float DEFAULT NULL,
  `damage` float DEFAULT NULL,
  `reload_speed` float DEFAULT NULL,
  `character_id` bigint(20) DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `object_id` bigint(20) DEFAULT NULL,
  `name` varchar(50) DEFAULT NULL,
  `rarity` enum('COMMON','EPIC','LEGENDARY','RARE') NOT NULL,
  `type` enum('GUN','MELEE') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_weapon_character` (`character_id`),
  KEY `fk_weapon_object` (`object_id`),
  CONSTRAINT `fk_weapon_character` FOREIGN KEY (`character_id`) REFERENCES `characters` (`id`),
  CONSTRAINT `fk_weapon_object` FOREIGN KEY (`object_id`) REFERENCES `objects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 테이블 데이터 rr.weapons:~0 rows (대략적) 내보내기

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
