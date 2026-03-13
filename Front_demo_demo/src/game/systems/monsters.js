import Phaser from "phaser";

export default class MonstersSystem {
    constructor(scene) {
        this.scene = scene;
        this.monsters = null;
        this.monstersMap = new Map();
        this.telegraphGraphics = null;
    }

    create() {
        this.monsters = this.scene.physics.add.group();
        this.monstersMap = new Map();
        this.telegraphGraphics = this.scene.add.graphics();
        this.telegraphGraphics.setDepth(5); // 몬스터 아래, 바닥 위

        // 충돌 설정 (Colliders)
        // 플레이어 vs 몬스터
        this.scene.physics.add.collider(
            this.scene.player,
            this.monsters,
            (player, monster) => {
                player.hp -= 5;

                // HP 바 로직 - 씬에 hpBar 참조가 있거나 UI 시스템이 업데이트한다고 가정
                // Tutorial.jsx의 기존 로직에 따름:
                // "this.hpBar.width = 220 * hpPercent"

                // UI 업데이트 메서드나 CombatSystem/PlayerSystem에 위임해야 함
                if (this.scene.updatePlayerHealthUI) {
                    this.scene.updatePlayerHealthUI();
                }

                /* 
                   기존 코드에서는 인라인으로 처리함:
                   const hpPercent = this.player.hp / this.player.maxHp;
                   this.hpBar.width = 220 * hpPercent;
                   if (hpPercent < 0.3) this.hpBar.fillColor = 0xff0000;
                   else this.hpBar.fillColor = 0x00ff00;
                   if (player.hp <= 0) { ... }
                */
            },
            null // 분리 허용 (기본값)
        );

        // 몬스터 vs 장애물
        if (this.scene.obstacles) {
            this.scene.physics.add.collider(this.monsters, this.scene.obstacles);
        }
    }

    spawnMonster(data) {
        const { id, x, y, hp } = data;
        if (this.monstersMap.has(id)) return;

        const m = this.scene.add.circle(x, y, 20, 0xff8800);
        this.scene.physics.add.existing(m);

        m.body.setCircle(20);
        m.body.setCollideWorldBounds(true);
        m.body.pushable = false;

        m.id = id;
        m.hp = hp;
        m.maxHp = data.maxHp || hp || 100;
        m.type = data.type || "NORMAL";
        m.lastPatternState = "NONE"; // 상태 추적용 초기화

        if (m.type === "BOSS") {
            m.setRadius(60);
            m.body.setCircle(60);
            m.setFillStyle(0xff0000); // 보스는 빨간색
        }

        // HP 바
        const isBoss = data.type === "BOSS";
        const barWidth = isBoss ? 120 : 40;
        const barY = isBoss ? -70 : -30;

        const hpBg = this.scene.add.rectangle(x, y + barY, barWidth, 6, 0xff0000);
        const hpBar = this.scene.add.rectangle(x - barWidth/2, y + barY, barWidth, 6, 0x00ff00);
        hpBar.setOrigin(0, 0.5);

        // 깊이(Depth) 설정
        m.setDepth(10);
        hpBg.setDepth(10);
        hpBar.setDepth(10);

        m.hpBg = hpBg;
        m.hpBar = hpBar;
        m.barWidth = barWidth;
        m.barY = barY;

        this.monsters.add(m);
        this.monstersMap.set(id, m);

        // 마스크 (Mask)
        // 몬스터는 Darkness(깊이 1000)에 의해 가려지므로, 마스크가 반전된 경우 개별적으로 마스크를 적용할 필요가 없음.
        // 어둠 구멍에 사용되는 반전 마스크를 몬스터에 적용하면 구멍 안에서 보이지 않게 됨.
        /*
        if (this.scene.visionSystem && this.scene.visionSystem.visionMask) {
            m.setMask(this.scene.visionSystem.visionMask);
            m.hpBg.setMask(this.scene.visionSystem.visionMask);
            m.hpBar.setMask(this.scene.visionSystem.visionMask);
        }
        */
    }

    updateMonsterHpBars() {
        this.monsters.children.each((monster) => {
            if (!monster.active) return;
            const barY = monster.barY || -30;
            const barWidth = monster.barWidth || 40;
            
            monster.hpBg.setPosition(monster.x, monster.y + barY);
            monster.hpBar.setPosition(monster.x - barWidth/2, monster.y + barY);
            
            const hpPercent = Phaser.Math.Clamp(monster.hp / monster.maxHp, 0, 1);
            monster.hpBar.width = barWidth * hpPercent;
        });
    }

