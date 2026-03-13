import Phaser from "phaser";
import { BASE_SPEED_DEFAULT, RUN_SPEED_DEFAULT } from "../constants";

export default class PartyPlayersSystem {
    constructor(scene) {
        this.scene = scene;
        this.partyPlayers = null;
    }

    create() {
        this.partyPlayers = this.scene.add.group();
    }

    createPartyPlayer(user, x, y) {
        // 중복 생성 방지 (여러 식별자 확인)
        const exists = this.partyPlayers
            .getChildren()
            .some((p) =>
                (user.userId && p.userId == user.userId) ||
                (user.id && p.characterId == user.id) ||
                (user.nickname && p.nickname == user.nickname)
            );

        if (exists) return;

        // 캐릭터 본체
        const jobTexture = user.job || "SOLDIER";
        const p = this.scene.add.sprite(x, y, jobTexture);
        this.scene.physics.add.existing(p);

        // Physics body setup
        const bodyRadius = p.width * 0.45; // 90% Width coverage in texture space
        p.body.setCircle(bodyRadius);
        p.body.setOffset(
            (p.width / 2) - bodyRadius,
            (p.height / 2) - bodyRadius
        );

        p.setDisplaySize(40, 40); // 원본 동그라미 크기(지름 40)에 맞춤
        p.body.setImmovable(true);

        p.userId = user.userId || user.id; // 🔥 여러 필드 지원
        p.characterId = user.id;
        p.nickname = user.nickname; // 🔥 닉네임 저장 (제거 시 검색용)

        console.log(`👤 PartyPlayer created: ${p.nickname} (ID: ${p.userId}, CharID: ${p.characterId})`);

        // 네트워크 입력
        p.netInput = {
            up: false,
            down: false,
            left: false,
            right: false,
            isRunning: false,
        };

        p.serverX = x;
        p.serverY = y;
        p.lookAngle = 0; // 🔥 초기 각도

        // 스탯
        p.maxHp = user.maxHp || 100;
        p.hp = user.hp !== undefined ? user.hp : p.maxHp;

        // 닉네임
        const nameText = this.scene.add
            .text(x, y - 40, user.nickname, {
                fontSize: "14px",
                color: "#ffffff",
                stroke: "#000000",
                strokeThickness: 3,
            })
            .setOrigin(0.5);

        // HP 바
        const hpBg = this.scene.add.rectangle(x, y - 25, 36, 5, 0xff0000);
        const hpBar = this.scene.add.rectangle(x - 18, y - 25, 36, 5, 0x00ff00);
        hpBar.setOrigin(0, 0.5);

        // 초기 HP 바 높이 업데이트
        hpBar.width = (p.hp / p.maxHp) * 36;

        // 총 (Gun)
        const gun = this.scene.add.sprite(x, y, "ak47");
        gun.setOrigin(0.2, 0.5);
        gun.setScale(0.0733); // 본인 총 크기와 동일하게 조절 (Resized: 0.11 -> 0.0733)

        // 깊이(Depth) 설정
        [p, nameText, hpBg, hpBar, gun].forEach((o) => {
            o.setDepth(10);
        });
        gun.setDepth(11); // 총은 몸체보다 살짝 위

        // 참조 설정
        p.nameText = nameText;
        p.hpBg = hpBg;
        p.hpBar = hpBar;
        p.gun = gun;

        this.partyPlayers.add(p);
    }

    removePartyPlayer(idOrName) {
        if (!this.partyPlayers || idOrName === undefined || idOrName === null) return;

        // "LEAVE" 같은 시스템 메시지가 인자로 들어오는 경우 건너뜀
        if (idOrName === "LEAVE" || idOrName === "KICK") return;

        console.log(`🔍 Seeking player to remove: [${idOrName}] (type: ${typeof idOrName})`);

        const p = this.partyPlayers
            .getChildren()
            .find((pp) =>
                (pp.userId != null && pp.userId == idOrName) ||
                (pp.characterId != null && pp.characterId == idOrName) ||
                (pp.nickname != null && pp.nickname == idOrName)
            );

        if (p) {
            console.log(`✨ Found player ${p.nickname}. Destroying...`);
            // Destroy associated UI and objects
            if (p.nameText) p.nameText.destroy();
            if (p.hpBg) p.hpBg.destroy();
            if (p.hpBar) p.hpBar.destroy();
            if (p.gun) p.gun.destroy();

            // Destroy the player sprite itself
            const nick = p.nickname;
            p.destroy();

            // Remove from the physics group
            this.partyPlayers.remove(p);

            console.log(`✅ Player ${nick} successfully removed from scene`);
        } else {
            console.warn(`⚠️ Player [${idOrName}] not found in active scene list. Current children:`,
                this.partyPlayers.getChildren().map(c => `${c.nickname}(${c.userId})`)
            );
        }
    }

