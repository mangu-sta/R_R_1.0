import Phaser from "phaser";
import { BULLET_SPEED, BULLET_RANGE_NORMAL, BULLET_RANGE_AIMING, GUN_MUZZLE_OFFSET } from "../constants";
import keyMapping from "../utils/keyMapping";
import soundManager from "../utils/soundManager";
import { toast } from "react-toastify";

export default class CombatSystem {
    constructor(scene) {
        this.scene = scene;
        this.keys = keyMapping.getKeys();
        this.bullets = null;
        this.partyBullets = null;
        this.isFiring = false;
        this.fireTimer = null;

        // 탄약 (Ammo)
        this.maxAmmo = 20;
        this.ammo = 20;
        this.isReloading = false;
        this.reloadKey = null;
        this.meleeKey = null;
        this.fireModeKey = null;
        this.isMeleeing = false;
        this.isAutoMode = true; // Default to Auto mode
        this.reloadTimer = null; // Track active reload timer


        // 키 업데이트 이벤트 리스너 (Cleanup을 위해 참조 저장)
        this.onKeysUpdated = (e) => {
            this.updateKeyBindings(e.detail);
        };
        window.addEventListener("keys-updated", this.onKeysUpdated);

        // Scene 종료 시 리스너 해제
        this.scene.events.once("shutdown", () => this.cleanup());
        this.scene.events.once("destroy", () => this.cleanup());
    }

    cleanup() {
        window.removeEventListener("keys-updated", this.onKeysUpdated);
    }

    updateKeyBindings(newKeys) {
        // Scene이 없거나 input 시스템이 없으면 중단
        if (!this.scene || !this.scene.input || !this.scene.input.keyboard) return;

        this.keys = newKeys;

        // Reload Key
        if (this.reloadKey) this.scene.input.keyboard.removeKey(this.reloadKey); // destroy 대신 removeKey 시도, Phaser 버전에 따라 다를 수 있음. 보통 destroy()면 충분
        if (this.reloadKey && this.reloadKey.destroy) this.reloadKey.destroy();

        this.reloadKey = this.scene.input.keyboard.addKey(this.keys.reload);
        this.reloadKey.on('down', () => this.reload());

        // Melee Key
        if (this.meleeKey) this.meleeKey.destroy();
        this.meleeKey = this.scene.input.keyboard.addKey(this.keys.melee);
        this.meleeKey.on('down', () => this.melee());

        // Fire Mode Key
        if (this.fireModeKey) this.fireModeKey.destroy();
        this.fireModeKey = this.scene.input.keyboard.addKey(this.keys.fireMode);
        this.fireModeKey.on('down', () => this.toggleFireMode());

        this.updateFireKeyBinding();
    }

    updateFireKeyBinding() {
        if (this.fireKey) {
            this.fireKey.destroy();
            this.fireKey = null;
        }

        const fireKeyStr = this.keys.fire;
        const isMouse = fireKeyStr === "LeftClick" || fireKeyStr === "RightClick";

        if (!isMouse && this.scene.input.keyboard) {
            this.fireKey = this.scene.input.keyboard.addKey(fireKeyStr);
            this.fireKey.on('down', () => this.startFiring());
            this.fireKey.on('up', () => this.stopFiring());
        }
    }

