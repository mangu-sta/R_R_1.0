import Phaser from "phaser";
import MovementSystem from "../systems/movement";
import StaminaSystem from "../systems/stamina";
import CameraSystem from "../systems/camera";
import VisionSystem from "../systems/vision";
import MonstersSystem from "../systems/monsters";
import PartyPlayersSystem from "../systems/partyPlayers";
import CombatSystem from "../systems/combat";
import UIScene from "./UIScene";
import NetworkHandlers from "../network/handlers";

import assaultRifleAudio from "../../assets/music/assaultrifle.wav";
import emptyGunShotAudio from "../../assets/music/empty-gun-shot.wav";
import reloadAudio from "../../assets/music/reload.mp3";
import stepAudio from "../../assets/music/step.wav";

import { MAIN_WORLD_WIDTH, MAIN_WORLD_HEIGHT, TILE_SIZE, BULLET_RANGE_NORMAL, BULLET_RANGE_AIMING, GUN_MUZZLE_OFFSET } from "../constants";
import keyMapping from "../utils/keyMapping";

export default class MainStageScene extends Phaser.Scene {
    constructor() {
        super("MainStageScene");
    }

    init(data) {
        if (data) {
            this.myUserId = data.myUserId;
            this.myCharacterId = data.myCharacterId;
            this.myJob = data.myJob;
            this.sendSender = data.sendSender;
            this.roomId = data.roomId;
            this.spawnX = data.posX;
            this.spawnY = data.posY;
        }
    }

    preload() {
        this.load.json("mainMapData", "/assets/json/main_map.json");

        // 오디오 리소스 (MainScene과 동일)
        this.load.audio("assault_rifle", assaultRifleAudio);
        this.load.audio("empty_gun", emptyGunShotAudio);
        this.load.audio("reload", reloadAudio);
        this.load.audio("step", stepAudio);
        this.load.image("ak47", "/assets/img/guns/AK-47.png");
        this.load.image("bullet", "/assets/img/Ammo/bullet-7.62mm.png");
        this.load.image("knife", "/assets/img/knife/knife.png");

        // Character Sprites
        this.load.image("SOLDIER", "/assets/img/character/Soldier.png");
        this.load.image("FIREFIGHTER", "/assets/img/character/FireFighter.png");
        this.load.image("REPORTER", "/assets/img/character/Reporter.png");
        this.load.image("DOCTOR", "/assets/img/character/Docter.png");
    }

    create() {
        // Initialize scene state (Keep init data)
        this.keys = keyMapping.getKeys();

        // 키 업데이트 이벤트 리스너
        window.addEventListener("keys-updated", (e) => {
            this.keys = e.detail;
            this.updateAimKey();
        });

        this.input.mouse.disableContextMenu();
        window.oncontextmenu = (e) => e.preventDefault();
        this.input.setDefaultCursor('url(/assets/img/crosshair.svg) 16 16, crosshair');
        this.isAiming = false;
        this.lookAngle = 0;
        this.transitionActive = false; // 메인 스테이지에서는 필요 시 다른 용도로 사용 가능

        this.netInput = {
            up: false,
            down: false,
            left: false,
            right: false,
            isRunning: false
        };

        // 맵 및 월드 크기 설정 (메인 스테이지 크기 적용)
        this.createMap();

        // 시스템 초기화
        this.movementSystem = new MovementSystem(this);
        this.staminaSystem = new StaminaSystem(this);
        this.cameraSystem = new CameraSystem(this);
        this.visionSystem = new VisionSystem(this);
        this.monstersSystem = new MonstersSystem(this);
        this.partyPlayersSystem = new PartyPlayersSystem(this);
        this.combatSystem = new CombatSystem(this);
        this.networkHandlers = new NetworkHandlers(this);

        this.createObstacles();
        this.createPlayer();

        // 시스템별 create 호출
        this.visionSystem.create();
        this.monstersSystem.create();
        this.combatSystem.create();
        this.partyPlayersSystem.create();
        this.cameraSystem.create(this.player);

        // UI 씬 재시작 (혹은 초기화)
        if (!this.scene.isActive("UIScene")) {
            this.scene.launch("UIScene");
        }

        // 초기 UI 데이터 연동
        this.time.delayedCall(100, () => {
            if (this.player) {
                this.game.events.emit('init-ui', {
                    hp: this.player.hp,
                    maxHp: this.player.maxHp,
                    stamina: this.player.stamina,
                    maxStamina: this.player.maxStamina,
                    exp: this.player.exp,
                    maxExp: this.player.maxExp,
                    level: this.player.level,
                    nickname: this.player.nickname || ""
                });
            }
        });

        this.sceneReady = true;
        window.dispatchEvent(new CustomEvent("phaser-ready", { detail: this }));

        console.log("🎮 MainStageScene Created (Large Map)");

        // Aiming Listeners (Mouse)
        this.input.on('pointerdown', (pointer) => {
            if (this.isAimKeyMouse()) {
                const aimKey = this.keys.aim;
                const isAimClick = (aimKey === "LeftClick" && pointer.button === 0) ||
                    (aimKey === "RightClick" && pointer.button === 2);
                if (isAimClick) this.setAiming(true);
            }
        });

        this.input.on('pointerup', (pointer) => {
            if (this.isAimKeyMouse()) {
                const aimKey = this.keys.aim;
                const isAimClick = (aimKey === "LeftClick" && pointer.button === 0) ||
                    (aimKey === "RightClick" && pointer.button === 2);
                if (isAimClick) this.setAiming(false);
            }
        });

        this.updateAimKey();
    }

