import Phaser from "phaser";
import MovementSystem from "../systems/movement";
import StaminaSystem from "../systems/stamina";
import CameraSystem from "../systems/camera";
import VisionSystem from "../systems/vision";
import MonstersSystem from "../systems/monsters";
import PartyPlayersSystem from "../systems/partyPlayers";
import CombatSystem from "../systems/combat";
import UIScene from "./UIScene"; // Import UIScene
import NetworkHandlers from "../network/handlers";

// Audio Imports
import assaultRifleAudio from "../../assets/music/assaultrifle.wav";
import emptyGunShotAudio from "../../assets/music/empty-gun-shot.wav";
import reloadAudio from "../../assets/music/reload.mp3";
import stepAudio from "../../assets/music/step.wav";
// import gunAudio from "../../assets/music/gun.wav"; // Not requested in logic yet

// ... lines 11-12
import { WORLD_WIDTH, WORLD_HEIGHT, TILE_SIZE, BULLET_RANGE_NORMAL, BULLET_RANGE_AIMING, GUN_MUZZLE_OFFSET } from "../constants";
import keyMapping from "../utils/keyMapping";

export default class MainScene extends Phaser.Scene {
    constructor() {
        super("MainScene");
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
        this.load.json("obstaclesData", "/assets/json/tutorial_map.json");
        // this.load.image("player", "./assets/img/pmc_ex.png");

        // Audio
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
        this.roomId = null;
        this.myUserId = null;
        this.sendSender = null; // Callback for sending data to server
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
        this.transitionActive = false; // 스테이지 전환 활성화 플래그
        this.netInput = {
            up: false,
            down: false,
            left: false,
            right: false,
            isRunning: false
        };

        // Map Setup (Could be a MapSystem, but simple enough to keep here or move if needed)
        this.createMap();

        // Initialize Systems
        this.movementSystem = new MovementSystem(this);
        this.staminaSystem = new StaminaSystem(this);
        this.cameraSystem = new CameraSystem(this);
        this.visionSystem = new VisionSystem(this);
        this.monstersSystem = new MonstersSystem(this);
        this.partyPlayersSystem = new PartyPlayersSystem(this);
        this.combatSystem = new CombatSystem(this);
        // this.uiSystem = new UISystem(this); // REMOVED
        this.networkHandlers = new NetworkHandlers(this);

        // Order matters for layering and dependency

        // 1. Obstacles (Vision system needs this, and Player collider needs this)
        this.createObstacles();

        // 2. Create Player (Movement system expects player)
        this.createPlayer();

        // 3. System Create Calls
        this.visionSystem.create(); // Needs obstacles
        this.monstersSystem.create(); // <--- Moved UP (Before Combat)
        this.combatSystem.create();   // <--- Now Combat can see Monsters Group
        this.cameraSystem.create(this.player);

        // 🔥 마우스 좌표 확인 치트 (Shift + Z + X)
        this.input.keyboard.on('keydown', (event) => {
            if (event.shiftKey && event.code === 'KeyX' && this.input.keyboard.checkDown(this.input.keyboard.addKey(Phaser.Input.Keyboard.KeyCodes.Z))) {
                const worldX = Math.floor(this.input.activePointer.worldX);
                const worldY = Math.floor(this.input.activePointer.worldY);
                console.info(`📍 Mouse World Position: X=${worldX}, Y=${worldY}`);
                // Assuming 'toast' is globally available or imported elsewhere
                if (typeof toast !== 'undefined') {
                    toast.info(`📍 현재 위치: X=${worldX}, Y=${worldY}`, {
                        position: "top-center",
                        autoClose: 2000
                    });
                }
            }
        });
        this.partyPlayersSystem.create();

        // 4. 스테이지 전환 트리거 생성 (Stage Transition Trigger)
        this.createTransitionTrigger();

        // 🔥 Launch UI Scene
        this.scene.launch("UIScene");

        // 🔥 Initial UI Data Emit
        // Wait briefly for UIScene to be ready, or emit repeatedly in update or rely on events
        this.time.delayedCall(100, () => {
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

            // Sync nickname label if exists
            if (this.player.nameText) {
                this.player.nameText.setText(this.player.nickname || "");
            }
        });

        // Expose scene event for React
        this.sceneReady = true;
        window.dispatchEvent(new CustomEvent("phaser-ready", { detail: this }));

        console.log("🎮 MainScene Created");

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

        if (!this.isAimKeyMouse()) {
            this.aimKeyObj = this.input.keyboard.addKey(this.keys.aim);
            this.aimKeyObj.on('down', () => this.setAiming(true));
            this.aimKeyObj.on('up', () => this.setAiming(false));
        }
    }

