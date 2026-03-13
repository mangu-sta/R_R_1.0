package com.release.rr.domain.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.release.rr.domain.map.model.ObstacleRect;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Component
public class ObstacleProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 맵 기준 obstacles.json, obstacles2.json
     *
     */
    public List<ObstacleRect> loadForMap(String nanoId, int stage) {
        String path = (stage == 1)
                ? "/map/obstacles2.json"
                : "/map/obstacles.json";

        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException(path + " not found in resources/map");
            ObstacleRect[] arr = objectMapper.readValue(is, ObstacleRect[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + path, e);
        }
    }

}