    isAimKeyMouse() {
        return this.keys.aim === "LeftClick" || this.keys.aim === "RightClick";
    }

    updateAimKey() {
        if (this.aimKeyObj) {
            this.aimKeyObj.destroy();
            this.aimKeyObj = null;
        }

        if (!this.isAimKeyMouse() && this.input.keyboard) {
            this.aimKeyObj = this.input.keyboard.addKey(this.keys.aim);
            this.aimKeyObj.on('down', () => this.setAiming(true));
            this.aimKeyObj.on('up', () => this.setAiming(false));
        }
    }

    setAiming(isAiming) {
        if (isAiming && this.player && this.player.isRunning) return;
        this.isAiming = isAiming;
        this.input.setDefaultCursor(isAiming ?
            'url(/assets/img/crosshair_aim.svg) 16 16, crosshair' :
            'url(/assets/img/crosshair.svg) 16 16, crosshair'
        );
    }

    update(time, delta) {
        if (!this.player) return;

        if (this.player.isRunning === undefined) this.player.isRunning = false;
        if (this.player.body) this.player.rotation = this.lookAngle;
        if (this.gun) {
            this.gun.setPosition(this.player.x, this.player.y);
            this.gun.rotation = this.lookAngle;
        }

        this.pointer = this.input.activePointer;

        this.movementSystem.update(delta);
        this.staminaSystem.update(delta);
        this.cameraSystem.update(delta);

        // 네트워크 위치 동기화 (10Hz)
        const now = this.time.now;
        if (!this.lastMoveSentTime) this.lastMoveSentTime = 0;
        if (now - this.lastMoveSentTime >= 1000 / 10) {
            this.lastMoveSentTime = now;
            this.sendMove();
        }

        this.updateRotation(this.pointer);
        this.monstersSystem.update();
        this.visionSystem.update();
        this.partyPlayersSystem.update(delta);
        this.combatSystem.update(delta);

        // 🔥 메인 플레이어 닉네임 라벨 위치 업데이트
        if (this.player.nameText) {
            this.player.nameText.setPosition(this.player.x, this.player.y - 45);
        }
    }

    createMap() {
        const map = this.add.graphics();
        this.physics.world.setBounds(0, 0, MAIN_WORLD_WIDTH, MAIN_WORLD_HEIGHT);

        for (let y = 0; y < MAIN_WORLD_HEIGHT; y += 32) {
            for (let x = 0; x < MAIN_WORLD_WIDTH; x += 32) {
                map.fillStyle((x + y) % 64 === 0 ? 0x242424 : 0x2a2a2a); // 메인 스테이지는 약간 더 어두운 톤
                map.fillRect(x, y, 32, 32);
            }
        }
    }

    createObstacles() {
        this.obstacles = this.physics.add.staticGroup();
        const obstacleData = this.cache.json.get("mainMapData");
        if (obstacleData) {
            obstacleData.forEach((data) => {
                const rect = this.add.rectangle(
                    data.x + data.width / 2,
                    data.y + data.height / 2,
                    data.width,
                    data.height,
                    0xff0000,
                    0 // 투명하게 설정 (필요시 시각화 가능)
                );
                this.physics.add.existing(rect, true);
                this.obstacles.add(rect);
            });
        }
    }