    setAiming(isAiming) {
        if (isAiming && this.player && this.player.isRunning) {
            return;
        }

        this.isAiming = isAiming;
        if (isAiming) {
            this.input.setDefaultCursor('url(/assets/img/crosshair_aim.svg) 16 16, crosshair');
        } else {
            this.input.setDefaultCursor('url(/assets/img/crosshair.svg) 16 16, crosshair');
        }
    }

    update(time, delta) {
        if (!this.player) return;

        // Default isRunning to false if undefined
        if (this.player.isRunning === undefined) {
            this.player.isRunning = false;
        }

        if (this.player && this.player.body) {
            this.player.rotation = this.lookAngle;
        }

        if (this.gun) {
            this.gun.setPosition(this.player.x, this.player.y);
            this.gun.rotation = this.lookAngle; // Assuming 'angle' refers to this.lookAngle
        }
        // Common Pointer
        this.pointer = this.input.activePointer;

        // Update Systems
        this.movementSystem.update(delta);
        this.staminaSystem.update(delta);
        this.cameraSystem.update(delta);

        // Throttle Move Send (10Hz - 10 times per second)
        const now = this.time.now;
        if (!this.lastMoveSentTime) this.lastMoveSentTime = 0;
        const MOVE_TICK = 1000 / 10;

        if (now - this.lastMoveSentTime >= MOVE_TICK) {
            this.lastMoveSentTime = now;
            this.sendMove();
        }

        // Update Rotation (Logic from Tutorial.jsx updateRotation)
        this.updateRotation(this.pointer);

        this.monstersSystem.update();
        this.visionSystem.update();
        this.partyPlayersSystem.update(delta);
        this.combatSystem.update(delta);

        // Update main player name tag position
        if (this.player.nameText) {
            this.player.nameText.setPosition(this.player.x, this.player.y - 45);
        }
    }

    // ===================================
    // Helper Methods (Some logic still here or delegated)
    // ===================================

