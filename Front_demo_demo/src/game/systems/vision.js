import Phaser from "phaser";

export default class VisionSystem {
    constructor(scene) {
        this.scene = scene;
        this.visionGraphics = null;
        this.visionMask = null;
        this.darkness = null;
        this.wallSegments = [];
        this.cheatActivated = false; // 치트 중복 실행 방지 플래그 (Toggle Vision)
        this.visionRangeCheat = false;
        this.rangeCheatActivated = false; // 치트 중복 실행 방지 플래그 (Range x4)
        this.wasBossActive = false; // 보스 상태 변경 감지용
    }

    create() {
        // 벽 세그먼트 캐싱
        this.wallSegments = [];
        if (this.scene.obstacles) {
            this.scene.obstacles.children.each((wall) => {
                const b = wall.getBounds();
                this.wallSegments.push(
                    new Phaser.Geom.Line(b.left, b.top, b.right, b.top),
                    new Phaser.Geom.Line(b.right, b.top, b.right, b.bottom),
                    new Phaser.Geom.Line(b.right, b.bottom, b.left, b.bottom),
                    new Phaser.Geom.Line(b.left, b.bottom, b.left, b.top)
                );
            });
        }

        // 어둠 오버레이 (Darkness Overlay)
        this.darkness = this.scene.add.rectangle(
            0,
            0,
            this.scene.scale.width,
            this.scene.scale.height,
            0x000000,
            1
        );
        this.darkness.setOrigin(0);
        this.darkness.setScrollFactor(0);
        this.darkness.setDepth(1000);

        // 시야 그래픽 (Vision Graphics)
        this.visionGraphics = this.scene.add.graphics();
        this.visionGraphics.setScrollFactor(0);
        this.visionGraphics.setDepth(1001);
        this.visionGraphics.visible = false;

        // 마스크 (Mask)
        this.visionMask = this.visionGraphics.createGeometryMask();
        this.visionMask.invertAlpha = true;
        this.darkness.setMask(this.visionMask);

        // 키 등록 (매 프레임 생성 방지)
        const k = this.scene.input.keyboard;
        this.keys = {
            shift: k.addKey(Phaser.Input.Keyboard.KeyCodes.SHIFT),
            p: k.addKey(Phaser.Input.Keyboard.KeyCodes.P),
            o: k.addKey(Phaser.Input.Keyboard.KeyCodes.O),
            i: k.addKey(Phaser.Input.Keyboard.KeyCodes.I),
            u: k.addKey(Phaser.Input.Keyboard.KeyCodes.U),
            h: k.addKey(Phaser.Input.Keyboard.KeyCodes.H),
            j: k.addKey(Phaser.Input.Keyboard.KeyCodes.J),
            k: k.addKey(Phaser.Input.Keyboard.KeyCodes.K),
            l: k.addKey(Phaser.Input.Keyboard.KeyCodes.L)
        };
    }

    toggleVision() {
        if (!this.darkness) return;

        const isVisible = !this.darkness.visible; // Current state before toggle is "Darkness Visible" (isCheatOff)
        // If darkness.visible is true, we are turning it OFF (Cheat ON) -> show everything
        // If darkness.visible is false, we are turning it ON (Cheat OFF) -> hide things

        // Toggle Darkness
        this.darkness.setVisible(!this.darkness.visible);

        const cheatActive = !this.darkness.visible;

        console.log(`🕶 Vision Toggle: ${cheatActive ? "ON (Reveal)" : "OFF (Normal)"}`);

        // Toggle Obstacles Visibility
        if (this.scene.obstacles) {
            this.scene.obstacles.children.each((obstacle) => {
                // If cheat is active, make walls visible (alpha 0.5)
                // If cheat is inactive, make walls invisible (alpha 0)
                if (obstacle.setFillStyle) {
                    obstacle.setFillStyle(0xff0000, cheatActive ? 0.5 : 0);
                }
            });
        }
    }

    toggleVisionRange() {
        this.visionRangeCheat = !this.visionRangeCheat;
        // Share state with CameraSystem via Scene
        this.scene.zoomCheatActive = this.visionRangeCheat;
        console.log(`🔭 Vision Range x4 & Zoom Out: ${this.visionRangeCheat ? "ON" : "OFF"}`);
    }

