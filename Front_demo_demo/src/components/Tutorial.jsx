import React, { useEffect, useRef, useState } from "react";
import Phaser from "phaser";
import { toast } from "react-toastify";
import { flushSync } from "react-dom";
// import { useParams } from "react-router-dom"; // Unused in original logic provided
import axios from "axios";

import MainScene from "../game/scenes/MainScene";
import MainStageScene from "../game/scenes/MainStageScene";
import UIScene from "../game/scenes/UIScene";
import { connectSocket } from "../game/network/socket";

import StatSelection from "./StatSelection";
import Option from "./Option";
import GameChat from "./GameChat";
import StageAdvancePopup from "./StageAdvancePopup"; // Import Popup
import "../css/App.css";

export default function Tutorial() {
  const [gameReady, setGameReady] = useState(false);
  const [sceneReady, setSceneReady] = useState(false);
  const [showStatSelection, setShowStatSelection] = useState(false); // 🔥 스탯 선택창 상태
  const [showOption, setShowOption] = useState(false); // 🔥 옵션 메뉴 상태
  const [stageAdvanceRequest, setStageAdvanceRequest] = useState(null); // 🔥 스테이지 이동 요청 상태
  const [centerMessage, setCenterMessage] = useState(""); // 🔥 중앙 알림 메시지 상태

  const [currentStage, setCurrentStage] = useState(0); // 🔥 현재 스테이지 상태

  const [myCharacter, setMyCharacter] = useState({});
  const [userList, setUserList] = useState([]);
  const [roomId, setRoomId] = useState("");
  const [toggle, setToggle] = useState(false);
  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const sceneRef = useRef(null);
  const [isChatOpen, setIsChatOpen] = useState(false);

  // 🔥 Restore missing state variables
  const [baseSpeed, setBaseSpeed] = useState(100);
  const [runSpeed, setRunSpeed] = useState(150);

  const [messages, setMessages] = useState([]);

  // ... (lines 33-112)

  // Load User Data
  useEffect(() => {
    const load = async () => {
      // getPartyUser에 setCurrentStage 전달
      const me = await getPartyUser(setMyCharacter, setRoomId, setCurrentStage);

      if (!me?.myCharacter?.userId || !me?.roomId) {
        console.error("❌ userId 또는 roomId 없음, 게임 시작 중단");
        return;
      }
      console.log(me.members);
      setUserList(me.members);
      setGameReady(true);
    };

    load();
  }, [toggle]);

  // 🔥 스테이지 변경 감지 및 씬 전환 (Server Sync)
  useEffect(() => {
    if (!sceneReady || !sceneRef.current) return;

    // 현재 실행 중인 씬 키 확인 (MainScene or MainStageScene)
    const currentSceneKey = sceneRef.current.scene.key;
    let targetSceneKey = "MainScene";

    if (currentStage === 0) targetSceneKey = "MainScene";
    else if (currentStage === 1) targetSceneKey = "MainStageScene";

    // 이미 해당 씬이면 무시
    if (currentSceneKey === targetSceneKey) return;

    console.log(`🔄 Stage Sync: Switching to ${targetSceneKey} (Stage ${currentStage})`);

    // 🔥 새로운 씬 시작 전 Ready 상태 리셋
    setSceneReady(false);

    // 씬 전환
    sceneRef.current.scene.start(targetSceneKey, {
      myUserId: myCharacter?.userId,
      myCharacterId: myCharacter?.id,
      myJob: myCharacter?.job,
      sendSender: safeSend,
      roomId: roomId,
      posX: myCharacter?.posX,
      posY: myCharacter?.posY
    });

  }, [currentStage, sceneReady]);

  // ... (Speed calc effect omitted)

  // ... (WebSocket Connection)


  // ... (lines 378 onwards)

  // getPartyUser 함수 수정
  //===================================
  // 게임외 함수
  //====================================
  const getPartyUser = async (setMyCharacter, setRoomId, setCurrentStage) => {
    try {
      const response = await axios.post(
        "/api/lobby/me",
        {},
        { withCredentials: true }
      );
      console.log(response.data);

      setMyCharacter(response.data.myCharacter);
      setRoomId(response.data.roomId);

      // 🔥 스테이지 정보 업데이트
      if (response.data.stage !== undefined && setCurrentStage) {
        console.log(`📊 Server Stage: ${response.data.stage}`);
        setCurrentStage(response.data.stage);
      }

      return response.data;
    } catch (error) {
      console.error(error);
      return null;
    }
  };

  // ... (Speed calc effect omitted)


  useEffect(() => {
    window.playerStats = {
      baseSpeed,
      runSpeed,
    };
  }, [baseSpeed, runSpeed]);

  // ... (WebSocket Helper omitted, no change) 
  const safeSend = (destination, body) => {
    const client = stompClientRef.current;
    if (!client || !client.connected) return;
    client.send(destination, {}, JSON.stringify(body));
  };

  // ===================================
  // Sync Data to Phaser
  // ===================================
  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene || !sceneReady || !myCharacter?.userId) return;

    scene.myUserId = myCharacter.userId;
    scene.myCharacterId = myCharacter.id; // 🔥 서버 식별용 ID 저장
    scene.myJob = myCharacter.job; // 🔥 직업 정보 저장
    scene.roomId = roomId; // 🔥 현재 룸 ID 저장 (이동/공격 통신용)
    scene.setNetworkSender(safeSend);

    if (scene.player) {
      // 🔥 서버에서 온 실시간 필드 우선 사용, 없으면 status JSON 파싱
      const status = myCharacter.status ? JSON.parse(myCharacter.status) : {};

      const strength = myCharacter.strength !== undefined ? myCharacter.strength : (status.STRENGTH || 0);
      const agility = myCharacter.agility !== undefined ? myCharacter.agility : (status.AGILITY || 0);
      const healthStat = myCharacter.health !== undefined ? myCharacter.health : (status.HEALTH || 0);
      const reloadStat = myCharacter.reload !== undefined ? myCharacter.reload : (status.RELOAD || 0);

      // 계산 로직
      const calculatedMaxHp = 100 + healthStat * 2;
      const calculatedBaseSpeed = 100 + agility * 2;
      const calculatedRunSpeed = calculatedBaseSpeed * 1.5;

      // Phaser 객체에 적용
      if (myCharacter.job) {
        scene.player.setTexture(myCharacter.job);
        scene.player.setDisplaySize(40, 40); // 🔥 텍스트 변경 후 크기 재설정
      }
      // 초기 maxExp 공식 동기화 (50 + level * 10)
      const initMaxExp = 50 + (scene.player.level * 10);
      scene.player.maxExp = initMaxExp;
      scene.player.nickname = myCharacter.nickname || "";
      scene.player.reloadStat = reloadStat;

      scene.player.maxHp = calculatedMaxHp;
      // HP 초기화: 서버 데이터(myCharacter.hp)가 있으면 사용, 없으면 최대 체력으로 설정
      if (scene.player.hp === undefined || scene.player.hp === 10 || scene.player.hp === 100) {
        scene.player.hp = myCharacter.hp !== undefined ? myCharacter.hp : calculatedMaxHp;
      } else if (scene.player.hp > calculatedMaxHp) {
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

      console.log("✅ Scene 데이터 주입 완료 (속도/체력 포함)", {
        speed: calculatedBaseSpeed,
        maxHp: calculatedMaxHp
      });
    }
  }, [sceneReady, myCharacter]);

  // ... (WebSocket Connection omitted)

  useEffect(() => {
    if (!roomId) {
      console.log("룸아이디가 없습니다.");
      return;
    }

    // 🔥 새로운 방(roomId)으로 연결하시 전, 이전 방의 몬스터 정리
    if (sceneRef.current && sceneRef.current.monstersSystem) {
      console.log("🧹 Room changed - Clearing previous monsters...");
      sceneRef.current.monstersSystem.clearMonsters();
    }

    connect(roomId);

    return () => disconnect();
  }, [roomId]);

  const connect = (targetRoomId = roomId) => {
    if (!targetRoomId) {
      console.log("룸아이디가 없습니다.");
      return;
    }

    if (stompClientRef.current?.connected) {
      console.log("⚠ 이미 WebSocket 연결 중 → connect() 생략");
      return;
    }

    console.log("🔌 WebSocket 연결 시도…");

    // Use modular socket helper
    stompClientRef.current = connectSocket({
      roomId: targetRoomId,
      onConnect: (client, subscription) => {
        // Unsubscribe old if exists
        if (subscriptionRef.current) {
          subscriptionRef.current.unsubscribe();
          subscriptionRef.current = null;
        }
        subscriptionRef.current = subscription;
        // 🔥 [CRITICAL] Send JOIN message to backend to mark character as connected in Redis
        client.send(`/app/game/${targetRoomId}/join`, {}, JSON.stringify({}));
      },
      onMessage: (chat) => {
        // 🔥 LEAVE/KICK 성격의 메시지가 오면 즉시 새로고침(Toggle) 트리거
        const isDeparture =
          chat.type === "LEAVE" ||
          chat.type === "KICK" ||
          chat.message === "LEAVE" ||
          chat.message === "KICK" ||
          (chat.value && (chat.value.message === "LEAVE" || chat.value.message === "KICK")) ||
          (chat.value === "LEAVE" || chat.value === "KICK");

        if (isDeparture) {
          console.log("🚪 Departure detected. Refreshing room state...");
          flushSync(() => {
            setToggle((prev) => !prev);
          });
        }

        switch (chat.type) {
          case "ACCEPT":
          case "LEAVE":
          case "KICK":
            if (sceneRef.current) sceneRef.current.handleGameMessage(chat);
            break;
          case "MESSAGE":
            setMessages((prev) => [...prev, chat.value]);
            if (sceneRef.current) sceneRef.current.handleGameMessage(chat);
            break;

          // 🔥 스테이지 이동 요청 수신
          case "STAGE_ADVANCE_REQUEST":
            console.log("🚩 스테이지 이동 요청 받음");
            setStageAdvanceRequest(chat);
            break;

          // 🔥 게임 오버 수신 (Fallback)
          case "GAME_OVER":
            console.log("💀 [Tutorial] GAME OVER Received! Triggering fallback sequence...");
            if (sceneRef.current) sceneRef.current.handleGameMessage(chat);

            // UI가 이벤트를 발생시키겠지만, 혹시 모를 상황 대비 안전장치 (4초 후 강제 복귀)
            setTimeout(() => {
              console.log("💀 [Tutorial] Force Return to Lobby (Fallback Timer)");
              window.dispatchEvent(new CustomEvent("return-to-lobby"));
            }, 4000);
            break;

          // 🔥 스테이지 이동 완료 (전원 수락)
          case "STAGE_ADVANCE_COMPLETED":
            console.log("🚀 스테이지 이동 승인됨! 1초 후 이동...");
            setStageAdvanceRequest(null); // 팝업 닫기
            setCenterMessage("모두 수락하였습니다! 곧 이동합니다...");

            // 1초 후 이동
            setTimeout(() => {
              setCenterMessage(""); // 메시지 지우기
              getPartyUser(setMyCharacter, setRoomId, setCurrentStage);
            }, 1000);
            break;

          // 🔥 스테이지 이동 거절됨
          case "STAGE_ADVANCE_REJECTED":
            toast.error("스테이지 이동이 거절되었습니다.");
            setStageAdvanceRequest(null);
            break;

          // 🔥 누군가 수락함 (진행상황)
          case "STAGE_ADVANCE_ACCEPTED":
            console.log("✅ 누군가 수락함:", chat);
            break;

          // 🔥 캐릭터 상태 업데이트 (레벨, 경험치, 스탯 등)
          case "PLAYER_STATE":
            if (chat.userId === myCharacter?.userId) {
              setMyCharacter(prev => ({
                ...prev,
                level: chat.level,
                exp: chat.exp,
                pendingStatPoints: chat.pendingStatPoints,
                strength: chat.strength,
                agility: chat.agility,
                health: chat.health,
                reload: chat.reload,
                hp: chat.hp,
                maxHp: chat.maxHp
              }));

              if (chat.pendingStatPoints > 0) {
                setShowStatSelection(true);
              } else {
                setShowStatSelection(false);
              }
            }
            // 🔥 [CRITICAL] Phaser 씬에도 상태 업데이트 전달
            if (sceneRef.current) sceneRef.current.handleGameMessage(chat);
            break;

          default:
            if (sceneRef.current) sceneRef.current.handleGameMessage(chat);
            break;
        }
        // 끄기: 너무 빈번한 메시지들 필터링
        const silentTypes = ["MOVE", "ROTATE", "PLAYER_HP_UPDATE", "BULLET", "FIRE", "HP"];
        if (!silentTypes.includes(chat.type)) {
          console.log("💬 새 메시지:", chat);
        }
      },
      onError: (error) => {
        // Toast or log
      }
    });
  };

  const disconnect = () => {
    const client = stompClientRef.current;

    if (!client || !client.connected) {
      return;
    }

    if (stompClientRef.current) {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }

      stompClientRef.current.disconnect(() => {
        console.log("🔌 WebSocket 연결 종료");
      });
    }
  };

  const sendMessage = (content) => {
    if (!stompClientRef.current || !content.trim()) return;

    const payload = {
      nickname: myCharacter.nickname,
      message: content,
    };

    safeSend(`/app/lobby/${roomId}/chat`, payload);
  };

  // ===================================
  // Phaser Event Listeners
  // ===================================
  useEffect(() => {
    const onReady = (e) => {
      sceneRef.current = e.detail;
      setSceneReady(true);
      console.log("🎮 Phaser Scene 준비 완료");

      // 💉 Inject Network Dependencies into Scene
      if (stompClientRef.current) {
        sceneRef.current.stompClient = stompClientRef.current;
      }
      if (roomId) {
        sceneRef.current.nanoId = roomId;
      }
    };

    // 🔥 레벨업 이벤트 리스너
    const onLevelUp = (e) => {
      console.log("🎉 [REACT] Level Up Event Received!", e.detail);
      setShowStatSelection(true);
    };

    // 🔥 스테이지 이동 요청 리스너 (MainScene에서 발생)
    const onRequestStageAdvance = async () => {
      if (!roomId) return;
      try {
        await axios.post(
          `/api/game/${roomId}/stage-advance/request`,
          {},
          { withCredentials: true }
        );
        toast.info("스테이지 이동을 요청했습니다.");
      } catch (err) {
        console.error(err);
        toast.error("요청 실패");
      }
    };

    // 🔥 서버 데이터 동기화 리스너
    const onDataSync = (e) => {
      setMyCharacter(prev => {
        const newData = { ...prev, ...e.detail };
        // ✅ 만약 포인트가 0이 되면 스탯 선택창 닫기
        if (newData.pendingStatPoints === 0) {
          setShowStatSelection(false);
        }
        return newData;
      });
    };

    window.addEventListener("phaser-ready", onReady);
    window.addEventListener("level-up", onLevelUp);
    window.addEventListener("player-data-sync", onDataSync);
    window.addEventListener("request-stage-advance", onRequestStageAdvance);

    // 🔥 로비 복귀 요청 리스너 (Game Over 또는 Boss Clear 후)
    const onReturnToLobby = () => {
      console.log("💀 [Event] 'return-to-lobby' caught in Tutorial.jsx");

      // 먼저 게임 정리
      if (sceneRef.current) {
        sceneRef.current.game.destroy(true);
      }

      // 로비로 페이지 이동
      navigate("/lobby");
    };
    window.addEventListener("return-to-lobby", onReturnToLobby);

    return () => {
      window.removeEventListener("phaser-ready", onReady);
      window.removeEventListener("level-up", onLevelUp);
      window.removeEventListener("player-data-sync", onDataSync);
      window.removeEventListener("request-stage-advance", onRequestStageAdvance);
      window.removeEventListener("return-to-lobby", onReturnToLobby);
    };
  }, [roomId]); // Added roomId dependency for the request

  // 🔥 ESC & Enter 키 리스너
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        // 이미 팝업이 떠있으면 닫고, 아니면 옵션 열기
        if (showStatSelection) {
          setShowStatSelection(false);
        } else {
          setShowOption(prev => !prev);
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
        // 이동 멈춤 처리
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
    stompClientRef.current.send(`/app/lobby/${roomId}/chat`, {}, JSON.stringify(payload));
  };

  // 🔥 스탯 선택 전송
  const handleStatSelect = (statIndex) => {
    if (!stompClientRef.current || !roomId) return;

    const payload = {
      userId: myCharacter.userId,
      statIndex: statIndex
    };

    safeSend(`/app/game/${roomId}/stat-select`, payload);
  };

  // ... (Party Players effect omitted)

  useEffect(() => {
    const scene = sceneRef.current;

    if (!sceneReady) return;
    if (!scene) return;
    if (!roomId) return;
    if (!userList.length) return;
    if (!myCharacter?.userId) return;

    scene.roomId = roomId;

    // 🔥 파티원 리스트 동기화 (추가/제거 통합 처리)
    if (scene.partyPlayersSystem) {
      scene.partyPlayersSystem.syncPartyPlayers(userList, myCharacter.userId);
    }
  }, [sceneReady, userList, myCharacter, roomId]);

  // ... (Init Game effect omitted)

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
      scene: [MainScene, MainStageScene, UIScene],
    };

    const game = new Phaser.Game(config);

    return () => {
      if (sceneRef.current) {
        sceneRef.current.sceneReady = false;
        sceneRef.current = null;
      }
      game.destroy(true);
    };
  }, [gameReady]);

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

      {/* 🔥 스탯 선택 팝업 */}
      {showStatSelection && (
        <StatSelection
          myCharacter={myCharacter}
          onSelect={handleStatSelect}
          onClose={() => setShowStatSelection(false)}
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


      {/* 🔥 스테이지 이동 요청 팝업 */}
      {stageAdvanceRequest && (
        <StageAdvancePopup
          onAccept={async () => {
            try {
              await axios.post(
                `/api/game/${roomId}/stage-advance/response`,
                { response: "ACCEPT" },
                { withCredentials: true }
              );
              setStageAdvanceRequest(null);
              toast.success("이동을 수락했습니다. 다른 플레이어를 기다립니다.");
            } catch (err) {
              console.error(err);
              toast.error("전송 실패");
            }
          }}
          onReject={async () => {
            try {
              await axios.post(
                `/api/game/${roomId}/stage-advance/response`,
                { response: "REJECT" },
                { withCredentials: true }
              );
              setStageAdvanceRequest(null);
              toast.info("이동을 거절했습니다.");
            } catch (err) {
              console.error(err);
              toast.error("전송 실패");
            }
          }}
        />
      )}

      {/* 🔥 중앙 알림 메시지 (Overlay) */}
      {centerMessage && (
        <div className="center-message-overlay">
          <div className="center-message-text">{centerMessage}</div>
        </div>
      )}
    </>
  );
}