    createPlayer() {
        // 메인 스테이지 시작 위치 (서버 데이터 우선, 없으면 중앙)
        const jobTexture = this.myJob || "SOLDIER";
        const spawnX = this.spawnX !== undefined ? this.spawnX : MAIN_WORLD_WIDTH / 2;
        const spawnY = this.spawnY !== undefined ? this.spawnY : MAIN_WORLD_HEIGHT / 2;
        this.player = this.add.sprite(spawnX, spawnY, jobTexture);
        this.physics.add.existing(this.player);

        // Physics body setup
        const bodyRadius = this.player.width * 0.45; // 90% Width coverage in texture space
        this.player.body.setCircle(bodyRadius);
        this.player.body.setOffset(
            (this.player.width / 2) - bodyRadius,
            (this.player.height / 2) - bodyRadius
        );

        this.player.setDisplaySize(40, 40); // 원본 동그라미 크기(지름 40)에 맞춤
        this.player.body.setCollideWorldBounds(true);
        this.player.body.setBounce(0.2);

        // 스테이터스 설정 (MainScene과 동일하게 초기화되지만 Tutorial 이후라면 갱신 필요)
        this.player.stamina = 100;
        this.player.maxStamina = 100;
        this.player.staminaRegen = 25;
        this.player.staminaDrain = 40;
        this.player.staminaCooldown = 0;
        this.player.runLockTime = 0;
        this.player.nickname = "";
        this.player.hp = 100;
        this.player.maxHp = 100;
        this.player.level = undefined; // ✅ 초기화 (undefined로 설정하여 첫 패킷 무시 보정)
        this.player.exp = 0;   // ✅ 초기화
        this.player.maxExp = 100; // ✅ 초기화

        this.player.armStamina = 100;
        this.player.maxArmStamina = 100;
        this.player.armStaminaRegen = 20;
        this.player.armStaminaDrain = 20;
        this.player.armStaminaCooldown = 0;
        this.player.armStaminaLockTime = 0;

        if (this.obstacles) {
            this.physics.add.collider(this.player, this.obstacles);
        }

        // 🔥 닉네임 라벨 (Main Player Name Tag)
        this.player.nameText = this.add.text(this.player.x, this.player.y - 45, "", {
            fontSize: "14px",
            color: "#ffffff",
            stroke: "#000000",
            strokeThickness: 3
        }).setOrigin(0.5).setDepth(100);

        this.gun = this.add.sprite(0, 0, "ak47");
        this.gun.setOrigin(0.2, 0.5);
        this.gun.setScale(0.0733); // 크기 조절 (Resized: 0.11 -> 0.0733)
        this.gun.setDepth(this.player.depth + 1);
    }

    updateRotation(pointer) {
        if (!this.player || !this.player.active) return;
        const worldPoint = this.cameras.main.getWorldPoint(pointer.x, pointer.y);
        const angle = Phaser.Math.Angle.Between(this.player.x, this.player.y, worldPoint.x, worldPoint.y);
        this.lookAngle = angle;
        this.player.setRotation(this.lookAngle);

        const now = this.time.now;
        if (!this.lastAngleSentTime) this.lastAngleSentTime = 0;
        if (now - this.lastAngleSentTime >= 1000 / 30) {
            this.lastAngleSentTime = now;
            this.sendRotation();
        }
    }

    // 서버 통신 Helper (MainScene과 동일)
    setNetworkSender(callback) { this.sendSender = callback; }
    sendRotation() {
        if (!this.sendSender || !this.myUserId || !this.roomId) return;

        this.sendSender(`/app/game/${this.roomId}/rotate`, {
            userId: this.myUserId,
            angle: this.lookAngle,
            timestamp: Date.now(),
        });
    }
    sendMove() {
        if (!this.sendSender || !this.myUserId || !this.roomId) {
            if (this.time.now % 1000 < 50) {
                console.warn("⚠️ Cannot send move: ", {
                    hasSender: !!this.sendSender,
                    myUserId: this.myUserId,
                    roomId: this.roomId
                });
            }
            return;
        }

        if (this.time.now % 1000 < 100) {
            console.log(`[MainStageScene] Sending Move: userId=${this.myUserId}, x=${this.player.x.toFixed(1)}, y=${this.player.y.toFixed(1)}`);
        }

        this.sendSender(`/app/game/${this.roomId}/move`, {
            userId: this.myUserId,
            input: this.netInput,
            x: this.player.x,
            y: this.player.y,
            timestamp: Date.now(),
        });
    }
    sendInput() { this.sendMove(); }
    sendFire() {
        if (!this.sendSender || !this.player) return;

        const range = this.isAiming ? BULLET_RANGE_AIMING : BULLET_RANGE_NORMAL;

        // 총구 위치 계산 (Calculate muzzle position)
        const muzzleX = this.player.x + Math.cos(this.lookAngle) * GUN_MUZZLE_OFFSET;
        const muzzleY = this.player.y + Math.sin(this.lookAngle) * GUN_MUZZLE_OFFSET;

        const targetX = muzzleX + Math.cos(this.lookAngle) * range;
        const targetY = muzzleY + Math.sin(this.lookAngle) * range;

        this.sendSender(`/app/game/${this.roomId}/fire`, {
            userId: this.myUserId,
            playerX: this.player.x,
            playerY: this.player.y,
            muzzleX: muzzleX,
            muzzleY: muzzleY,
            angle: this.lookAngle,
            range: range,
            targetX: targetX,
            targetY: targetY
        });
    }

    sendMonsterHit(data) {
        if (!this.sendSender || !this.roomId) return;
        this.sendSender(`/app/game/${this.roomId}/monster-hit`, data);
    }
    handleGameMessage(msg) { this.networkHandlers.handleMessage(msg); }
    createPartyPlayer(user, x, y) { this.partyPlayersSystem.createPartyPlayer(user, x, y); }
    spawnPartyBullet(data) { this.combatSystem.spawnPartyBullet(data); }
    spawnMonster(data) { this.monstersSystem.spawnMonster(data); }
}
