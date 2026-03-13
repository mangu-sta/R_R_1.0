---
config:
  layout: elk
  theme: redux
---
erDiagram
    direction TB

    USERS {
        Long user_id PK ""
        String nickname ""
        String password ""
        LocalDateTime registered_at ""
        String ip ""
        Integer level ""
        Text key_config ""
    }

    CHARACTERS {
        Long id PK ""
        Long user_id FK ""
        Long map_id FK ""
        String job ""
        Integer level ""
        Integer exp ""
        Float pos_x ""
        Float pos_y ""
        Float hp ""
        Float max_hp ""
        LongText status ""
        Float angle ""
        LocalDateTime created_at ""
        Boolean is_end ""
    }

    MAPS {
        Long map_id PK ""
        String nano_id UK ""
        Long map_owner FK ""
        String map_name ""
        LocalDateTime created_at ""
        Integer stage ""
        Integer kill_count ""
    }

    MONSTERS {
        Long id PK ""
        Long map_id FK ""
        String type ""
        String name ""
        Float pos_x ""
        Float pos_y ""
        Float hp ""
        Float max_hp ""
        Float damage ""
        Float attack_speed ""
        Float speed ""
        Float range ""
        Integer exp ""
    }

    WEAPONS {
        Long id PK ""
        Long object_id FK ""
        Long character_id FK ""
        String name ""
        String rarity ""
        Float damage ""
        Float attack_speed ""
        Float reload_speed ""
        String type ""
        Integer ammo ""
    }

    OBJECTS {
        Long id PK ""
        Long map_id FK ""
        String object_name ""
        Integer pos_x ""
        Integer pos_y ""
        Float width ""
        Float height ""
        Boolean is_break ""
    }

    FRIENDS {
        Long user_id PK, FK ""
        Long friend_id PK, FK ""
        LocalDateTime created_at ""
        Boolean is_blocked ""
        LocalDateTime blocked_at ""
    }

    GROUP_MEMBERS {
        Long map_id PK, FK ""
        Long user_id PK, FK ""
        LocalDateTime joined_at ""
    }

    MESSAGES {
        Long message_id PK ""
        Long group_id FK ""
        Long user_id FK ""
        Text content ""
        LocalDateTime sent_at ""
    }

    BOSS_CLEAR_RECORD {
        Long id PK ""
        String nano_id ""
        String nickname ""
        Long time_taken_seconds ""
        LocalDateTime cleared_at ""
    }

    USERS ||--o{ CHARACTERS : "owns"
    USERS ||--o{ MAPS : "owns"
    USERS ||--o{ FRIENDS : "acts as user"
    USERS ||--o{ FRIENDS : "acts as friend"
    USERS ||--o{ GROUP_MEMBERS : "joined"
    USERS ||--o{ MESSAGES : "sent"
    MAPS ||--o{ CHARACTERS : "contains"
    MAPS ||--o{ MONSTERS : "spawned in"
    MAPS ||--o{ OBJECTS : "has"
    MAPS ||--o{ GROUP_MEMBERS : "has members"
    MAPS ||--o{ MESSAGES : "has"
    CHARACTERS ||--o{ WEAPONS : "holds"
    OBJECTS ||--o{ WEAPONS : "base for"
