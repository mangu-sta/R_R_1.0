import Phaser from "phaser";

export default class UIScene extends Phaser.Scene {
    constructor() {
        super("UIScene");
    }

    create() {
        // --- HP Bar ---
        this.hpBarBg = this.add.rectangle(
            50,
            50,
            220,
            30,
            0x333333
        ).setOrigin(0, 0);

        this.hpBar = this.add.rectangle(
            50,
            50,
            220,
            30,
            0x00ff00
        ).setOrigin(0, 0);

        // --- Stamina Bar ---
        this.staminaBarBg = this.add.rectangle(
            50,
            90,
            220,
            20,
            0x222222
        ).setOrigin(0, 0);

        this.staminaBar = this.add.rectangle(
            50,
            90,
            220,
            20,
            0x00bfff
        ).setOrigin(0, 0);

        // --- Arm Stamina Bar ---
        this.armStaminaBarBg = this.add.rectangle(
            50,
            115,
            220,
            10,
            0x222222
        ).setOrigin(0, 0);

        this.armStaminaBar = this.add.rectangle(
            50,
            115,
            220,
            10,
            0x8b4513 // Brown
        ).setOrigin(0, 0);

        // --- Level Text ---
        this.levelText = this.add.text(
            50,
            20,
            "Lv.0",
            {
                fontSize: "24px",
                fill: "#ffffff",
                fontFamily: "Arial",
                stroke: "#000000",
                strokeThickness: 4,
            }
        ).setOrigin(0, 0).setDepth(1000);

        this.nicknameText = this.add.text(
            120, // To the right of Lv.1
            20,
            "",
            {
                fontSize: "24px",
                fill: "#ffffff",
                fontFamily: "Arial",
                stroke: "#000000",
                strokeThickness: 4,
            }
        ).setOrigin(0, 0);

        // --- EXP Bar ---
        // Bottom center
        const width = this.scale.width;
        const height = this.scale.height;

        this.expBarBg = this.add.rectangle(
            50,
            height - 30,
            width - 100,
            10,
            0x333333
        ).setOrigin(0, 0).setDepth(1000);

        this.expBar = this.add.rectangle(
            50,
            height - 30,
            0,
            10,
            0xffff00
        ).setOrigin(0, 0);
        this.expBar.setDepth(1001);

        // --- Ammo Text ---
        // Bottom Right
        this.ammoText = this.add.text(
            width - 50,
            height - 50,
            "30 / 30",
            {
                fontSize: "32px",
                fill: "#ffffff",
                fontFamily: "Arial",
                stroke: "#000000",
                strokeThickness: 4,
            }
        ).setOrigin(1, 1);

        // --- Fire Mode Text ---
        this.fireModeText = this.add.text(
            width - 200, // Left of ammoText
            height - 50,
            "AUTO",
            {
                fontSize: "20px",
                fill: "#00bfff",
                fontFamily: "Arial",
                stroke: "#000000",
                strokeThickness: 3,
            }
        ).setOrigin(1, 1);

        // --- Reload Prompt ---
        this.reloadPrompt = this.add.text(
            width - 50,
            height - 90,
            "[ Press R ]",
            {
                fontSize: "24px",
                fill: "#ff0000",
                fontFamily: "Arial",
                stroke: "#000000",
                strokeThickness: 4,
            }
        ).setOrigin(1, 1).setVisible(false);

        // Blink Animation for Prompt
        this.tweens.add({
            targets: this.reloadPrompt,
            alpha: 0,
            duration: 500,
            yoyo: true,
            repeat: -1
        });

        // Handle Resize
        this.scale.on('resize', this.resize, this);

        // --- Event Listeners ---
        const game = this.game;

        game.events.on('update-hp', this.updateHp, this);
        game.events.on('update-stamina', this.updateStamina, this);
        game.events.on('update-arm-stamina', this.updateArmStamina, this);
        game.events.on('update-exp', this.updateExp, this);
        game.events.on('update-ammo', this.updateAmmo, this); // New Event
        game.events.on('update-fire-mode', this.updateFireMode, this);
        game.events.on('start-reload', this.onStartReload, this);
        game.events.on('cancel-reload', this.onCancelReload, this);
        game.events.on('init-ui', this.initUI, this);
        game.events.on('init-ui', this.initUI, this);
        game.events.on('show-message', this.showMessage, this);
        game.events.on('show-boss-result', this.showBossResult, this);
        game.events.on('show-game-over', this.showGameOver, this);

        this.reloadGraphics = this.add.graphics();
        this.reloadGraphics.setDepth(100);
        this.reloadProgress = 0;
        this.isReloading = false;
        this.reloadTween = null;

        // Setup cleanup
        this.events.on('shutdown', () => {
            game.events.off('update-hp', this.updateHp, this);
            game.events.off('update-stamina', this.updateStamina, this);
            game.events.off('update-arm-stamina', this.updateArmStamina, this);
            game.events.off('update-exp', this.updateExp, this);
            game.events.off('update-ammo', this.updateAmmo, this); // New Event
            game.events.off('update-fire-mode', this.updateFireMode, this);
            game.events.off('start-reload', this.onStartReload, this);
            game.events.off('cancel-reload', this.onCancelReload, this);
            game.events.off('init-ui', this.initUI, this);
            game.events.off('init-ui', this.initUI, this);
            game.events.off('show-message', this.showMessage, this);
            game.events.off('show-boss-result', this.showBossResult, this);
            game.events.off('show-game-over', this.showGameOver, this);
        });
    }

