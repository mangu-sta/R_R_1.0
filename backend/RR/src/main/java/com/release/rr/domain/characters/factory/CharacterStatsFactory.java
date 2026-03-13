package com.release.rr.domain.characters.factory;

import com.release.rr.domain.characters.entity.CharacterEntity;

import java.util.HashMap;
import java.util.Map;

public class CharacterStatsFactory {

    public static String defaultStats(CharacterEntity.Job job) {

        Map<String, Integer> stats = new HashMap<>();

        switch (job) {
            case FIREFIGHTER:
                stats.put("STRENGTH", 8);
                stats.put("AGILITY", 4);
                stats.put("HEALTH", 4);
                stats.put("RELOAD", 4);
                break;

            case SOLDIER:
                stats.put("STRENGTH", 4);
                stats.put("AGILITY", 4);
                stats.put("HEALTH", 4);
                stats.put("RELOAD", 8);
                break;

            case DOCTOR:
                stats.put("STRENGTH", 4);
                stats.put("AGILITY", 4);
                stats.put("HEALTH", 8);
                stats.put("RELOAD", 4);
                break;

            case REPORTER:
                stats.put("STRENGTH", 4);
                stats.put("AGILITY", 8);
                stats.put("HEALTH", 4);
                stats.put("RELOAD", 4);
                break;
        }

        return toJson(stats);
    }

    private static String toJson(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder("{");

        map.forEach((k, v) -> sb.append("\"").append(k).append("\":").append(v).append(","));

        sb.deleteCharAt(sb.length() - 1); // 마지막 , 제거
        sb.append("}");
        return sb.toString();
    }
}
