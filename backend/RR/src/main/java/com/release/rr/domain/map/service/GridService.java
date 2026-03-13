package com.release.rr.domain.map.service;

import com.release.rr.domain.map.model.MapGrid;
import com.release.rr.domain.map.model.ObstacleRect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GridService {

    private final ObstacleProvider obstacleProvider;
    private static final int WALL_SHRINK_PX = 8;
    // nanoId별 캐시 (맵이 바뀌면 clear하면 됨)
    private final ConcurrentHashMap<String, MapGrid> cache = new ConcurrentHashMap<>();


    /**
     * 타일 크기는 프론트 TILE_SIZE와 반드시 동일해야 A* 결과가 같아짐.
     * 지금은 상수로 두고, 나중에 MapEntity에 넣어도 됨.
     */
    private static final int TILE_SIZE = 32;
    private static final int WORLD_WIDTH_PX  = 2368;
    private static final int WORLD_HEIGHT_PX = 1728;

    /**
     * 월드 크기(px)
     */
    public MapGrid getOrBuild(String nanoId, int stage) {
        return cache.computeIfAbsent(
                cacheKey(nanoId, stage),
                k -> buildGrid(nanoId, stage)
        );
    }


    public void invalidate(String nanoId) {
        cache.keySet().removeIf(key -> key.startsWith(nanoId + ":"));
    }


    private MapGrid buildGrid(String nanoId, int stage) {

        List<ObstacleRect> obstacles = obstacleProvider.loadForMap(nanoId, stage);

        int worldWidthPx;
        int worldHeightPx;

        if (stage == 1) {
            // 본게임
            worldWidthPx  = 4736;
            worldHeightPx = 3456;
        } else {
            // 튜토리얼
            worldWidthPx  = 2368;
            worldHeightPx = 1728;
        }

        int gridWidth  = worldWidthPx  / TILE_SIZE;
        int gridHeight = worldHeightPx / TILE_SIZE;


        System.out.println(
                "[GRID BUILD] nanoId=" + nanoId +
                        " world=" + worldWidthPx + "x" + worldHeightPx +
                        " grid=" + gridWidth + "x" + gridHeight +
                        " tileSize=" + TILE_SIZE
        );


        int[][] grid = new int[gridHeight][gridWidth];

        // 벽 마킹 (center 기준 버전 사용!)
        for (ObstacleRect r : obstacles) {
            markRectAsWall(grid, r, TILE_SIZE);
        }

        return new MapGrid(
                nanoId,
                TILE_SIZE,
                gridWidth,
                gridHeight,
                worldWidthPx,
                worldHeightPx,
                grid,
                obstacles
        );
    }

    /*

    */
    private static final int AGENT_RADIUS_PX = TILE_SIZE / 4;

    private void markRectAsWall(int[][] grid, ObstacleRect r, int tileSize) {
        int gridH = grid.length;
        int gridW = grid[0].length;

        int startX = Math.max(0, (int)Math.floor(r.x() / tileSize));
        int endX   = Math.min(gridW - 1,
                (int)Math.floor((r.x() + r.width()) / tileSize));

        int startY = Math.max(0, (int)Math.floor(r.y() / tileSize));
        int endY   = Math.min(gridH - 1,
                (int)Math.floor((r.y() + r.height()) / tileSize));

        for (int ty = startY; ty <= endY; ty++) {
            for (int tx = startX; tx <= endX; tx++) {
                grid[ty][tx] = 1;
            }
        }
    }


    private String cacheKey(String nanoId, int stage) {
        return nanoId + ":" + stage;
    }

}