    create() {
        this.bullets = this.scene.physics.add.group({
            runChildUpdate: true,
        });
        this.partyBullets = this.scene.physics.add.group();

        this.reloadKey = this.scene.input.keyboard.addKey(this.keys.reload);
        this.reloadKey.on('down', () => this.reload());

        this.meleeKey = this.scene.input.keyboard.addKey(this.keys.melee);
        this.meleeKey.on('down', () => this.melee());

        this.fireModeKey = this.scene.input.keyboard.addKey(this.keys.fireMode);
        this.fireModeKey.on('down', () => this.toggleFireMode());

        this.updateFireKeyBinding();

        // 초기 UI 업데이트 (Initial UI Update)
        this.scene.time.delayedCall(500, () => {
            this.scene.game.events.emit('update-ammo', this.ammo, this.maxAmmo);
            this.scene.game.events.emit('update-fire-mode', this.isAutoMode);
        });

        // 충돌/오버랩 로직 (Overlap Logic)
        // 몬스터 그룹을 사용하여 아래에서 실제 충돌을 등록합니다.

        // MainScene에서 혹은 몬스터 그룹에 접근 가능한 경우 여기서 충돌 설정을 수행합니다.
        if (this.scene.monstersSystem && this.scene.monstersSystem.monsters) {
            // 본인 총알: 서버로 적중 보고만 수행 (Local bullets: Report hit to server only)
            this.scene.physics.add.overlap(
                this.bullets,
                this.scene.monstersSystem.monsters,
                (bullet, monster) => {
                    bullet.destroy();

                    // 서버로 적중 메시지 전송 (Send hit message to server)
                    if (this.scene.myUserId) {
                        this.scene.sendMonsterHit({
                            userId: this.scene.myUserId,
                            userX: this.scene.player.x,
                            userY: this.scene.player.y,
                            monsterId: monster.id,
                            monsterX: monster.x,
                            monsterY: monster.y,
                            isKnife: false
                        });
                    }
                }
            );

            // 파티원 총알: 시각적으로만 소멸 (Party bullets: Destroy visually only, no damage)
            this.scene.physics.add.overlap(
                this.partyBullets,
                this.scene.monstersSystem.monsters,
                (bullet, monster) => {
                    bullet.destroy();
                }
            );
        }

        this.setupInput();
    }

    setupInput() {
        this.scene.input.on("pointerdown", (pointer) => {
            const fireKey = this.keys.fire;
            const isTargetButton = (fireKey === "LeftClick" && pointer.button === 0) ||
                (fireKey === "RightClick" && pointer.button === 2);

            if (!isTargetButton) return;
            this.startFiring();
        });

        const stopFiring = () => {
            if (this.keys.fire === "LeftClick" || this.keys.fire === "RightClick") {
                this.stopFiring();
            }
        };

        this.scene.input.on("pointerup", stopFiring);

        // 전역 이벤트 (Global events)
        const onGlobalPointerUp = () => stopFiring();
        window.addEventListener("mouseup", onGlobalPointerUp);
        window.addEventListener("pointerup", onGlobalPointerUp);

        // 씬 종료 시 정리 (일반적으로 MainScene에서 처리하지만, 
        // 여기서 직접 등록하거나 destroy를 호출하는 것을 기억해야 함)
        this.scene.events.on("shutdown", () => {
            this.stopFiring();
            if (this.fireKey) {
                this.fireKey.destroy();
                this.fireKey = null;
            }
            if (this.fireModeKey) {
                this.fireModeKey.destroy();
                this.fireModeKey = null;
            }
            if (this.reloadKey) {
                this.reloadKey.destroy();
                this.reloadKey = null;
            }
            if (this.meleeKey) {
                this.meleeKey.destroy();
                this.meleeKey = null;
            }
            window.removeEventListener("mouseup", onGlobalPointerUp);
            window.removeEventListener("pointerup", onGlobalPointerUp);
        });
        this.scene.events.on("destroy", () => {
            this.stopFiring();
            if (this.fireKey) {
                this.fireKey.destroy();
                this.fireKey = null;
            }
            if (this.fireModeKey) {
                this.fireModeKey.destroy();
                this.fireModeKey = null;
            }
            if (this.reloadKey) {
                this.reloadKey.destroy();
                this.reloadKey = null;
            }
            if (this.meleeKey) {
                this.meleeKey.destroy();
                this.meleeKey = null;
            }
            window.removeEventListener("mouseup", onGlobalPointerUp);
            window.removeEventListener("pointerup", onGlobalPointerUp);
        });
    }