    resize(gameSize) {
        if (!this.expBarBg) return;

        const width = gameSize.width;
        const height = gameSize.height;

        this.expBarBg.setPosition(50, height - 30);
        this.expBarBg.width = width - 100;

        this.expBar.setPosition(50, height - 30);

        if (this.ammoText) {
            this.ammoText.setPosition(width - 50, height - 50);
            if (this.fireModeText) {
                this.fireModeText.setPosition(width - 200, height - 50);
            }
        }

        if (this.reloadPrompt) {
            this.reloadPrompt.setPosition(width - 50, height - 90);
        }
    }

    initUI(data) {
        // data: { hp, maxHp, stamina, maxStamina, exp, maxExp, level }
        if (data.hp !== undefined) this.updateHp(data.hp, data.maxHp);
        if (data.stamina !== undefined) this.updateStamina(data.stamina, data.maxStamina);
        if (data.exp !== undefined) this.updateExp(data.exp, data.maxExp, data.level);
        if (data.ammo !== undefined) this.updateAmmo(data.ammo, data.maxAmmo);
        if (data.nickname !== undefined) this.updateNickname(data.nickname);
    }

    updateHp(hp, maxHp) {
        if (!this.hpBar || !this.hpBarBg) return;

        // 스탯 4(MaxHp 108)일 때 220px이 되도록 비율 조정
        const barWidth = maxHp * (220 / 108);
        this.hpBarBg.width = barWidth;

        const percent = Phaser.Math.Clamp(hp / maxHp, 0, 1);
        this.hpBar.width = barWidth * percent;

        if (percent < 0.3) {
            this.hpBar.fillColor = 0xff0000;
        } else {
            this.hpBar.fillColor = 0x00ff00;
        }
    }

    updateStamina(stamina, maxStamina) {
        if (!this.staminaBar) return;
        const percent = Phaser.Math.Clamp(stamina / maxStamina, 0, 1);
        this.staminaBar.width = 220 * percent;
        this.staminaBar.fillColor = percent < 0.2 ? 0xffaa00 : 0x00bfff;
    }

    updateArmStamina(stamina, maxStamina) {
        if (!this.armStaminaBar) return;
        const percent = Phaser.Math.Clamp(stamina / maxStamina, 0, 1);
        this.armStaminaBar.width = 220 * percent;
    }

    updateExp(exp, maxExp, level) {
        if (!this.expBar) return;

        if (level !== undefined) {
            this.levelText.setText(`Lv.${level}`); 
        }

        const width = this.scale.width - 100;
        let percent = Phaser.Math.Clamp(exp / maxExp, 0, 1);
        if (isNaN(percent)) percent = 0;

        this.expBar.width = width * percent;
        console.log(`📊 [UI] Exp Update: ${exp}/${maxExp} (${(percent*100).toFixed(1)}%) | Level: ${level}`);
    }

