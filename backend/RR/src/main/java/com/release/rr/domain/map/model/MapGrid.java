package com.release.rr.domain.map.model;

import lombok.Getter;

import java.util.List;

@Getter
public class MapGrid {

    private final String nanoId;

    // 타일 단위
    private final int tileSize;

    // grid 크기(타일 기준)
    private final int gridWidth;
    private final int gridHeight;

    // 월드 크기(px 기준)
    private final int worldWidthPx;
    private final int worldHeightPx;

    // 0=통과, 1=벽
    private final int[][] grid;

    // LOS 용 벽 사각형들(원본)
    private final List<ObstacleRect> obstacles;

    public MapGrid(
            String nanoId,
            int tileSize,
            int gridWidth,
            int gridHeight,
            int worldWidthPx,
            int worldHeightPx,
            int[][] grid,
            List<ObstacleRect> obstacles
    ) {
        this.nanoId = nanoId;
        this.tileSize = tileSize;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.worldWidthPx = worldWidthPx;
        this.worldHeightPx = worldHeightPx;
        this.grid = grid;
        this.obstacles = obstacles;
    }

    /** px -> tile */
    public int toTileX(float x) { return (int) Math.floor(x / tileSize); }
    public int toTileY(float y) { return (int) Math.floor(y / tileSize); }

    /** tile -> px(center) */
    public float toWorldCenterX(int tx) { return tx * tileSize + tileSize / 2f; }
    public float toWorldCenterY(int ty) { return ty * tileSize + tileSize / 2f; }
}