    startFiring() {
        if (this.isFiring || this.isReloading) return;
        if (this.ammo <= 0) {
            this.scene.sound.play("empty_gun", { volume: soundManager.getVolume("sfx") });
            return;
        }

        this.isFiring = true;

        // 즉시 발사 (Immediate fire)
        this.performFire();

        // 연사 모드인 경우에만 타이머 등록 (Register timer only in Auto mode)
        if (this.isAutoMode) {
            this.fireTimer = this.scene.time.addEvent({
                delay: 150,
                loop: true,
                callback: () => {
                    if (this.ammo <= 0) {
                        this.stopFiring();
                        return;
                    }
                    if (this.isReloading) {
                        this.stopFiring();
                        return;
                    }
                    this.performFire();
                },
            });
        }
    }

    performFire() {
        if (this.isReloading) {
            console.warn("장전 중에는 사격할 수 없습니다. (performFire blocked)");
            return;
        }

        // 발사 메시지 전송 (Send Fire Message)
        if (this.scene.sendFire) {
            this.scene.sendFire();
        }

        // 로컬에서 발사 (Fire locally)
        this.fireBullet(
            this.scene.player.x,
            this.scene.player.y
        );
    }

    stopFiring() {
        this.isFiring = false;
        if (this.fireTimer) {
            this.fireTimer.remove();
            this.fireTimer = null;
        }
    }

    toggleFireMode() {
        this.isAutoMode = !this.isAutoMode;
        console.log(`발사 모드 전환: ${this.isAutoMode ? "연사" : "단발"}`);

        // 시각적 피드백 (Visual feedback)
        this.scene.sound.play("reload", { volume: soundManager.getVolume("sfx") * 0.5, rate: 2 });
        // const modeText = this.isAutoMode ? "연사 (AUTO)" : "단발 (SINGLE)";
        // toast.info(`사격 모드: ${modeText}`, { autoClose: 1000 });

        // UI 연동 이벤트 발생
        this.scene.game.events.emit('update-fire-mode', this.isAutoMode);

        // 발사 중인 경우 즉시 반영 (Apply immediately if firing)
        if (this.isFiring) {
            if (this.isAutoMode) {
                if (!this.fireTimer) {
                    this.fireTimer = this.scene.time.addEvent({
                        delay: 150,
                        loop: true,
                        callback: () => {
                            if (this.ammo <= 0 || this.isReloading) {
                                this.stopFiring();
                                return;
                            }
                            this.performFire();
                        },
                    });
                }
            } else {
                if (this.fireTimer) {
                    this.fireTimer.remove();
                    this.fireTimer = null;
                }
            }
        }
    }

    reload() {
        if (this.isReloading) return;
        if (this.ammo >= this.maxAmmo) return;

        this.isReloading = true;
        this.stopFiring();
        // console.log("장전 중...");
        this.scene.sound.play("reload", { volume: soundManager.getVolume("sfx") }); // 장전 사운드

        // RELOAD 스탯에 기반한 장전 소요 시간 계산
        const stat = this.scene.player.reloadStat || 0;
        let duration = 2400 - (stat - 4) * (1900 / 36);
        duration = Math.max(500, duration); // Clamp to minimum 0.5s

        // 장전(Reload) 이벤트 발생
        this.scene.game.events.emit('start-reload', duration);

        this.reloadTimer = this.scene.time.delayedCall(duration, () => {
            if (this.scene && this.scene.sys) { // Scene still exists check
                this.ammo = this.maxAmmo;
                this.isReloading = false;
                this.reloadTimer = null;
                console.log("장전 완료");
                this.scene.game.events.emit('update-ammo', this.ammo, this.maxAmmo);
            }
        });
    }

