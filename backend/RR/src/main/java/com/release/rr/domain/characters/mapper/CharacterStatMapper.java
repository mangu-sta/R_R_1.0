package com.release.rr.domain.characters.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release.rr.domain.characters.stat.CharacterStatSnapshot;

public class CharacterStatMapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static CharacterStatSnapshot fromStatusJson(String status) {
        if (status == null || status.isBlank()) {
            return new CharacterStatSnapshot(0, 0, 0, 0, 0);
        }
        try {
            JsonNode node = mapper.readTree(status);

            return new CharacterStatSnapshot(
                    node.path("STRENGTH").asInt(0),
                    node.path("AGILITY").asInt(0),
                    node.path("HEALTH").asInt(0),
                    node.path("RELOAD").asInt(0),
                    node.path("PENDING_POINTS").asInt(0)
            );

        } catch (Exception e) {
            // JSON 깨졌을 때 안전 장치
            return new CharacterStatSnapshot(0, 0, 0, 0, 0);
        }
    }

    public static String toStatusJson(int str, int agi, int heal, int reload, int pending) {
        return String.format(
            "{\"STRENGTH\":%d,\"AGILITY\":%d,\"HEALTH\":%d,\"RELOAD\":%d,\"PENDING_POINTS\":%d}",
            str, agi, heal, reload, pending
        );
    }
}