    update() {
        if (!this.keys) return;

        const k = this.scene.input.keyboard;
        const shift = this.keys.shift.isDown;

        // 🔥 Cheat Code 1: Shift + P + O + I + U (Vision Toggle)
        const p = this.keys.p.isDown;
        const o = this.keys.o.isDown;
        const i = this.keys.i.isDown;
        const u = this.keys.u.isDown;

        if (shift && p && o && i && u) {
            if (!this.cheatActivated) {
                this.toggleVision();
                this.cheatActivated = true;
            }
        } else {
            this.cheatActivated = false;
        }

        // 🔥 Cheat Code 2: Shift + H + J + K + L (Range x4)
        const h = this.keys.h.isDown;
        const j = this.keys.j.isDown;
        const kKey = this.keys.k.isDown;
        const l = this.keys.l.isDown;

        if (shift && h && j && kKey && l) {
            if (!this.rangeCheatActivated) {
                this.toggleVisionRange();
                this.rangeCheatActivated = true;
            }
        } else {
            this.rangeCheatActivated = false;
        }

        if (!this.scene.player || !this.visionGraphics) return;

        // 보스전 상태 변경 감지 (State Change Detection)
        if (this.scene.isBossActive !== this.wasBossActive) {
            this.wasBossActive = this.scene.isBossActive;

            // 보스전 진입 시 (Enter Boss Mode)
            if (this.scene.isBossActive) {
                this.darkness.setVisible(false);
                if (this.scene.obstacles) {
                    this.scene.obstacles.children.each((obstacle) => {
                        if (obstacle.setFillStyle) {
                            obstacle.setFillStyle(0xff0000, 0.5);
                        }
                    });
                }
            } 
            // 보스전 종료 시 (Exit Boss Mode) -> 원래대로 복구 (Optional, or wait for next manual toggle)
            else {
                 this.darkness.setVisible(true);
                 if (this.scene.obstacles) {
                    this.scene.obstacles.children.each((obstacle) => {
                        if (obstacle.setFillStyle) {
                            // Reset to invisible/default
                            // Note: original code didn't have a specific reset for obstacles other than toggleVision logic.
                            // Assuming we want to hide them again.
                            obstacle.setFillStyle(0xff0000, 0); 
                        }
                    });
                }
            }
        }

        // If darkness is hidden, we don't need to update the vision graphics mask
        if (!this.darkness.visible) {
            this.visionGraphics.clear();
            return;
        }

        this.visionGraphics.clear();

        const cam = this.scene.cameras.main;
        const origin = { x: this.scene.player.x, y: this.scene.player.y };
        // scene.lookAngle은 카메라나 입력을 통해 업데이트되어야 함.
        // Tutorial.jsx에서는 MainScene의 updateRotation에서 이를 설정했음.
        // 씬이나 이동 시스템에 의해 업데이트되는 `this.scene.lookAngle`을 사용함.

        const baseAngle = this.scene.lookAngle || 0;
        const coneAngle = this.scene.isAiming ? Phaser.Math.DegToRad(30) : Phaser.Math.DegToRad(60);

        const multiplier = this.visionRangeCheat ? 4 : 1;
        const circleRadius = 90 * multiplier;
        const coneRadius = (this.scene.isAiming ? 640 : 400) * multiplier;

        const rays = 180;
        const step = (Math.PI * 2) / rays;

        this.visionGraphics.fillStyle(0xffffff, 1);
        this.visionGraphics.beginPath();
        this.visionGraphics.moveTo(
            origin.x - cam.scrollX,
            origin.y - cam.scrollY
        );

        for (let i = 0; i <= rays; i++) {
            const angle = step * i;

            let radius = circleRadius;
            const delta = Phaser.Math.Angle.Wrap(angle - baseAngle);
            if (Math.abs(delta) <= coneAngle / 2) {
                radius = coneRadius;
            }

            const ray = new Phaser.Geom.Line(
                origin.x,
                origin.y,
                origin.x + Math.cos(angle) * radius,
                origin.y + Math.sin(angle) * radius
            );

            let closest = null;
            let minDist = radius;

            for (const seg of this.wallSegments) {
                const hit = Phaser.Geom.Intersects.GetLineToLine(ray, seg);
                if (hit) {
                    const d = Phaser.Math.Distance.Between(
                        origin.x,
                        origin.y,
                        hit.x,
                        hit.y
                    );
                    if (d < minDist) {
                        minDist = d;
                        closest = hit;
                    }
                }
            }

            const x = closest ? closest.x : origin.x + Math.cos(angle) * radius;
            const y = closest ? closest.y : origin.y + Math.sin(angle) * radius;

            this.visionGraphics.lineTo(x - cam.scrollX, y - cam.scrollY);
        }

        this.visionGraphics.closePath();
        this.visionGraphics.fillPath();
    }
}