    melee() {
        const player = this.scene.player;
        if (!player || this.isMeleeing) return;

        // 재장전 중 근접 공격 시 재장전 중단 (Interrupt reload if meleeing)
        if (this.isReloading) {
            if (this.reloadTimer) {
                this.reloadTimer.remove();
                this.reloadTimer = null;
            }
            this.isReloading = false;
            console.log("재장전 중단 (근접 공격)");
            this.scene.game.events.emit('cancel-reload');
            this.scene.game.events.emit('update-ammo', this.ammo, this.maxAmmo); // UI 갱신 (Press R 다시 띄우기 위해)
        }

        // 팔 스테미나 25 소모 (최대 100기준 4회)
        if (player.armStamina < 25) {
            console.log("근접 공격을 위한 스테미나가 부족합니다.");
            return;
        }

        this.isMeleeing = true;
        player.armStamina -= 25;
        player.armStaminaCooldown = 1000; // 공격 후 잠시 회복 대기
        this.scene.game.events.emit('update-arm-stamina', player.armStamina, player.maxArmStamina);

        this.stopFiring();
        this.scene.sound.play("reload", { volume: soundManager.getVolume("sfx") * 0.3, rate: 2 }); // 임시 휘두르는 소리

        // --- 시각 효과 (Knife Swing with Player Pivot) ---
        const angle = this.scene.lookAngle;
        const radius = 75; // 판정 범위 대폭 확대 (Increased hit radius: 52 -> 75)
        const offset = 40; // 플레이어 중심쪽으로 더 가까이 (Reduced offset: 60 -> 40)
        const swingArc = 40 * (Math.PI / 180);

        // 컨테이너를 플레이어 위치에 생성하여 회전축으로 사용 (Container as pivot point at player position)
        const swingContainer = this.scene.add.container(player.x, player.y);

        const knife = this.scene.add.sprite(offset, 0, "knife");
        knife.setOrigin(0.5, 0.9); // 칼 손잡이 부분
        knife.setScale(0.06); // 칼 크기 2/3로 축소 (0.09 -> 0.06)
        knife.setFlipX(true);
        // 칼날이 바깥쪽을 향하도록 조정 (Point blade away from player)
        knife.rotation = Math.PI / 2;

        swingContainer.add(knife);
        swingContainer.setDepth(player.depth + 5);

        // 시작 각도 설정
        swingContainer.rotation = angle - swingArc;

        this.scene.tweens.add({
            targets: swingContainer,
            rotation: angle + swingArc,
            alpha: { from: 1, to: 0 },
            duration: 150,
            onComplete: () => {
                swingContainer.destroy();
                this.isMeleeing = false;
            }
        });

        // --- 충돌 감지 (Hit Detection) ---
        // 기존 반경(radius=75) 내에서 각도 차이(+/- 40도) 체크
        if (this.scene.monstersSystem && this.scene.monstersSystem.monsters) {
            this.scene.monstersSystem.monsters.children.each((monster) => {
                const dist = Phaser.Math.Distance.Between(player.x, player.y, monster.x, monster.y);
                if (dist <= radius + 20) { // 몬스터 반지름 20
                    const targetAngle = Phaser.Math.Angle.Between(player.x, player.y, monster.x, monster.y);
                    const diff = Phaser.Math.Angle.Wrap(targetAngle - angle);

                    if (Math.abs(diff) < 40 * (Math.PI / 180)) { // +/- 40도 범위 (Match visual 80 deg swing)
                        console.log("몬스터 근접 공격 적중!", monster.id);

                        // 서버로 적중 메시지 전송 (Report melee hit to server)
                        if (this.scene.myUserId) {
                            this.scene.sendMonsterHit({
                                userId: this.scene.myUserId,
                                userX: player.x,
                                userY: player.y,
                                monsterId: monster.id,
                                monsterX: monster.x,
                                monsterY: monster.y,
                                isKnife: true
                            });
                        }
                    }
                }
            });
        }
    }

    update(delta) {
        // Local UI update removed
    }

