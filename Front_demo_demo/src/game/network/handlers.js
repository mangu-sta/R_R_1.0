import Phaser from "phaser";

export default class NetworkHandlers {
    constructor(scene) {
        this.scene = scene;
    }

    handleMessage(chat) {
        switch (chat.type) {
            case "MOVE":
                this.handleMove(chat);
                break;
            case "HP":
                this.handleHp(chat);
                break;
            case "MONSTER_HP_UPDATE":
                this.handleMonsterHp(chat);
                break;
            case "FIRE":
            case "BULLET":
                this.handleFire(chat);
                break;
            case "MONSTER_STATE":
                this.handleMonsterState(chat);
                break;
            case "EXP":
                this.handleExp(chat);
                break;
            case "PLAYER_HP_UPDATE":
                this.handlePlayerHpUpdate(chat);
                break;
            case "ROTATE":
                this.handleRotate(chat);
                break;
            case "PLAYER_STATE":
                this.handlePlayerState(chat);
                break;
            case "LEAVE":
            case "KICK":
            case "MESSAGE":
                this.handlePlayerLeave(chat);
                break;
            case "BOSS_KILLED":
                this.handleBossKilled(chat);
                break;
            case "GAME_OVER":
                this.handleGameOver(chat);
                break;
            case "INIT":
                this.handleInit(chat);
                break;
            default:
                break;
        }
    }

    handleInit(chat) {
        console.log("🏁 [NetworkHandlers] INIT message received:", chat);

        // 1. 플레이어 소환
        if (chat.players && this.scene.partyPlayersSystem) {
            chat.players.forEach(p => {
                // 내 캐릭터는 건너뜀
                if (Number(p.userId) === Number(this.scene.myUserId)) return;

                // 기존 캐릭터 정보 래핑 (createPartyPlayer 형식에 맞게)
                const userData = {
                    id: p.characterId,
                    userId: p.userId,
                    nickname: p.nickname,
                    job: p.job,
                    hp: p.hp,
                    maxHp: p.maxHp,
                    posX: p.posX,
                    posY: p.posY
                };
                this.scene.partyPlayersSystem.createPartyPlayer(userData, p.posX, p.posY);
            });
        }

        // 2. 몬스터 소환
        if (chat.monsters && this.scene.monstersSystem) {
            chat.monsters.forEach(m => {
                this.scene.monstersSystem.spawnMonster({
                    monsterId: m.monsterId,
                    x: m.x,
                    y: m.y,
                    hp: m.hp,
                    alive: m.alive
                });
            });
        }
    }

    handleGameOver(chat) {
        console.log("💀 GAME OVER BROADCAST RECEIVED");
        this.scene.game.events.emit('show-game-over');
    }

    handleBossKilled(chat) {
        // chat: { type: 'BOSS_KILLED', killer: 'Nickname', timeTaken: 123 }
        console.log("🏆 BOSS KILLED by " + chat.killer + " in " + chat.timeTaken + "s");
        this.scene.game.events.emit('show-boss-result', chat);
    }

    handleMove(chat) {
        if (!this.scene.partyPlayersSystem) return;

        const p = this.scene.partyPlayersSystem.partyPlayers
            .getChildren()
            .find((pp) => pp.userId === chat.userId);

        if (!p) return;

        // 🔥 네트워크 입력 상태 저장
        p.netInput = {
            up: chat.up,
            down: chat.down,
            left: chat.left,
            right: chat.right,
            isRunning: chat.isRunning,
        };

        // 🔥 서버 좌표 (보정용)
        p.serverX = chat.x;
        p.serverY = chat.y;
        p.lastMoveTime = Date.now();
    }

    handleHp(chat) {
        if (!this.scene.partyPlayersSystem) return;

        const p = this.scene.partyPlayersSystem.partyPlayers
            .getChildren()
            .find((pp) => pp.userId === chat.userId);

        if (!p) return;

        p.hp = chat.hp;

        // If it's my player, update UI Overlay
        if (p.userId === this.scene.myUserId) {
            this.scene.game.events.emit('update-hp', p.hp, p.maxHp);
        } else {
            // Party member HP bar (if exists above head)
            p.hpBar.width = (p.hp / p.maxHp) * 36;
        }
    }

