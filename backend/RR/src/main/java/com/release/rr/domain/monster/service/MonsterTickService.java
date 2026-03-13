package com.release.rr.domain.monster.service;

import com.release.rr.domain.monster.entity.MonsterEntity;
import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.domain.map.model.MapGrid;
import com.release.rr.domain.map.service.GridService;
import com.release.rr.domain.map.service.MapStageProvider;
import com.release.rr.domain.monster.ai.AStarPathFinder;
import com.release.rr.domain.monster.ai.LineOfSightService;
import com.release.rr.domain.monster.ai.MonsterAiService;
import com.release.rr.domain.monster.ai.MonsterAiState;
import com.release.rr.domain.monster.dto.MonsterStateItem;
import com.release.rr.domain.monster.dto.MonsterStateMessage;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import com.release.rr.global.redis.dto.MonsterStateDto;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.release.rr.domain.combat.service.PlayerDamageService;
import com.release.rr.domain.combat.dto.res.PlayerHpUpdateEvent;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonsterTickService {

    /*
     * ===============================
     * Dependencies
     * ===============================
     */
    private final MonsterStateRedisDao monsterRedis;
    private final CharacterStateRedisDao characterRedis;

    private final GridService gridService;
    private final AStarPathFinder aStar;
    private final LineOfSightService los;
    private final MonsterAiService monsterAiService;

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisGameRoomDao redisGameRoomDao;

    private final PlayerDamageService playerDamageService;
    private final MapStageProvider mapStageProvider;

    /*
     * ===============================
     * Tick Constants (프론트와 동일)
     * ===============================
     */
    // private static final float SPEED = 50f; // px/sec
    private static final float TICK_SEC = 0.03f; // 30ms (Scheduled 주기와 동기화)
    private static final float ARRIVE_DIST = 10f; // px
    private static final long PATH_RECALC_MS = 150; // 경로 재계산 주기
    // LOS 직선 추격 허용 최대 거리
    private static final float LOS_CHASE_DIST = 120f;

    // ===== Attack Constants =====
    private static final long ATTACK_COOLDOWN_MS = 1000; // 1초
    // private static final float ATTACK_RANGE = 50f; // px (지금 네 ATTACK 판정 거리와 맞추면
    // 됨)
    // private static final float ATTACK_DAMAGE = 1f; // 임시 고정 데미지

    /*
     * ===============================
     * Main Tick
     * ===============================
     */
    @Scheduled(fixedDelay = 20)
    public void tick() {
        try {
            List<String> nanoIds = redisGameRoomDao.findActiveNanoIds();
            if (nanoIds.isEmpty())
                return;

            // System.out.println("[MonsterTickService] TICK START - Active Rooms: " +
            // nanoIds.size());
            for (String nanoId : nanoIds) {
                tickMap(nanoId);
            }
        } catch (Exception e) {
            System.err.println("[MonsterTickService] CRITICAL ERROR in tick loop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void tickMap(String nanoId) {
        // 현재 맵의 몬스터 / 플레이어 상태 조회
        List<MonsterStateDto> monsters = monsterRedis.findAllByMap(nanoId);
        List<CharacterStateDto> players = characterRedis.findAllByMap(nanoId);

        if (players.isEmpty()) {
            // Only log if there are monsters but no connected players
            if (!monsters.isEmpty()) {
                // System.out.println("[MonsterTickService] SKIPPING nanoId=" + nanoId + " - No
                // connected players found (Monsters: " + monsters.size() + ")");
            }
            return;
        }

        // System.out.println("[MonsterTickService] tickMap nanoId=" + nanoId + ",
        // monsters=" + monsters.size() + ", players=" + players.size());
        // if (!players.isEmpty() && System.currentTimeMillis() % 1000 < 50) {
        // for (CharacterStateDto p : players) {
        // System.out.println("[MonsterTickService] ACTIVE PLAYER: userId=" +
        // p.getUserId() +
        // ", characterId=" + p.getCharacterId() + " (x=" + p.getX() + ", y=" + p.getY()
        // + ")");
        // }
        // }

        // broadcast(nanoId, monsters); // Already called at the end of the method

        // System.out.println("✅ tickMap MAIN LOGIC ENTER");

        int stage = mapStageProvider.getStage(nanoId);
        MapGrid mapGrid = gridService.getOrBuild(nanoId, stage);

        long now = System.currentTimeMillis();

        for (MonsterStateDto monster : monsters) {
            if (!monster.isAlive())
                continue;
            /*
             * -------------------------------
             * 1️⃣ AI 타겟 유지 / 변경
             * -------------------------------
             */
            monsterAiService.updateTarget(monster, players, stage);

            if (monster.getTargetCharacterId() == null) {
                monsterRedis.save(monster.getMonsterId(), monster);
                continue;
            }

            CharacterStateDto target = characterRedis.getState(monster.getTargetCharacterId());

            if (target == null)
                continue;

            /*
             * -------------------------------
             * 2️⃣ 상태 전이 (CHASE / ATTACK)
             * -------------------------------
             */
            monsterAiService.updateState(monster, target);

            /*
             * -------------------------------
             * 3️⃣ 이동/공격 처리
             * -------------------------------
             */
            if (MonsterEntity.MonsterType.BOSS.name().equals(monster.getType())) {
                processBossAi(monster, target, nanoId, now);
            } else if (MonsterAiState.CHASE.name().equals(monster.getState())) {
                move(monster, target, mapGrid, now);
            } else if (MonsterAiState.ATTACK.name().equals(monster.getState())) {
                tryAttack(monster, target, nanoId, now);
            }

            // ❗ 타겟 좌표 디버그 로그 (몬스터가 엉뚱한 곳으로 가는지 확인)
            if (now % 1000 < 30) { // 1초에 한 번만 출력
                System.out.println("[MonsterTickService] nanoId=" + nanoId +
                        ", monsterId=" + monster.getMonsterId() +
                        " -> TARGET characterId=" + target.getCharacterId() +
                        " (x=" + target.getX() + ", y=" + target.getY() + ")" +
                        ", state=" + monster.getState());
            }

            // ⭐ 타겟 유저 ID 전달용 임시 저장 (브로드캐스트 시 사용)
            monster.setTargetUserId(target.getUserId());

            // ❗ HP 덮어쓰기 방지를 위해 부분 업데이트 호출
            monsterRedis.updateAiState(monster.getMonsterId(), monster);
        }
        // System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<TICK>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        broadcast(nanoId, monsters);

    }

    /*
     * ===============================
     * Movement Logic
     * ===============================
     */

    private void move(
            MonsterStateDto monster,
            CharacterStateDto target,
            MapGrid mapGrid,
            long now) {

        // LOS 체크 → 직선 추적 가능 여부
        boolean hasLOS = los.hasLineOfSight(
                monster.getX(), monster.getY(),
                target.getX(), target.getY(),
                mapGrid.getObstacles());

        // ⭐ 몬스터 ↔ 플레이어 거리 계산
        float dx = target.getX() - monster.getX();
        float dy = target.getY() - monster.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // ⭐ 가까우면 직선, 멀거나 LOS 끊기면 A*
        if (hasLOS
                && dist <= LOS_CHASE_DIST
                && canGoDirect(monster, target, mapGrid)) {

            monster.setPath(null);
            monster.setPathIndex(0);
            moveStraight(monster, target.getX(), target.getY());
            return;
        }

        // 여기로 오면 "직선 추격 불가" → 무조건 A*
        moveWithPath(monster, target, mapGrid, now);

        /*
         * System.out.println(
         * "[MOVE] hasLOS=" + hasLOS +
         * " dist=" + dist +
         * " state=" + monster.getState()
         * );
         */

    }

    private void moveStraight(MonsterStateDto m, float tx, float ty) {
        float dx = tx - m.getX();
        float dy = ty - m.getY();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001f)
            return;

        float speed = m.getSpeed(); // ✅ 몬스터 개별 스탯

        m.setX(m.getX() + (dx / len) * speed * TICK_SEC);
        m.setY(m.getY() + (dy / len) * speed * TICK_SEC);
    }

    private void moveWithPath(

            MonsterStateDto m,
            CharacterStateDto target,
            MapGrid mapGrid,
            long now) {

        // 경로 재계산
        int gx = mapGrid.toTileX(target.getX());
        int gy = mapGrid.toTileY(target.getY());
        int sx = mapGrid.toTileX(m.getX());
        int sy = mapGrid.toTileY(m.getY());

        int[][] grid = mapGrid.getGrid();
        int h = grid.length;
        int w = grid[0].length;

        if (sx < 0 || sy < 0 || sx >= w || sy >= h
                || gx < 0 || gy < 0 || gx >= w || gy >= h) {

            System.out.println(
                    "[A* OOB] nanoId=" + mapGrid.getNanoId() +
                            " sx=" + sx + " sy=" + sy +
                            " gx=" + gx + " gy=" + gy +
                            " grid=" + w + "x" + h +
                            " m=(" + m.getX() + "," + m.getY() + ")" +
                            " t=(" + target.getX() + "," + target.getY() + ")");

            m.setPath(null);
            m.setPathIndex(0);
            m.setNextPathTime(now + 200);
            return;
        }

        // ⬇️ 이제부터 안전하게 접근 가능
        if (grid[sy][sx] == 1) {
            int[] altS = findNearestWalkableTile(sx, sy, grid);
            if (altS == null)
                return;
            sx = altS[0];
            sy = altS[1];
        }

        if (grid[gy][gx] == 1) {
            int[] alt = findNearestWalkableTile(gx, gy, grid);
            if (alt == null)
                return;
            gx = alt[0];
            gy = alt[1];
        }

        if (mapGrid.getGrid()[sy][sx] == 1) {
            int[] altS = findNearestWalkableTile(sx, sy, mapGrid.getGrid());
            if (altS == null)
                return;
            sx = altS[0];
            sy = altS[1];
        }

        // ⭐ 목표 타일이 벽이면 주변 통로로 스냅
        if (mapGrid.getGrid()[gy][gx] == 1) {
            int[] alt = findNearestWalkableTile(gx, gy, mapGrid.getGrid());
            if (alt == null)
                return;

            gx = alt[0];
            gy = alt[1];
        }

        boolean targetTileChanged = gx != m.getLastTargetTileX()
                || gy != m.getLastTargetTileY();

        if (m.getPath() == null
                || now >= m.getNextPathTime()
                || targetTileChanged) {

            // int sx = mapGrid.toTileX(m.getX());
            // int sy = mapGrid.toTileY(m.getY());

            List<int[]> path = aStar.find(
                    sx, sy,
                    gx, gy,
                    mapGrid.getGrid());

            if (path.isEmpty()) {
                System.out.println(
                        "❌ A* PATH EMPTY " +
                                "sx=" + sx + ", sy=" + sy +
                                " gx=" + gx + ", gy=" + gy);
                m.setNextPathTime(now + 50);
                return;
            }

            // 시작 노드 제거 (자기 자신)
            if (path.size() > 1) {
                path = path.subList(1, path.size());
            }

            m.setPath(encodePath(path));
            m.setPathIndex(0);

            m.setNextPathTime(now + PATH_RECALC_MS);

            // ⭐ 타겟 타일 기억
            m.setLastTargetTileX(gx);
            m.setLastTargetTileY(gy);
        }

        // 현재 노드로 이동
        List<int[]> nodes = decodePath(m.getPath());
        if (m.getPathIndex() >= nodes.size())
            return;

        int[] node = nodes.get(m.getPathIndex());
        float tx = mapGrid.toWorldCenterX(node[0]);
        float ty = mapGrid.toWorldCenterY(node[1]);

        float dx = tx - m.getX();
        float dy = ty - m.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < ARRIVE_DIST) {
            m.setPathIndex(m.getPathIndex() + 1);
            return;
        }

        float speed = m.getSpeed();

        m.setX(m.getX() + (dx / dist) * speed * TICK_SEC);
        m.setY(m.getY() + (dy / dist) * speed * TICK_SEC);

        float moved = Math.abs(m.getX() - m.getLastX()) +
                Math.abs(m.getY() - m.getLastY());

        if (moved < 0.1f) {
            if (m.getStuckSince() == 0) {
                m.setStuckSince(now);
            } else if (now - m.getStuckSince() > 300) {
                // 300ms 이상 안 움직였으면 경로 폐기
                m.setPath(null);
                m.setPathIndex(0);
                m.setStuckSince(0);
                return;
            }
        } else {
            m.setStuckSince(0);
        }

        m.setLastX(m.getX());
        m.setLastY(m.getY());

    }

    private int[] findNearestWalkableTile(int gx, int gy, int[][] grid) {
        int h = grid.length;
        int w = grid[0].length;

        int radius = 1;
        while (radius <= 3) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = gx + dx;
                    int ny = gy + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h)
                        continue;
                    if (grid[ny][nx] == 0) {
                        return new int[] { nx, ny };
                    }
                }
            }
            radius++;
        }
        return null;
    }

    /*
     * ===============================
     * Utils
     * ===============================
     */

    private String encodePath(List<int[]> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i)[0]).append(",").append(path.get(i)[1]);
            if (i < path.size() - 1)
                sb.append(";");
        }
        return sb.toString();
    }

    private List<int[]> decodePath(String path) {
        List<int[]> list = new ArrayList<>();
        if (path == null || path.isEmpty())
            return list;

        for (String p : path.split(";")) {
            String[] xy = p.split(",");
            list.add(new int[] {
                    Integer.parseInt(xy[0]),
                    Integer.parseInt(xy[1])
            });
        }
        return list;
    }

    /*
     * ===============================
     * Broadcast
     * ===============================
     */

    private void broadcast(String nanoId, List<MonsterStateDto> monsters) {
        if (monsters.isEmpty())
            return; // Don't broadcast empty lists for now to reduce noise

        List<MonsterStateItem> items = monsters.stream()
                .map(m -> MonsterStateItem.builder()
                        .id(m.getMonsterId())
                        .type(m.getType())
                        .name(m.getName())
                        .x(m.getX())
                        .y(m.getY())
                        .hp(m.getHp())
                        .maxHp(m.getMaxHp())
                        .state(m.getState())
                        .targetUserId(m.getTargetUserId())
                        .patternType(m.getPatternType())
                        .patternState(m.getPatternState())
                        .telegraphX(m.getTelegraphX())
                        .telegraphY(m.getTelegraphY())
                        .build())
                .toList();

        // System.out.println("[MonsterTickService] BROADCAST " + items.size() + "
        // monsters to /topic/game/" + nanoId);
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                MonsterStateMessage.builder()
                        .type(GameEventType.MONSTER_STATE)
                        .monsters(items)
                        .timestamp(System.currentTimeMillis())
                        .build());
    }

    private boolean canGoDirect(MonsterStateDto m, CharacterStateDto t, MapGrid mapGrid) {
        int x0 = mapGrid.toTileX(m.getX());
        int y0 = mapGrid.toTileY(m.getY());
        int x1 = mapGrid.toTileX(t.getX());
        int y1 = mapGrid.toTileY(t.getY());

        int[][] grid = mapGrid.getGrid();
        int w = grid[0].length;
        int h = grid.length;

        // Bresenham (타일 직선 스캔)
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            if (x < 0 || y < 0 || x >= w || y >= h)
                return false;
            if (grid[y][x] == 1)
                return false; // 벽 있으면 직선 불가

            if (x == x1 && y == y1)
                break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }

        return true;
    }

    private void tryAttack(
            MonsterStateDto monster,
            CharacterStateDto target,
            String nanoId,
            long now) {
        // 1) 쿨타임 검사
        if (now - monster.getLastAttackTime() < ATTACK_COOLDOWN_MS) {
            return;
        }

        // 2) 거리 검사 (서버 기준)
        float dx = monster.getX() - target.getX();
        float dy = monster.getY() - target.getY();
        float dist2 = dx * dx + dy * dy;

        float range = monster.getRange();

        if (dist2 > range * range) {
            // 거리 벗어났으면 공격 안 함 (상태 전환은 updateState가 담당)
            return;
        }

        // 3) 데미지 적용 (Redis 캐릭터 HP 감소 + dead 판단)
        float damage = monster.getDamage(); // ✅ 몬스터 스탯

        PlayerHpUpdateEvent event = playerDamageService.applyPlayerDamage(
                nanoId,
                target.getCharacterId(),
                damage);

        // 4) 공격 시간 갱신 (중복 공격 방지)
        monster.setLastAttackTime(now);

        // 5) 브로드캐스트
        if (event != null) {
            messagingTemplate.convertAndSend(
                    "/topic/game/" + nanoId,
                    event // ⭐ 그대로 보냄
            );
        }

    }

    private void processBossAi(MonsterStateDto boss, CharacterStateDto target, String nanoId, long now) {
        String pState = boss.getPatternState();
        if (pState == null || "NONE".equals(pState)) {
            // 패턴 시작할지 결정 (CHASE 또는 ATTACK 중일 때 3초마다)
            if (now - boss.getPatternStartTime() > 4000) {
                startRandomPattern(boss, target, now);
            } else {
                // 패턴 대기 중에는 일반 AI (CHASE/ATTACK)
                // 보스용 일반 이동/공격 로직
                float dx = target.getX() - boss.getX();
                float dy = target.getY() - boss.getY();
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > boss.getRange()) {
                    moveStraight(boss, target.getX(), target.getY());
                } else {
                    tryAttack(boss, target, nanoId, now);
                }
            }
            return;
        }

        // 패턴 진행
        switch (boss.getPatternType()) {
            case "DASH" -> tickDashPattern(boss, target, nanoId, now);
            case "SWING" -> tickSwingPattern(boss, target, nanoId, now);
            case "SLAM" -> tickSlamPattern(boss, target, nanoId, now);
            default -> {
                boss.setPatternState("NONE");
                boss.setPatternStartTime(now);
            }
        }
    }

    private void startRandomPattern(MonsterStateDto boss, CharacterStateDto target, long now) {
        String[] patterns = { "DASH", "SWING", "SLAM" };
        String chosen = patterns[(int) (Math.random() * patterns.length)];

        boss.setPatternType(chosen);
        boss.setPatternState("TELEGRAPH");
        boss.setPatternStartTime(now);
        boss.setState(MonsterAiState.PATTERN.name());

        // 전조 위치 결정
        boss.setTelegraphX(target.getX());
        boss.setTelegraphY(target.getY());

        System.out.println("👹 BOSS START PATTERN: " + chosen);
    }

    private void tickDashPattern(MonsterStateDto boss, CharacterStateDto target, String nanoId, long now) {
        long elapsed = now - boss.getPatternStartTime();
        if ("TELEGRAPH".equals(boss.getPatternState())) {
            if (elapsed > 1500) { // 1.5초 전조
                boss.setPatternState("ACTION");
                boss.setPatternStartTime(now);
            }
        } else if ("ACTION".equals(boss.getPatternState())) {
            float dx = boss.getTelegraphX() - boss.getX();
            float dy = boss.getTelegraphY() - boss.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < 20f || elapsed > 1000) {
                boss.setPatternState("NONE");
                boss.setPatternStartTime(now);
                boss.setState(MonsterAiState.CHASE.name());
            } else {
                float dashSpeed = boss.getSpeed() * 8f;
                boss.setX(boss.getX() + (dx / dist) * dashSpeed * TICK_SEC);
                boss.setY(boss.getY() + (dy / dist) * dashSpeed * TICK_SEC);

                checkAoeDamage(boss, nanoId, 60f, boss.getDamage() * 1.5f);
            }
        }
    }

    private void tickSwingPattern(MonsterStateDto boss, CharacterStateDto target, String nanoId, long now) {
        long elapsed = now - boss.getPatternStartTime();
        if ("TELEGRAPH".equals(boss.getPatternState())) {
            if (elapsed > 1000) {
                boss.setPatternState("ACTION");
                boss.setPatternStartTime(now);
            }
        } else if ("ACTION".equals(boss.getPatternState())) {
            checkAoeDamage(boss, nanoId, 180f, boss.getDamage() * 2.0f);

            if (elapsed > 500) {
                boss.setPatternState("NONE");
                boss.setPatternStartTime(now);
                boss.setState(MonsterAiState.CHASE.name());
            }
        }
    }

    private void tickSlamPattern(MonsterStateDto boss, CharacterStateDto target, String nanoId, long now) {
        long elapsed = now - boss.getPatternStartTime();
        if ("TELEGRAPH".equals(boss.getPatternState())) {
            if (elapsed > 1500) {
                boss.setPatternState("ACTION"); // JUMP
                boss.setPatternStartTime(now);
            }
        } else if ("ACTION".equals(boss.getPatternState())) {
            if (elapsed < 800) {
                // 점프 중 효과
            } else {
                boss.setX(boss.getTelegraphX());
                boss.setY(boss.getTelegraphY());
                checkAoeDamage(boss, nanoId, 250f, boss.getDamage() * 3.0f);

                boss.setPatternState("NONE");
                boss.setPatternStartTime(now);
                boss.setState(MonsterAiState.CHASE.name());
            }
        }
    }

    private void checkAoeDamage(MonsterStateDto boss, String nanoId, float radius, float damage) {
        List<CharacterStateDto> players = characterRedis.findAllByMap(nanoId);
        for (CharacterStateDto p : players) {
            if (!p.isAlive())
                continue;
            float dx = p.getX() - boss.getX();
            float dy = p.getY() - boss.getY();
            if (dx * dx + dy * dy <= radius * radius) {
                PlayerHpUpdateEvent event = playerDamageService.applyPlayerDamage(nanoId, p.getCharacterId(), damage);
                if (event != null) {
                    messagingTemplate.convertAndSend("/topic/game/" + nanoId, event);
                }
            }
        }
    }
}
