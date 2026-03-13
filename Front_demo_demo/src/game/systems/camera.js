import Phaser from "phaser";

export default class CameraSystem {
    constructor(scene) {
        this.scene = scene;
        this.walkZoom = 2;
        this.runZoom = 1.95;
        this.aimZoom = 1.5;
    }

    create(player) {
        this.scene.cameras.main.startFollow(player, true, 0.08, 0.08);

        // Use the scene's physics world bounds (set in createMap)
        const worldBounds = this.scene.physics.world.bounds;
        this.scene.cameras.main.setBounds(0, 0, worldBounds.width, worldBounds.height);

        this.scene.cameras.main.setZoom(this.walkZoom);
    }

    update(delta) {
        const player = this.scene.player;
        if (!player) return;

        let targetZoom = player.isRunning ? this.runZoom : this.walkZoom;
        if (this.scene.isAiming) {
            targetZoom = this.aimZoom;
        }

        // 🔥 Cheat: Zoom Out
        if (this.scene.zoomCheatActive) {
            targetZoom = 0.5;
        } 
        // 🔥 Boss Mode: 10% Wider View
        else if (this.scene.isBossActive) {
            // User requested 10% wider view.
            // FOV ~ 1/Zoom. 1.1x FOV -> Zoom / 1.1.
            targetZoom = this.walkZoom / 1.1;
        }

        this.scene.cameras.main.zoom = Phaser.Math.Linear(
            this.scene.cameras.main.zoom,
            targetZoom,
            player.runLockTime > 0 ? 0.1 : 0.05
        );
    }
}