    handleFire(chat) {
        this.scene.combatSystem?.spawnPartyBullet(chat);
    }

    handleExp(chat) {
        // chat: { userId, exp, level, (optional) maxExp }

        // 1. My Player handling
        if (this.scene.myUserId === chat.userId && this.scene.player) {
            const oldLevel = this.scene.player.level || 0;
            const newLevel = chat.level;

            this.scene.player.exp = chat.exp;
            this.scene.player.level = newLevel;

            // Calculate maxExp if not provided (Example formula: level * 100)
            const maxExp = chat.maxExp || (newLevel * 100);
            this.scene.player.maxExp = maxExp;

            // UI Update - Emit Event
            this.scene.game.events.emit('update-exp', chat.exp, maxExp, newLevel);

            // Level Up Event
            if (newLevel > oldLevel) {
                console.log(`🎉 Level Up! ${oldLevel} -> ${newLevel}`);
                window.dispatchEvent(new CustomEvent("level-up", {
                    detail: { level: newLevel }
                }));
            }
            return;
        }

        // 2. Other Party Players handling (if we want to show their level too, but UI might not support it yet)
        if (this.scene.partyPlayersSystem) {
            const p = this.scene.partyPlayersSystem.partyPlayers
                .getChildren()
                .find((pp) => pp.userId === chat.userId);

            if (p) {
                p.level = chat.level;
                p.exp = chat.exp;
                // Currently no UI for other players' level over their head, but good to update state.
            }
        }
    }

    handleMonsterState(chat) {
        if (!this.scene.monstersSystem) return;

        // 0. 서버 리스트에 없는 몬스터 제거 (Sync List)
        const incomingIds = new Set(chat.monsters.map(ms => ms.id));
        for (const [id, monster] of this.scene.monstersSystem.monstersMap) {
            if (!incomingIds.has(id)) {
                this.scene.monstersSystem.removeMonster(id);
            }
        }

        chat.monsters.forEach((ms) => {
            let m = this.scene.monstersSystem.monstersMap.get(ms.id);

            // 1. 체력이 0 이하인 경우 (명시적 제거)
            if (ms.hp <= 0) {
                if (m) this.scene.monstersSystem.removeMonster(ms.id);
                return;
            }

            // 2. 새로운 몬스터인 경우 (체력 > 0)
            if (!m) {
                this.scene.monstersSystem.spawnMonster(ms);
                m = this.scene.monstersSystem.monstersMap.get(ms.id);
            }

            // 3. 기존/신규 몬스터 데이터 업데이트
            if (m) {
                // 위치 보간
                m.x = Phaser.Math.Linear(m.x, ms.x, 0.25);
                m.y = Phaser.Math.Linear(m.y, ms.y, 0.25);

                // HP 업데이트
                m.hp = ms.hp;
                m.maxHp = ms.maxHp || m.maxHp || 100; // 서버 데이터 사용
                m.hpBar.width = Math.max(0, (m.hp / m.maxHp) * 40);

                // 상태 및 타겟 정보
                m.state = ms.state;
                m.targetUserId = ms.targetUserId;

                // Boss-related fields
                m.type = ms.type;
                m.patternState = ms.patternState;
                m.patternType = ms.patternType;
                m.telegraphX = ms.telegraphX;
                m.telegraphY = ms.telegraphY;
            }
        });
    }

    handleMonsterHp(chat) {
        // chat: { type: 'MONSTER_HP_UPDATE', monsterId: 1, hp: 80, dead: false, state: 'CHASE' }
        if (!this.scene.monstersSystem) return;

        const m = this.scene.monstersSystem.monstersMap.get(chat.monsterId);
        if (m) {
            m.hp = chat.hp;
            if (chat.dead) {
                this.scene.monstersSystem.removeMonster(chat.monsterId);
            }
        }
    }