    updateNickname(nickname) {
        if (!this.nicknameText) return;
        this.nicknameText.setText(nickname);
    }

    updateAmmo(current, max) {
        if (!this.ammoText) return;
        this.ammoText.setText(`${current} / ${max}`);

        // Color Interpolation (White to Red)
        // Simple threshold approach or full interpolation?
        // Let's do thresholds for now as requested "colder to red as it gets closer to 0"

        const ratio = current / max;
        if (ratio <= 0 && !this.isReloading) {
            this.ammoText.setColor("#ff0000"); // Red
            if (this.reloadPrompt) this.reloadPrompt.setVisible(true);
        } else if (ratio <= 0.3) {
            this.ammoText.setColor("#ff4444");
            if (this.reloadPrompt) this.reloadPrompt.setVisible(false);
        } else if (ratio <= 0.5) {
            this.ammoText.setColor("#ffaaaa");
            if (this.reloadPrompt) this.reloadPrompt.setVisible(false);
        } else {
            this.ammoText.setColor("#ffffff");
            if (this.reloadPrompt) this.reloadPrompt.setVisible(false);
        }
    }

    updateFireMode(isAuto) {
        if (!this.fireModeText) return;
        this.fireModeText.setText(isAuto ? "AUTO" : "SINGLE");
        this.fireModeText.setColor(isAuto ? "#00bfff" : "#ffffff");
    }

    onStartReload(duration) {
        this.isReloading = true;
        if (this.reloadPrompt) this.reloadPrompt.setVisible(false);
        this.reloadProgress = 0;

        if (this.reloadTween) {
            this.reloadTween.stop();
        }

        this.reloadTween = this.tweens.add({
            targets: this,
            reloadProgress: 1,
            duration: duration,
            onUpdate: () => {
                this.drawReloadCircle();
            },
            onComplete: () => {
                this.isReloading = false;
                this.reloadTween = null;
                if (this.reloadGraphics) {
                    this.reloadGraphics.clear();
                }
            }
        });
    }

    onCancelReload() {
        this.isReloading = false;
        if (this.reloadTween) {
            this.reloadTween.stop();
            this.reloadTween = null;
        }
        if (this.reloadGraphics) {
            this.reloadGraphics.clear();
        }
        // updateAmmo logic will handle showing [ Press R ] if ammo is 0
    }

    drawReloadCircle() {
        if (!this.reloadGraphics) return;

        const { width, height } = this.scale;
        const x = width - 70;  // Ammo UI 상단 (Above Ammo UI)
        const y = height - 120; // Ammo UI 상단 (Above Ammo UI)

        this.reloadGraphics.clear();

        // 배경 원 (Background Circle)
        this.reloadGraphics.lineStyle(4, 0xffffff, 0.3);
        this.reloadGraphics.strokeCircle(x, y, 20);

        // 진행 바 (Progress Arc)
        this.reloadGraphics.lineStyle(4, 0x00bfff, 1);
        const startAngle = Phaser.Math.DegToRad(-90);
        const endAngle = Phaser.Math.DegToRad(-90 + (this.reloadProgress * 360));

        this.reloadGraphics.beginPath();
        this.reloadGraphics.arc(x, y, 20, startAngle, endAngle, false);
        this.reloadGraphics.strokePath();
    }

    // 화면 중앙에 메시지 표시
    showMessage(text) {
        const { width, height } = this.scale;

        const msgText = this.add.text(width / 2, height / 2, text, {
            fontSize: "32px",
            fill: "#00ffff",
            fontFamily: "Arial",
            stroke: "#000000",
            strokeThickness: 6,
            align: "center"
        }).setOrigin(0.5).setDepth(2000);

        // 애니메이션 효과 후 제거
        this.tweens.add({
            targets: msgText,
            y: height / 2 - 50,
            alpha: 0,
            delay: 1500,
            duration: 1000,
            onComplete: () => msgText.destroy()
        });
    }