    fireBullet(x, y, targetX, targetY) {
        if (this.isReloading) return;

        // 탄약 감소 (Decrease Ammo)
        this.ammo--;
        this.scene.game.events.emit('update-ammo', this.ammo, this.maxAmmo);

        // 발사 사운드 재생 (Play Fire Sound)
        this.scene.sound.play("assault_rifle", { volume: soundManager.getVolume("sfx") * 0.5 });

        const angle = this.scene.lookAngle; // scene의 일관된 조준 각도 사용 (Use consistent lookAngle from scene)

        // 총구 위치 계산 (Calculate muzzle position)
        const muzzleX = x + Math.cos(angle) * GUN_MUZZLE_OFFSET;
        const muzzleY = y + Math.sin(angle) * GUN_MUZZLE_OFFSET;

        // 원형 대신 스프라이트 생성 (Create sprite instead of circle)
        const bullet = this.scene.add.sprite(muzzleX, muzzleY, "bullet");
        this.scene.physics.add.existing(bullet);

        bullet.setScale(0.025); // 크기 절반으로 축소 (0.05 -> 0.025)
        bullet.setOrigin(0.5, 0.5);
        bullet.rotation = angle + Math.PI / 2; // 'top'이 날아가는 방향이 되게 90도(PI/2) 보정

        // 물리 바디는 여전히 원형으로 설정하여 충돌 판정 정확도 유지
        bullet.body.setCircle(5);
        bullet.body.setCollideWorldBounds(false);

        const bulletSpeed = BULLET_SPEED;
        const maxDistance = this.scene.isAiming ? BULLET_RANGE_AIMING : BULLET_RANGE_NORMAL;

        const vx = Math.cos(angle) * bulletSpeed;
        const vy = Math.sin(angle) * bulletSpeed;

        // 벽과의 충돌 처리 (Collide with walls)
        if (this.scene.obstacles) {
            this.scene.physics.add.collider(bullet, this.scene.obstacles, (b) => {
                b.destroy();
            });
        }

        const timeToLive = (maxDistance / bulletSpeed) * 1000;
        this.scene.time.addEvent({
            delay: timeToLive,
            callback: () => bullet.destroy(),
        });

        this.bullets.add(bullet);
        bullet.body.setVelocity(vx, vy);
    }

    spawnPartyBullet(data) {
        // 본인의 총알은 이미 로컬에서 발사되었으므로 중복 생성을 방지합니다. (Avoid duplicate bullet for local player)
        if (data.userId === this.scene.myUserId) return;

        const { muzzleX, muzzleY, x, y, angle, range } = data;
        const speed = BULLET_SPEED;
        const maxDistance = range || BULLET_RANGE_NORMAL;

        // muzzleX/Y가 있으면 사용하고, 없으면 기존 x/y 사용 (Fallback to x/y if muzzleX/Y missing)
        const spawnX = muzzleX !== undefined ? muzzleX : x;
        const spawnY = muzzleY !== undefined ? muzzleY : y;

        const bullet = this.scene.add.sprite(spawnX, spawnY, "bullet");
        this.scene.physics.add.existing(bullet);

        bullet.setScale(0.025);
        bullet.rotation = angle + Math.PI / 2;
        bullet.setTint(0xff8888); // 파티원 총알 색상 구분

        bullet.body.setCircle(5);

        // 중요: 그룹에 먼저 추가한 후 속도를 설정해야 합니다. (Add to group BEFORE setting velocity)
        this.partyBullets.add(bullet);

        bullet.body.setVelocity(
            Math.cos(angle) * speed,
            Math.sin(angle) * speed
        );

        if (this.scene.obstacles) {
            this.scene.physics.add.collider(bullet, this.scene.obstacles, () => {
                bullet.destroy();
            });
        }

        this.scene.time.addEvent({
            delay: (maxDistance / speed) * 1000,
            callback: () => bullet.destroy(),
        });
    }
}