    drawTelegraphs() {
        if (!this.telegraphGraphics) return;
        this.telegraphGraphics.clear();

        this.monstersMap.forEach((m) => {
            if (m.type === "BOSS" && m.patternState === "TELEGRAPH") {
                this.telegraphGraphics.lineStyle(2, 0xff0000, 0.5);
                this.telegraphGraphics.fillStyle(0xff0000, 0.2);

                if (m.patternType === "DASH") {
                    // 돌진 경로 표시 (직선)
                    this.telegraphGraphics.lineBetween(m.x, m.y, m.telegraphX, m.telegraphY);
                    this.telegraphGraphics.strokeCircle(m.telegraphX, m.telegraphY, 40);
                } else if (m.patternType === "SWING") {
                    // 휘두르기 범위 (원)
                    this.telegraphGraphics.fillCircle(m.x, m.y, 180);
                    this.telegraphGraphics.strokeCircle(m.x, m.y, 180);
                } else if (m.patternType === "SLAM") {
                    // 내려찍기 범위 (원)
                    this.telegraphGraphics.fillCircle(m.telegraphX, m.telegraphY, 250);
                    this.telegraphGraphics.strokeCircle(m.telegraphX, m.telegraphY, 250);
                }
            }
            
            // 패턴 중 ACTION 상태일 때 특수 효과? (필요시 추가)
        });
    }

    update() {
        this.updateMonsterHpBars();
        this.drawTelegraphs();
        this.handleBossEffects();
        this.checkBossActive();
    }

    checkBossActive() {
        let bossFound = false;
        for (const m of this.monstersMap.values()) {
            if (m.type === "BOSS") {
                bossFound = true;
                break;
            }
        }
        this.scene.isBossActive = bossFound;
    }

    handleBossEffects() {
        this.monstersMap.forEach(m => {
            if (m.type !== "BOSS") return;

            // 상태 변경 감지
            if (m.lastPatternState !== m.patternState) {
                if (m.patternState === "ACTION") {
                    this.playActionEffect(m);
                }
                m.lastPatternState = m.patternState;
            }
        });
    }

    playActionEffect(m) {
        if (m.patternType === "SLAM") {
            // 점프 효과 (간단히 투명도 흐리기나 스케일 조절로 표현 가능하지만 여기선 생략)
            
            // 0.8초 후 내려찍기 임팩트
            this.scene.time.delayedCall(800, () => {
                if (!m.active) return;
                
                // 1. 화면 흔들림 (강하게)
                this.scene.cameras.main.shake(200, 0.015); 
                
                // 2. 충격파 이펙트
                this.showImpactCircle(m.telegraphX, m.telegraphY, 250, 0xff0000);
            });
        }
        else if (m.patternType === "SWING") {
            // 즉시 휘두르기 이펙트
            this.scene.cameras.main.shake(100, 0.005);
            this.showImpactCircle(m.x, m.y, 180, 0xffaa00);
        }
    }

    showImpactCircle(x, y, radius, color) {
        const g = this.scene.add.graphics();
        g.fillStyle(color, 0.6);
        g.fillCircle(x, y, radius);
        g.setDepth(20);
        
        this.scene.tweens.add({
            targets: g,
            alpha: 0,
            scale: 1.1,
            duration: 300,
            onComplete: () => g.destroy()
        });
    }

    removeMonster(id) {
        const m = this.monstersMap.get(id);
        if (!m) return;

        // HP 바 제거
        if (m.hpBar) m.hpBar.destroy();
        if (m.hpBg) m.hpBg.destroy();

        // 몬스터 본체 제거
        m.destroy();

        // 맵 및 그룹에서 제거
        this.monstersMap.delete(id);
        this.monsters.remove(m);
    }

    clearMonsters() {
        if (!this.monstersMap) return;

        // Map의 모든 키(id)를 수집하여 순회
        // forEach 중 delete를 하면 문제될 수 있으므로 키 목록을 먼저 확보
        const ids = Array.from(this.monstersMap.keys());
        ids.forEach(id => {
            this.removeMonster(id);
        });

        this.monstersMap.clear();
        console.log("🧹 Monsters Cleared");
    }
}