    // 🔥 서버 유저 리스트와 씬상의 파티원 리스트를 동기화 (추가 및 제거)
    syncPartyPlayers(userList, myUserId, defaultX = 1000, defaultY = 1000) {
        if (!this.partyPlayers) return;

        // 1. 없는 유저 제거 (Safe iteration with spread)
        console.log("🔄 Syncing Party Players with UserList:", userList.map(u => u.nickname));

        [...this.partyPlayers.getChildren()].forEach(p => {
            const stillExists = userList.some(u =>
                (u.userId != null && u.userId == p.userId) ||
                (u.id != null && u.id == p.characterId) ||
                (u.nickname != null && u.nickname == p.nickname)
            );
            if (!stillExists) {
                console.log(`👤 Sync mismatch detected: ${p.nickname} is gone!`);
                this.removePartyPlayer(p.userId || p.characterId || p.nickname);
            }
        });

        // 2. 새로운 유저 추가
        userList.forEach((user, i) => {
            if (user.userId == myUserId) return;

            // 🔥 서버 좌표 우선 사용, 없으면 기본 오프셋 스폰
            const startX = user.posX !== undefined ? user.posX : defaultX + i * 60;
            const startY = user.posY !== undefined ? user.posY : defaultY;

            this.createPartyPlayer(user, startX, startY);
        });
    }

    update(delta) {
        // window.playerStats는 파티원에게도 적용됨
        const baseSpeed = window.playerStats?.baseSpeed ?? BASE_SPEED_DEFAULT;
        const runSpeed = window.playerStats?.runSpeed ?? RUN_SPEED_DEFAULT;

        this.partyPlayers.children.each((p) => {
            if (!p.active || !p.netInput) return;

            let dx = 0;
            let dy = 0;

            if (p.netInput.left) dx -= 1;
            if (p.netInput.right) dx += 1;
            if (p.netInput.up) dy -= 1;
            if (p.netInput.down) dy += 1;

            const vec = new Phaser.Math.Vector2(dx, dy);

            if (vec.length() > 0) {
                vec.normalize();
                const speed = p.netInput.isRunning ? runSpeed : baseSpeed;

                p.x += vec.x * speed * (delta / 1000);
                p.y += vec.y * speed * (delta / 1000);
            }

            // 위치 동기화 보정 (Smoothing)
            if (p.serverX !== undefined) {
                const dist = Phaser.Math.Distance.Between(
                    p.x,
                    p.y,
                    p.serverX,
                    p.serverY
                );

                if (dist > 200) {
                    // 격차가 너무 크면 즉시 순간이동
                    p.x = p.serverX;
                    p.y = p.serverY;
                } else if (dist > 2) {
                    // 자연스러운 보간 (Lerp)
                    // 거리가 멀수록 보정 강도를 약간 높임
                    const lerpFactor = dist > 50 ? 0.2 : 0.1;
                    p.x = Phaser.Math.Linear(p.x, p.serverX, lerpFactor);
                    p.y = Phaser.Math.Linear(p.y, p.serverY, lerpFactor);
                }
            }

            // UI 동기화
            p.nameText.setPosition(p.x, p.y - 40);
            p.hpBg.setPosition(p.x, p.y - 25);
            p.hpBar.setPosition(p.x - 18, p.y - 25);

            // 총 위치 및 회전 동기화
            if (p.gun) {
                p.gun.setPosition(p.x, p.y);
                p.gun.rotation = p.lookAngle || 0;
            }
            // 캐릭터 회전 동기화 (오프셋 제거)
            p.rotation = p.lookAngle || 0;
        });
    }
}
