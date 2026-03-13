export default class UISystem {
    constructor(scene) {
        this.scene = scene;
    }

    create() {
        this.scene.player.maxHp = 10;
        this.scene.player.hp = 10;
        this.scene.player.level = 1;
        this.scene.player.exp = 0;
        this.scene.player.maxExp = 100;

        // Level Text
        this.scene.levelText = this.scene.add.text(
            20,
            20,
            `Lv.${this.scene.player.level}`,
            {
                fontSize: "24px",
                fill: "#ffffff",
                fontFamily: "Arial",
                stroke: "#000000",
                strokeThickness: 4,
            }
        );

        // HP Bar
        this.scene.hpBarBg = this.scene.add.rectangle(
            window.innerWidth / 2 - 260,
            window.innerHeight - 565,
            220,
            15,
            0x333333
        );

        this.scene.hpBar = this.scene.add.rectangle(
            window.innerWidth / 2 - 370,
            window.innerHeight - 565,
            220,
            15,
            0x00ff00
        );
        this.scene.hpBar.setOrigin(0, 0.5);

        // Stamina Bar
        this.scene.staminaBarBg = this.scene.add.rectangle(
            window.innerWidth / 2 - 260,
            window.innerHeight - 540,
            220,
            10,
            0x222222
        );

        this.scene.staminaBar = this.scene.add.rectangle(
            window.innerWidth / 2 - 370,
            window.innerHeight - 540,
            220,
            10,
            0x00bfff
        );
        this.scene.staminaBar.setOrigin(0, 0.5);

        // EXP Bar (Bottom of screen)
        this.scene.expBarBg = this.scene.add.rectangle(
            window.innerWidth / 2,
            window.innerHeight - 20,
            window.innerWidth - 100,
            10,
            0x333333
        );

        this.scene.expBar = this.scene.add.rectangle(
            50, // Starts from left margin
            window.innerHeight - 20,
            0, // Initial width 0
            10,
            0xffff00 // Yellow for EXP
        );
        this.scene.expBar.setOrigin(0, 0.5);


        // Common Settings
        [
            this.scene.hpBarBg,
            this.scene.hpBar,
            this.scene.staminaBarBg,
            this.scene.staminaBar,
            this.scene.levelText,
            this.scene.expBarBg,
            this.scene.expBar
        ].forEach((ui) => {
            ui.setScrollFactor(0);
            ui.setDepth(2000);
            ui.setMask(null);
        });
    }

    updateExp(exp, maxExp, level) {
        if (!this.scene.expBar) return;

        this.scene.player.exp = exp;
        this.scene.player.maxExp = maxExp;
        this.scene.player.level = level;

        // Update Level Text
        this.scene.levelText.setText(`Lv.${level}`);

        // Update EXP Bar
        const maxWidth = window.innerWidth - 100;
        let percent = Phaser.Math.Clamp(exp / maxExp, 0, 1);
        if (isNaN(percent)) percent = 0;

        this.scene.expBar.width = maxWidth * percent;
    }
}
