// src/MainScene.js
import Phaser from "phaser";

export default class MainScene extends Phaser.Scene {
  constructor() {
    super("MainScene");
  }

  preload() {
    this.load.image("player", "https://i.imgur.com/Z6X5Q3G.png"); // 임시 플레이어 이미지
  }

  create() {
    // 플레이어 생성
    this.player = this.physics.add
      .sprite(400, 300, "player")
      .setScale(0.5)
      .setCollideWorldBounds(true);

    // 키 입력
    this.keys = this.input.keyboard.addKeys("W,A,S,D");

    // 마우스 커서 보이게
    this.input.setDefaultCursor("crosshair");
  }

  update() {
    // WASD 이동
    const speed = 200;
    this.player.setVelocity(0);

    if (this.keys.W.isDown) this.player.setVelocityY(-speed);
    if (this.keys.S.isDown) this.player.setVelocityY(speed);
    if (this.keys.A.isDown) this.player.setVelocityX(-speed);
    if (this.keys.D.isDown) this.player.setVelocityX(speed);

    // 마우스 방향으로 회전
    const pointer = this.input.activePointer;
    const angle = Phaser.Math.Angle.Between(
      this.player.x,
      this.player.y,
      pointer.worldX,
      pointer.worldY
    );
    this.player.setRotation(angle);
  }
}