    handlePlayerHpUpdate(chat) {
        // chat: { type: 'PLAYER_HP_UPDATE', characterId: 20, hp: 65, dead: false }
        if (!this.scene.player) return;

        // 1. My Player handling
        if (this.scene.myCharacterId === chat.characterId) {
            this.scene.player.hp = chat.hp;
            this.scene.game.events.emit('update-hp', chat.hp, this.scene.player.maxHp);

            if (chat.dead) {
                console.log("💀 YOU ARE DEAD (Waiting for Game Over Broadcast)");
                // this.scene.game.events.emit('show-game-over'); // 🔥 Disable local trigger, use broadcast
            }
            return;
        }

        // 2. Other Party Players handling
        if (this.scene.partyPlayersSystem) {
            const p = this.scene.partyPlayersSystem.partyPlayers
                .getChildren()
                .find((pp) => pp.characterId === chat.characterId);

            if (p) {
                p.hp = chat.hp;
                // Update HP bar (width: 36px base)
                const percent = Math.max(0, p.hp / p.maxHp);
                p.hpBar.width = percent * 36;

                if (chat.dead) {
                    console.log(`💀 Party member ${p.userId} is dead`);
                }
            }
        }
    }

    handlePlayerState(chat) {
        // console.log("🔍 [NetworkHandlers] PLAYER_STATE received:", chat);
        if (Number(this.scene.myUserId) === Number(chat.userId) && this.scene.player) {
            const oldLevel = this.scene.player.level;
            const newLevel = chat.level;

            this.scene.player.exp = chat.exp;
            this.scene.player.level = newLevel;
            this.scene.player.maxHp = chat.maxHp;
            this.scene.player.hp = chat.hp;
            this.scene.player.pendingStatPoints = chat.pendingStatPoints; // ✅ 필드 동기화 추가

            // maxExp: 서버 데이터 우선, 없으면 공식 계산(동기화: 50 + level * 10)
            const maxExp = chat.maxExp || (50 + (newLevel * 10));
            this.scene.player.maxExp = maxExp;

            // UI Update - Emit Event
            this.scene.game.events.emit('update-exp', chat.exp, maxExp, newLevel);
            this.scene.game.events.emit('update-hp', chat.hp, chat.maxHp);

            // React State Sync Event
            window.dispatchEvent(new CustomEvent("player-data-sync", {
                detail: chat
            }));

            // Level Up Event (레벨이 증가했을 때만 발생)
            if (oldLevel !== undefined && newLevel > oldLevel) {
                console.log(`🎉 [LEVEL UP EVENT] Dispatched: ${oldLevel} -> ${newLevel}`);
                window.dispatchEvent(new CustomEvent('level-up', {
                    detail: { oldLevel, newLevel }
                }));
            }
        }
    }

    handleRotate(chat) {
        // chat: { userId, angle, timestamp }
        if (!this.scene.partyPlayersSystem) return;

        const p = this.scene.partyPlayersSystem.partyPlayers
            .getChildren()
            .find((pp) => pp.userId === chat.userId);

        if (p) {
            p.lookAngle = chat.angle;
        }
    }

    handlePlayerLeave(chat) {
        console.log("👋 Member status broadcast received:", chat);

        // prioritising actual fields like userId or nickname
        let identifier = chat.userId || chat.nickname;

        // Fallback to value if it's a string (avoiding generic "LEAVE")
        if (!identifier && typeof chat.value === 'string' && chat.value !== "LEAVE" && chat.value !== "KICK") {
            identifier = chat.value;
        }

        // Fallback to message if it's a string
        if (!identifier && typeof chat.message === 'string' && chat.message !== "LEAVE" && chat.message !== "KICK") {
            identifier = chat.message;
        }

        if (!identifier) {
            console.log("ℹ️ No specific player ID found in broadcast. Relying on list-sync fallback.");
            return;
        }

        console.log(`👋 Identified leaver from broadcast: ${identifier}`);
        this.scene.partyPlayersSystem?.removePartyPlayer(identifier);
    }
}
