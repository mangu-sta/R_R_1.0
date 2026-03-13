import Phaser from "phaser";
import { BASE_SPEED_DEFAULT, RUN_SPEED_DEFAULT } from "../constants";
import keyMapping from "../utils/keyMapping";
import soundManager from "../utils/soundManager";

export default class MovementSystem {
    constructor(scene) {
        this.scene = scene;
        this.keys = keyMapping.getKeys();
        this.cursors = scene.input.keyboard.addKeys({
            up: this.keys.moveUp,
            down: this.keys.moveDown,
            left: this.keys.moveLeft,
            right: this.keys.moveRight,
        });

        this.shiftKey = scene.input.keyboard.addKey(this.keys.run);

        this.isMoving = false;
        this.stepSound = null;
        this.setupInputListeners();

        // 키 업데이트 이벤트 리스너 (Cleanup을 위해 참조 저장)
        this.onKeysUpdated = (e) => {
            this.updateKeyBindings(e.detail);
        };
        window.addEventListener("keys-updated", this.onKeysUpdated);

        // 사운드 업데이트 이벤트 리스너
        this.onSoundUpdated = () => {
            if (this.stepSound) {
                this.stepSound.setVolume(soundManager.getVolume("sfx") * 0.5);
            }
        };
        window.addEventListener("sound-settings-updated", this.onSoundUpdated);

        // Scene 종료 시 리스너 해제
        this.scene.events.once("shutdown", () => this.cleanup());
        this.scene.events.once("destroy", () => this.cleanup());
    }

    cleanup() {
        window.removeEventListener("keys-updated", this.onKeysUpdated);
        window.removeEventListener("sound-settings-updated", this.onSoundUpdated);
    }

    updateKeyBindings(newKeys) {
        // Scene이 없거나 input 시스템이 없으면 중단 (방어 코드)
        if (!this.scene || !this.scene.input || !this.scene.input.keyboard) return;

        this.keys = newKeys;

        // Remove old cursors if possible
        // Note: Phaser keys don't have a simple 'remove' from this.cursors object, but we can overwrite.
        // Previously created keys (this.cursors.up etc) are generic Key objects. 
        // We can just create new ones. Old ones might linger in plugin but it's okay.

        this.cursors = this.scene.input.keyboard.addKeys({
            up: this.keys.moveUp,
            down: this.keys.moveDown,
            left: this.keys.moveLeft,
            right: this.keys.moveRight,
        });
        if (this.shiftKey) {
            this.scene.input.keyboard.removeKey(this.keys.run); // Try to remove old key capture if needed
            // Actually removeKey logic is tricky, usually just re-adding works or removeCapture.
        }
        this.shiftKey = this.scene.input.keyboard.addKey(this.keys.run);
    }

    setupInputListeners() {
        // We will move the netInput update logic to the Update loop 
        // to avoid key binding sync issues with events.
    }

    update(delta) {
        if (!this.scene.player || !this.scene.player.body) return;

        // Sync netInput with current key states for proper network sync
        this.scene.netInput.up = this.cursors.up.isDown;
        this.scene.netInput.down = this.cursors.down.isDown;
        this.scene.netInput.left = this.cursors.left.isDown;
        this.scene.netInput.right = this.cursors.right.isDown;
        this.scene.netInput.isRunning = this.shiftKey.isDown;

        // Handle aiming/running conflict
        if (this.scene.netInput.isRunning && this.scene.isAiming) {
            this.scene.setAiming(false);
        }

        let dx = 0;
        let dy = 0;

        const up = this.scene.netInput.up;
        const down = this.scene.netInput.down;
        const left = this.scene.netInput.left;
        const right = this.scene.netInput.right;

        if (left) dx -= 1;
        if (right) dx += 1;
        if (up) dy -= 1;
        if (down) dy += 1;

        const vec = new Phaser.Math.Vector2(dx, dy);
        this.isMoving = vec.length() > 0;
        this.scene.isMoving = this.isMoving; // StaminaSystem에서 사용할 수 있도록 노출

        // window.playerStats는 React (Tutorial.jsx)에서 설정됨
        const base = window.playerStats?.baseSpeed ?? BASE_SPEED_DEFAULT;
        const run = window.playerStats?.runSpeed ?? RUN_SPEED_DEFAULT;

        let speed = this.scene.player.isRunning ? run : base;

        if (this.scene.isAiming) {
            speed *= 0.5; // 조준 중 50% 이동 속도 페널티
        }

        if (this.isMoving) {
            vec.normalize().scale(speed);
            this.scene.player.body.setVelocity(vec.x, vec.y);

            // 발소리 재생 (Step Sound)
            if (!this.stepSound) {
                this.stepSound = this.scene.sound.add("step", { loop: true, volume: soundManager.getVolume("sfx") * 0.5 });
            }
            if (!this.stepSound.isPlaying) {
                this.stepSound.play();
            }

            // Adjust Speed/Pitch based on running
            if (this.scene.player.isRunning) {
                this.stepSound.setRate(1.5); // 달릴 때 더 빠른 발소리
            } else {
                this.stepSound.setRate(1.0); // 보통 발소리
            }
        } else {
            this.scene.player.body.setVelocity(0, 0);

            // 발소리 중지
            if (this.stepSound && this.stepSound.isPlaying) {
                this.stepSound.stop();
            }
        }
    }
}
