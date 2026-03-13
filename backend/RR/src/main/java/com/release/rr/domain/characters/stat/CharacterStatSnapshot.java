package com.release.rr.domain.characters.stat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CharacterStatSnapshot {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int strength;
    private final int agility;
    private final int health;
    private final int reload;
    private final int pendingPoints;

    public static CharacterStatSnapshot fromStatusJson(String statusJson) {
        if (statusJson == null || statusJson.isBlank()) {
            return new CharacterStatSnapshot(0, 0, 0, 0, 0);
        }

        try {
            JsonNode node = MAPPER.readTree(statusJson);

            return new CharacterStatSnapshot(
                    node.path("STRENGTH").asInt(0),
                    node.path("AGILITY").asInt(0),
                    node.path("HEALTH").asInt(0),
                    node.path("RELOAD").asInt(0),
                    node.path("PENDING_POINTS").asInt(0)
            );
        } catch (Exception e) {
            // JSON 깨져도 서버 안 죽게
            return new CharacterStatSnapshot(0, 0, 0, 0, 0);
        }
    }

    // ✅ 파생값 (서버는 maxHp/attack만 계산한다 했으니 이것만 써도 됨)
    public float getMaxHp() {
        return 100f + (health * 2f);
    }

    public float getAttack() {
        return 20f + (strength * 2f);
    }
}