    createMap() {
        const map = this.add.graphics();
        const GRID_WIDTH = WORLD_WIDTH / TILE_SIZE;
        const GRID_HEIGHT = WORLD_HEIGHT / TILE_SIZE;

        this.physics.world.setBounds(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        for (let y = 0; y < WORLD_HEIGHT; y += 32) {
            for (let x = 0; x < WORLD_WIDTH; x += 32) {
                map.fillStyle((x + y) % 64 === 0 ? 0x303030 : 0x383838);
                map.fillRect(x, y, 32, 32);
            }
        }
    }

    createObstacles() {
        this.obstacles = this.physics.add.staticGroup();
        const obstacleData = this.cache.json.get("obstaclesData");
        if (obstacleData) {
            obstacleData.forEach((data) => {
                const rect = this.add.rectangle(
                    data.x + data.width / 2,
                    data.y + data.height / 2,
                    data.width,
                    data.height,
                    0xff0000,
                    0
                );
                this.physics.add.existing(rect, true);
                this.obstacles.add(rect);
            });
        }
    }

    // 다음 스테이지 트리거 생성
    createTransitionTrigger() {
        // 좌측 하단 문 위치 보정 (플레이어가 닿을 수 있는 위치로 조정)
        const x = 60;
        const y = 1660;
        const width = 60;
        const height = 80;

        // 시각적 표현 (문 느낌의 파란색 영역)
        this.transitionZone = this.add.rectangle(x, y, width, height, 0x00ffff, 0.3);
        this.physics.add.existing(this.transitionZone, true);

        // 글로우 효과 (Tween)
        this.tweens.add({
            targets: this.transitionZone,
            fillAlpha: 0.6,
            duration: 1000,
            yoyo: true,
            repeat: -1
        });

        // "MAIN STAGE" 텍스트 추가
        this.add.text(x + 20, y, "MAIN STAGE", {
            fontSize: "14px",
            fill: "#00ffff",
            stroke: "#000000",
            strokeThickness: 2
        }).setOrigin(0, 0.5);

        // 플레이어와의 오버랩 설정
        this.physics.add.overlap(this.player, this.transitionZone, () => {
            if (this.transitionActive) return;
            this.transitionActive = true;

            console.log("메인 스테이지 진입 시도 -> 파티원 수락 요청 전송");

            // React로 "스테이지 이동 요청" 이벤트 발송
            window.dispatchEvent(new CustomEvent("request-stage-advance"));

            // 쿨타임 (중복 트리거 방지)
            this.time.delayedCall(5000, () => {
                this.transitionActive = false;
            });
        });
    }

    createPlayer() {
        const jobTexture = this.myJob || "SOLDIER";
        const spawnX = this.spawnX !== undefined ? this.spawnX : 915.5;
        const spawnY = this.spawnY !== undefined ? this.spawnY : 180.25;
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

        this.player.stamina = 100;
        this.player.maxStamina = 100;
        this.player.staminaRegen = 25;
        this.player.staminaDrain = 40;
        this.player.staminaCooldown = 0;
        this.player.runLockTime = 0;
        this.player.nickname = "";
        this.player.hp = 100;
        this.player.maxHp = 100;
        this.player.level = undefined;
        this.player.exp = 0;
        this.player.maxExp = 100;

        this.player.armStamina = 100;
        this.player.maxArmStamina = 100;
        this.player.armStaminaRegen = 20;
        this.player.armStaminaDrain = 20; // 5 seconds use
        this.player.armStaminaCooldown = 0;
        this.player.armStaminaLockTime = 0;

        // 닉네임 라벨 (Main Player Name Tag)
        this.player.nameText = this.add.text(this.player.x, this.player.y - 45, "", {
            fontSize: "14px",
            color: "#ffffff",
            stroke: "#000000",
            strokeThickness: 3
        }).setOrigin(0.5).setDepth(100);

        // Collider setup (delegated or here)
        if (this.obstacles) {
            this.physics.add.collider(this.player, this.obstacles);
        }

        // Visual Gun
        this.gun = this.add.sprite(0, 0, "ak47");
        this.gun.setOrigin(0.2, 0.5); // Pivot at handle (adjusted for typical gun sprites)
        this.gun.setScale(0.0733); // 크기 조절 (Resized: 0.11 -> 0.0733)
        this.gun.setDepth(this.player.depth + 1); // Above player
    }

    updateRotation(pointer) {
        if (!this.player || !this.player.active) return;

        // Use World Coordinates for rotation
        // Force recalculation from screen coordinates to handle camera movement even if mouse is stationary
        const worldPoint = this.cameras.main.getWorldPoint(pointer.x, pointer.y);

        const angle = Phaser.Math.Angle.Between(
            this.player.x,
            this.player.y,
            worldPoint.x,
            worldPoint.y
        );

        this.lookAngle = angle;
        this.player.setRotation(this.lookAngle);

        // Throttle send
        const now = this.time.now;
        if (!this.lastAngleSentTime) this.lastAngleSentTime = 0;
        const ANGLE_TICK = 1000 / 30;

        if (now - this.lastAngleSentTime >= ANGLE_TICK) {
            this.lastAngleSentTime = now;
            this.sendRotation();
        }
    }

    // API exposed to Systems
    sendInput() {
        this.sendMove();
    }

    sendRotation() {
        if (!this.sendSender || !this.myUserId || !this.roomId) return;

        this.sendSender(`/app/game/${this.roomId}/rotate`, {
            userId: this.myUserId,
            angle: this.lookAngle,
            timestamp: Date.now(),
        });
    }

    sendMove() {
        if (!this.sendSender) return;
        if (!this.myUserId || !this.roomId) return;

        this.sendSender(`/app/game/${this.roomId}/move`, {
            userId: this.myUserId,
            input: this.netInput, // populated by MovementSystem
            x: this.player.x,
            y: this.player.y,
            timestamp: Date.now(),
        });
    }

    sendFire() {
        if (!this.sendSender || !this.player || !this.roomId) return;

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

    // API exposed to React
    setNetworkSender(callback) {
        this.sendSender = callback;
    }

    createPartyPlayer(user, x, y) {
        this.partyPlayersSystem.createPartyPlayer(user, x, y);
    }

    spawnPartyBullet(data) {
        this.combatSystem.spawnPartyBullet(data);
    }

    spawnMonster(data) {
        this.monstersSystem.spawnMonster(data);
    }

    handleGameMessage(msg) {
        this.networkHandlers.handleMessage(msg);
    }
}