    showBossResult(data) {
        const { width, height } = this.scale;
        
        // Background
        this.bossResultContainer = this.add.container(width / 2, height / 2).setDepth(3000);
        
        const bg = this.add.rectangle(0, 0, 500, 400, 0x000000, 0.9);
        bg.setStrokeStyle(4, 0xffd700);
        
        const title = this.add.text(0, -150, "🏆 BOSS CLEARED!", {
            fontSize: "40px",
            color: "#ffd700",
            fontStyle: "bold",
            stroke: "#000000",
            strokeThickness: 6
        }).setOrigin(0.5);

        const playersText = data.players ? data.players.join(", ") : data.killer;
        const info = this.add.text(0, -30, 
            `Players: ${playersText}\n\nTime: ${data.timeTaken}s`, {
            fontSize: "24px",
            color: "#ffffff",
            align: "center",
            wordWrap: { width: 450 },
            stroke: "#000000",
            strokeThickness: 3
        }).setOrigin(0.5);

        // LOBBY Button
        const btn = this.add.rectangle(0, 120, 180, 50, 0x28a745).setInteractive({ useHandCursor: true });
        const btnText = this.add.text(0, 120, "GO TO LOBBY", {
            fontSize: "24px", color: "#ffffff", fontStyle: "bold"
        }).setOrigin(0.5);

        btn.on('pointerdown', () => {
            this.bossResultContainer.destroy();
            // 🔥 로비로 복귀
            window.dispatchEvent(new CustomEvent("return-to-lobby"));
        });

        this.bossResultContainer.add([bg, title, info, btn, btnText]);
    }

    showGameOver() {
        const { width, height } = this.scale;
        
        const text = this.add.text(width/2, height/2, "💀 GAME OVER 💀", {
            fontSize: "64px",
            color: "#ff0000",
            fontStyle: "bold",
            stroke: "#000000",
            strokeThickness: 8
        }).setOrigin(0.5).setDepth(4000).setAlpha(0);

        this.tweens.add({
            targets: text,
            alpha: 1,
            duration: 1000,
            yoyo: false,
            hold: 3000,
            onComplete: () => {
                text.destroy();
                // 🔥 로비로 복귀
                window.dispatchEvent(new CustomEvent("return-to-lobby"));
            }
        });
    }

    fetchAndShowRanking() {
        // Fetch ranking from API
        fetch('/api/rank/boss')
            .then(res => res.json())
            .then(data => {
                this.showRankingUI(data);
            })
            .catch(err => {
                console.error("Failed to fetch ranking", err);
                this.showMessage("Failed to load ranking.");
            });
    }

    showRankingUI(rankingData) {
        const { width, height } = this.scale;
        
        this.rankingContainer = this.add.container(width / 2, height / 2).setDepth(3000);
        
        const bg = this.add.rectangle(0, 0, 500, 600, 0x111111, 0.9);
        bg.setStrokeStyle(4, 0x00ffff);
        
        const title = this.add.text(0, -250, "🏆 HALL OF FAME 🏆", {
            fontSize: "32px",
            color: "#00ffff",
            fontStyle: "bold"
        }).setOrigin(0.5);

        let yPos = -200;
        const listItems = [];
        
        // Header
        const header = this.add.text(-220, yPos, "Rank   Nickname             Time", {
             fontSize: "20px", color: "#aaaaaa"
        });
        listItems.push(header);
        yPos += 40;

        rankingData.forEach((record, index) => {
            const rank = index + 1;
            const line = `${rank}.${" ".repeat(3 - rank.toString().length)}   ${record.nickname.padEnd(15)}    ${record.timeTakenSeconds}s`;
            const item = this.add.text(-220, yPos, line, {
                fontSize: "20px",
                color: index < 3 ? "#ffd700" : "#ffffff",
                fontFamily: "monospace"
            });
            listItems.push(item);
            yPos += 30;
        });

        // Close Button
        const btn = this.add.rectangle(0, 250, 150, 50, 0xff0000).setInteractive({ useHandCursor: true });
        const btnText = this.add.text(0, 250, "CLOSE", {
            fontSize: "24px", color: "#ffffff"
        }).setOrigin(0.5);

        btn.on('pointerdown', () => {
            this.rankingContainer.destroy();
        });

        this.rankingContainer.add([bg, title, ...listItems, btn, btnText]);
    }

}
