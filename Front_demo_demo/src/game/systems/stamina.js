import Phaser from "phaser";
import {
    STAMINA_EMPTY_LOCK,
    STAMINA_STOP_DELAY,
    RUN_START_PERCENT,
} from "../constants";

export default class StaminaSystem {
    constructor(scene) {
        this.scene = scene;
        this.staminaBar = null;
        this.staminaBarBg = null;
    }

    create() {
        // UI 생성은 createUI.js에서 처리되지만, 참조가 필요함
        // 아니면 여기서 스태미나 바를 생성할 수도 있음?
        // 기존 코드는 `createUI`에서 UI를 생성했음
        // `this.scene.staminaBar`를 통해 바에 접근함
    }

    update(delta) {
        const player = this.scene.player;
        if (!player) return;

        // 타이머 업데이트
        player.staminaCooldown = Math.max(0, player.staminaCooldown - delta);
        player.runLockTime = Math.max(0, player.runLockTime - delta);

        const canStartRun =
            player.stamina >= player.maxStamina * RUN_START_PERCENT &&
            player.runLockTime === 0;

        // MovementSystem에서 scene.isMoving을 설정함
        // 달리기를 시작할 수 있는지 확인 (스태미나가 일정 비율 이상이고 락이 걸리지 않은 상태)
        const isMoving = this.scene.isMoving;
        const shiftDown = this.scene.movementSystem?.shiftKey?.isDown;

        // 달리기 시작
        if (!player.isRunning && shiftDown && isMoving && canStartRun) {
            player.isRunning = true;
        }

        // 달리기 중지
        if (
            !shiftDown ||
            !isMoving ||
            player.stamina <= 0 ||
            player.runLockTime > 0
        ) {
            player.isRunning = false;
        }

        // 스태미나 값 업데이트
        if (player.isRunning) {
            player.stamina -= (player.staminaDrain * delta) / 1000;
            player.staminaCooldown = STAMINA_STOP_DELAY;

            if (player.stamina <= 0) {
                player.stamina = 0;
                player.runLockTime = STAMINA_EMPTY_LOCK;
                player.staminaCooldown = STAMINA_EMPTY_LOCK;
            }
        } else if (player.staminaCooldown === 0) {
            player.stamina += (player.staminaRegen * delta) / 1000;
        }

        // --- 팔 스태미나 로직 ---
        player.armStaminaCooldown = Math.max(0, player.armStaminaCooldown - delta);
        player.armStaminaLockTime = Math.max(0, player.armStaminaLockTime - delta);

        if (this.scene.isAiming) {
            player.armStamina -= (player.armStaminaDrain * delta) / 1000;
            player.armStaminaCooldown = STAMINA_STOP_DELAY; // 조준 해제 후 짧은 대기

            if (player.armStamina <= 0) {
                player.armStamina = 0;
                player.armStaminaLockTime = STAMINA_EMPTY_LOCK; // 완전히 소모된 후의 긴 대기
                player.armStaminaCooldown = STAMINA_EMPTY_LOCK;
                this.scene.setAiming(false); // 자동 취소
            }
        } else if (player.armStaminaCooldown === 0 && player.armStaminaLockTime === 0) {
            player.armStamina += (player.armStaminaRegen * delta) / 1000;
        }

        player.stamina = Phaser.Math.Clamp(player.stamina, 0, player.maxStamina);
        player.armStamina = Phaser.Math.Clamp(player.armStamina, 0, player.maxArmStamina);

        // UI 업데이트
        this.scene.game.events.emit('update-stamina', player.stamina, player.maxStamina);
        this.scene.game.events.emit('update-arm-stamina', player.armStamina, player.maxArmStamina);

        // 네트워크 동기화
        if (this.scene.netInput) {
            this.scene.netInput.isRunning = player.isRunning;
        }
    }
}
