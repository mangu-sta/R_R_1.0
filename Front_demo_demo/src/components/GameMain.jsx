import React, { useEffect, useRef, useState } from "react";
import Phaser from "phaser";
import { toast } from "react-toastify";
import { flushSync } from "react-dom";
import axios from "axios";

import MainStageScene from "../game/scenes/MainStageScene";
import UIScene from "../game/scenes/UIScene";
import { connectSocket } from "../game/network/socket";
import StatSelection from "./StatSelection";
import Option from "./Option";
import GameChat from "./GameChat";
import "../css/App.css";

export default function GameMain() {
  const [gameReady, setGameReady] = useState(false);
  const [sceneReady, setSceneReady] = useState(false);
  const [showStatSelection, setShowStatSelection] = useState(false);
  const [showOption, setShowOption] = useState(false);

  const [myCharacter, setMyCharacter] = useState({});
  const [userList, setUserList] = useState([]);
  const [roomId, setRoomId] = useState("");
  const [toggle, setToggle] = useState(false);
  const [messages, setMessages] = useState([]);
  const [isChatOpen, setIsChatOpen] = useState(false);

  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const sceneRef = useRef(null);

  const [baseSpeed, setBaseSpeed] = useState(100);
  const [runSpeed, setRunSpeed] = useState(150);

  const safeSend = (destination, body) => {
    const client = stompClientRef.current;
    if (!client || !client.connected) return;
    client.send(destination, {}, JSON.stringify(body));
  };

  // Sync Data to Phaser (Same as Tutorial.jsx)
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !sceneReady || !myCharacter?.userId) return;

    scene.myUserId = myCharacter.userId;
    scene.myJob = myCharacter.job; // 🔥 직업 정보 저장
    scene.setNetworkSender(safeSend);

    if (scene.player) {
      const status = myCharacter.status ? JSON.parse(myCharacter.status) : {};
      const agility = status.AGILITY || 0;
      const healthStat = status.HEALTH || 0;
      const reloadStat = status.RELOAD || 0;

      const calculatedMaxHp = 100 + healthStat * 2;
      const calculatedBaseSpeed = 100 + agility * 2;
      const calculatedRunSpeed = calculatedBaseSpeed * 1.5;

      if (myCharacter.job) {
        scene.player.setTexture(myCharacter.job);
        scene.player.setDisplaySize(40, 40); // 🔥 텍스트 변경 후 크기 재설정
      }
      scene.player.level = myCharacter.level || 0;
      scene.player.exp = myCharacter.exp || 0;
      scene.player.maxExp = (scene.player.level || 1) * 100;
      scene.player.nickname = myCharacter.nickname || "";
      scene.player.reloadStat = reloadStat;
      scene.player.maxHp = calculatedMaxHp;

      if (scene.player.hp === undefined || scene.player.hp === 10 || scene.player.hp === 100) {
        scene.player.hp = calculatedMaxHp;
      }

      setBaseSpeed(calculatedBaseSpeed);
      setRunSpeed(calculatedRunSpeed);

      if (scene.game) {
        scene.game.events.emit('init-ui', {
          hp: scene.player.hp,
          maxHp: scene.player.maxHp,
          stamina: scene.player.stamina,
          maxStamina: scene.player.maxStamina,
          exp: scene.player.exp,
          maxExp: scene.player.maxExp,
          level: scene.player.level,
          nickname: myCharacter.nickname
        });
      }
    }
  }, [sceneReady, myCharacter]);

  useEffect(() => {
    const load = async () => {
      try {
        const response = await axios.post("/api/lobby/me", {}, { withCredentials: true });
        setMyCharacter(response.data.myCharacter);
        setRoomId(response.data.roomId);
        setUserList(response.data.members);
        setGameReady(true);
      } catch (error) {
        console.error("데이터 로드 실패:", error);
      }
    };
    load();
  }, [toggle]);

  useEffect(() => {
    window.playerStats = { baseSpeed, runSpeed };
  }, [baseSpeed, runSpeed]);

  useEffect(() => {
    if (!roomId) return;
    connect(roomId);
    return () => disconnect();
  }, [roomId]);

  const connect = (targetRoomId) => {
    if (stompClientRef.current?.connected) return;
    stompClientRef.current = connectSocket({
      roomId: targetRoomId,
      onConnect: (client, subscription) => {
        if (subscriptionRef.current) subscriptionRef.current.unsubscribe();
        subscriptionRef.current = subscription;
      },
      onMessage: (chat) => {
        switch (chat.type) {
          case "ACCEPT":
          case "LEAVE":
          case "KICK":
            flushSync(() => {
              setToggle((prev) => !prev);
            });
            break;
          case "MESSAGE":
            setMessages((prev) => [...prev, chat.value]);
            break;
          default:
            break;
        }

        // Phaser 씬에 메시지 전달
        if (sceneRef.current) {
          sceneRef.current.handleGameMessage(chat);
        }
        // 끄기: 너무 빈번한 메시지들 필터링
        const silentTypes = ["MOVE", "ROTATE", "MONSTER_STATE", "PLAYER_HP_UPDATE", "BULLET", "FIRE", "HP"];
        if (!silentTypes.includes(chat.type)) {
          console.log("💬 새 메시지:", chat);
        }
      },
    });
  };

  const disconnect = () => {
    if (stompClientRef.current) {
      if (subscriptionRef.current) subscriptionRef.current.unsubscribe();
      stompClientRef.current.disconnect();
    }
  };

  useEffect(() => {
    const onReady = (e) => {
      sceneRef.current = e.detail;
      setSceneReady(true);
    };
    const onLevelUp = () => setShowStatSelection(true);
    window.addEventListener("phaser-ready", onReady);
    window.addEventListener("level-up", onLevelUp);
    return () => {
      window.removeEventListener("phaser-ready", onReady);
      window.removeEventListener("level-up", onLevelUp);
    };
  }, []);

  // 🔥 ESC & Enter 키 리스너
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        if (showStatSelection) {
          setShowStatSelection(false);
        } else {
          setShowOption((prev) => !prev);
        }
      }
    };
    const handleEnterKey = (e) => {
      if (e.key === "Enter" && !showOption && !showStatSelection) {
        setIsChatOpen((prev) => !prev);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keydown", handleEnterKey);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keydown", handleEnterKey);
    };
  }, [showStatSelection, showOption]);

  // 🔥 채팅창 열림에 따른 Phaser 입력 제어
  useEffect(() => {
    if (sceneRef.current?.input?.keyboard) {
      if (isChatOpen) {
        sceneRef.current.input.keyboard.enabled = false;
        // 이동 멈춤 처리 (키를 떼 행위가 무시될 수 있으므로 초기화)
        if (sceneRef.current.netInput) {
          sceneRef.current.netInput.up = false;
          sceneRef.current.netInput.down = false;
          sceneRef.current.netInput.left = false;
          sceneRef.current.netInput.right = false;
        }
      } else {
        sceneRef.current.input.keyboard.enabled = true;
      }
    }
  }, [isChatOpen]);

  const onSendChat = (text) => {
    if (!stompClientRef.current || !text.trim()) return;
    const payload = {
      nickname: myCharacter.nickname,
      message: text,
    };
    stompClientRef.current.send(`/app/game/${roomId}/chat`, {}, JSON.stringify(payload));
  };

  useEffect(() => {
    const scene = sceneRef.current;
    if (!sceneReady || !scene || !roomId || !userList.length || !myCharacter?.userId) return;
    scene.roomId = roomId;

    // 🔥 파티원 리스트 동기화 (추가/제거 통합 처리)
    if (scene.partyPlayersSystem) {
      scene.partyPlayersSystem.syncPartyPlayers(userList, myCharacter.userId, 1000, 1000);
    }
  }, [sceneReady, userList, myCharacter, roomId]);

  useEffect(() => {
    if (!gameReady) return;
    const config = {
      type: Phaser.AUTO,
      parent: "game-container",
      scale: {
        mode: Phaser.Scale.RESIZE,
        autoCenter: Phaser.Scale.CENTER_BOTH,
        width: window.innerWidth,
        height: window.innerHeight,
      },
      physics: {
        default: "arcade",
        arcade: { debug: false },
      },
      scene: [MainStageScene, UIScene], // 게임 메인에서는 바로 메인 스테이지 실행
    };
    const game = new Phaser.Game(config);
    return () => {
      if (sceneRef.current) sceneRef.current.sceneReady = false;
      game.destroy(true);
    };
  }, [gameReady, roomId]);

  return (
    <>
      <div
        id="game-container"
        style={{
          width: "100%",
          height: "100%",
          overflow: "hidden",
          margin: 0,
          padding: 0,
          position: "fixed",
          top: 0,
          left: 0
        }}
      ></div>
      {showStatSelection && (
        <StatSelection
          myCharacter={myCharacter}
          onClose={() => {
            setShowStatSelection(false);
            setToggle((prev) => !prev);
          }}
        />
      )}

      {/* 🔥 옵션 메뉴 (ESC) */}
      {showOption && (
        <>
          <div className="blur-bg" onClick={() => setShowOption(false)}></div>
          <div className="centered-popup">
            <Option onClose={() => setShowOption(false)} />
          </div>
        </>
      )}

      {/* 🔥 인게임 채팅 */}
      {/* 🔥 인게임 채팅 (항상 렌더링 + overlay 모드 지원) */}
      <GameChat
        myNickname={myCharacter.nickname}
        messages={messages}
        onSend={onSendChat}
        onClose={() => setIsChatOpen(false)}
        isOpen={isChatOpen}
      />
    </>
  );
}
