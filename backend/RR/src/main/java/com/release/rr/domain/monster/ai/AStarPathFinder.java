package com.release.rr.domain.monster.ai;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AStarPathFinder {

    private static final int MAX_ITER = 6000;

    // 8방향 (프론트와 동일)
    private static final int[][] DIRS = {
            { 1, 0}, {-1, 0}, { 0, 1}, { 0,-1},
            { 1, 1}, { 1,-1}, {-1, 1}, {-1,-1}
    };

    private static class Node {
        int x, y;
        double g, h;
        Node parent;

        Node(int x, int y, double g, double h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        double f() {
            return g + h;
        }
    }

    /**
     * grid[y][x] == 1 → 장애물
     * 반환: (sx,sy)부터 (gx,gy)까지의 경로 (없으면 빈 리스트)
     */
    public List<int[]> find(int sx, int sy, int gx, int gy, int[][] grid) {

        if (grid == null || grid.length == 0) return List.of();

        int height = grid.length;
        int width = grid[0].length;

        if (!inBounds(sx, sy, width, height)
                || !inBounds(gx, gy, width, height)) return List.of();

        if (grid[sy][sx] == 1 || grid[gy][gx] == 1) return List.of();

        List<Node> open = new ArrayList<>();
        Map<String, Node> openMap = new HashMap<>();
        Set<String> closed = new HashSet<>();

        Node start = new Node(
                sx, sy,
                0,
                octileHeuristic(sx, sy, gx, gy),
                null
        );

        open.add(start);
        openMap.put(key(sx, sy), start);

        int iterations = 0;

        while (!open.isEmpty()) {
            if (++iterations > MAX_ITER) {
                return List.of();
            }

            // 프론트와 동일: 매 루프마다 정렬
            open.sort(Comparator.comparingDouble(Node::f));
            Node current = open.remove(0);
            openMap.remove(key(current.x, current.y));

            if (current.x == gx && current.y == gy) {
                return buildPath(current);
            }

            closed.add(key(current.x, current.y));

            for (int[] d : DIRS) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];
                String nKey = key(nx, ny);

                if (!inBounds(nx, ny, width, height)) continue;
                if (grid[ny][nx] == 1) continue;
                if (closed.contains(nKey)) continue;

                // ⭐ 대각선 코너 끼임 방지 (프론트와 동일)
                boolean diagonal = Math.abs(d[0]) + Math.abs(d[1]) == 2;
                if (diagonal) {
                    int side1 = grid[current.y][current.x + d[0]];
                    int side2 = grid[current.y + d[1]][current.x];

                    // 둘 다 벽이면만 차단
                    if (side1 == 1 && side2 == 1) {
                        continue;
                    }
                }


                double cost = diagonal ? 1.4 : 1.0;
                double g = current.g + cost;

                if (openMap.containsKey(nKey)) {
                    Node node = openMap.get(nKey);
                    if (g < node.g) {
                        node.g = g;
                        node.parent = current;
                    }
                    continue;
                }

                int dx = Math.abs(gx - nx);
                int dy = Math.abs(gy - ny);

                Node next = new Node(
                        nx, ny,
                        g,
                        Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy),
                        current
                );

                open.add(next);
                openMap.put(nKey, next);
            }
        }

        return List.of();
    }

    // =========================
    // Utils
    // =========================

    private boolean inBounds(int x, int y, int w, int h) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    private double octileHeuristic(int x, int y, int gx, int gy) {
        int dx = Math.abs(gx - x);
        int dy = Math.abs(gy - y);
        return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
    }

    private List<int[]> buildPath(Node node) {
        List<int[]> path = new ArrayList<>();
        while (node != null) {
            path.add(new int[]{node.x, node.y});
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private String key(int x, int y) {
        return x + "," + y;
    }
}
