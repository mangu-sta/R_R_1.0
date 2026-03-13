import "../css/Lobby.css";

import { Link, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { toast } from "react-toastify";
import { flushSync } from "react-dom";
import SockJS from "sockjs-client";
import Stomp from "stompjs";
import InviteSlot from "./InviteSlot";
import ChatPopup from "./ChatPopup";
import GameStartPopup from "./GameStartPopup";

import axios from "axios";

export default function Loby({ setGameStartHandler }) {
  const navigate = useNavigate();
  const [myCharacter, setMyCharacter] = useState({});
  const [userList, setUserList] = useState([]);
  const [openSlotIndex, setOpenSlotIndex] = useState(null);
  const [roomId, setRoomId] = useState("");
  const [toggle, setToggle] = useState(false);
  const [openChat, setOpenChat] = useState(false);
  const [gameStartRequest, setGameStartRequest] = useState(null); // 🔥 게임 시작 요청 팝업 상태

  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);

  const [messages, setMessages] = useState([]);

  const [jobImg] = useState({
    soldier: "./assets/img/character/soldier_profile.png",
    firefighter: "./assets/img/character/firefighter_profile.png",
    reporter: "./assets/img/character/reporter_profile.png",
    doctor: "./assets/img/character/doctor_profile.png",
  });

  const pressedKeys = useRef(new Set()); // 🔥 조합키 감지용

  // ESC 누르면 채팅창 닫기 + 초대창 닫기 + 조합키 감지
  useEffect(() => {
    const handleKeyDown = (e) => {
      // 1️⃣ 조합키 감지 (Shift + I + H)
      pressedKeys.current.add(e.key.toLowerCase());
      if (
        pressedKeys.current.has("shift") &&
        pressedKeys.current.has("i") &&
        pressedKeys.current.has("h")
      ) {
        const nicknames = userList.map((u) => u.nickname).join(", ");
        toast.success(`🏆 승리! 파티원: ${nicknames}`, {
          position: "top-center",
          autoClose: 5000,
        });
        // 연속 발동 방지
        pressedKeys.current.delete("i");
        pressedKeys.current.delete("h");
      }

      if (e.key !== "Escape") return;

      // 2️⃣ ChatPopup이 열렸으면 → Chat 먼저 닫고 ESC 전파 차단
      if (openChat) {
        e.stopPropagation();
        setOpenChat(false);
        return;
      }

      // 3️⃣ InviteSlot(openSlotIndex)이 열렸으면 닫기
      if (openSlotIndex !== null) {
        e.stopPropagation();
        setOpenSlotIndex(null);
        return;
      }

      // 4️⃣ GameStartPopup이 열렸으면 닫기 (거절 처리와 통일)
      if (gameStartRequest) {
        e.stopPropagation();
        setGameStartRequest(null);
        return;
      }
    };

    const handleKeyUp = (e) => {
      pressedKeys.current.delete(e.key.toLowerCase());
    };

    window.addEventListener("keydown", handleKeyDown, true);
    window.addEventListener("keyup", handleKeyUp, true);
    return () => {
      window.removeEventListener("keydown", handleKeyDown, true);
      window.removeEventListener("keyup", handleKeyUp, true);
    };
  }, [openChat, openSlotIndex, gameStartRequest, userList]);

  useEffect(() => {
    const load = async () => {
      const me = await getPartyUser(setMyCharacter, setRoomId);
      if (!me) return;
      console.log(me.members);
      setUserList(me.members);
    };

    load();
  }, [toggle]);

  // 🔥 상위 컴포넌트(App.jsx)의 헤더 버튼과 게임 시작 로직 연결
  useEffect(() => {
    if (!setGameStartHandler || !roomId) return;

    // 게임 시작 요청 함수
    const requestStart = async () => {
      // 내가 방장인지 체크 (여기선 userList[0]이 방장이라 가정)
      // userList 업데이트 시점에 의존하므로 myCharacter 정보와 비교 필요
      // 다만 버튼 클릭 시점에 userList가 최신 상태인지가 중요

      try {
        await axios.post(
          `/api/lobby/${roomId}/start-request`,
          {},
          { withCredentials: true }
        );
        toast.info("게임 시작을 요청했습니다.");
      } catch (err) {
        console.error(err);
        toast.error("게임 시작 요청 실패");
      }
    };

    // 핸들러 등록
    setGameStartHandler(() => requestStart);

    return () => {
      // 언마운트 시 해제
      setGameStartHandler(null);
    };
  }, [setGameStartHandler, roomId]); // roomId나 userList 바뀔 때마다 갱신 필요할 수 있음

  //=============================

  // 웹소켓 연결 + 초기 로딩
  useEffect(() => {
    if (!roomId) {
      console.log("룸아이디가 없습니다.");
      return;
    } // mapId가 로딩되기 전엔 연결 X

    connect(roomId);

    return () => disconnect();
  }, [roomId]);

  // SockJs + STOMP 연결
  const connect = (targetRoomId = roomId) => {
    if (!targetRoomId) {
      console.log("룸아이디가 없습니다.");
      return;
    }

    // ⛔ 중복 연결 방지
    if (stompClientRef.current?.connected) {
      console.log("⚠ 이미 WebSocket 연결 중 → connect() 생략");
      return;
    }

    console.log("🔌 WebSocket 연결 시도…");
    const socket = new SockJS(`http://${window.location.hostname}:8080/ws/lobby`);
    stompClientRef.current = Stomp.over(socket);
    stompClientRef.current.debug = null; // 🔥 로우(Raw) 메시지 디버그 로그 끄기

    stompClientRef.current.connect(
      {},
      () => {
        console.log("📡 Loby WebSocket 연결 성공");

        // 기존 room 구독 해제하고 새 room 구독
        if (subscriptionRef.current) {
          subscriptionRef.current.unsubscribe();
          subscriptionRef.current = null;
          console.log("🔄 이전 구독 해제 완료");
        }
        subscriptionRef.current = stompClientRef.current.subscribe(
          `/topic/lobby/${targetRoomId}`,
          (msg) => {
            const chat = JSON.parse(msg.body);
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
              case "GAME_START_REQUEST":
                setGameStartRequest(chat);
                break;
              case "GAME_START":
                // 게임 시작! -> /game으로 이동
                console.log("🚀 GAME_START 메시지 수신:", chat);
                navigate("/tutorial");
                break;
              case "GAME_START_REJECTED":
                toast.error("게임 시작이 거절되었습니다.");
                setGameStartRequest(null);
                break;
              default:
                break;
            }
            console.log("💬 새 메시지:", chat.value);
          }
        );
        console.log(`🏠 방(${targetRoomId}) 구독 완료`);
        // // pending 처리
        // if (pendingRoomId.current && pendingRoomId.current !== targetRoomId) {
        //   const nextRoom = pendingRoomId.current;
        //   pendingRoomId.current = null;
        //   disconnect(() => connect(nextRoom));
        // }
      },
      (error) => {
        console.error("❌ WebSocket 연결 실패:", error);
      }
    );
  };

  // WebSocket 해제
  const disconnect = () => {
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

  // 메시지 전송 (필요 없으면 삭제 가능)
  const sendMessage = (content) => {
    if (!stompClientRef.current || !content.trim()) return;

    const payload = {
      nickname: myCharacter.nickname,
      message: content,
    };

    stompClientRef.current.send(
      `/app/lobby/${roomId}/chat`,
      {},
      JSON.stringify(payload)
    );
  };
  return (
    <fieldset className="Loby">
      {/* 🔹 현재 접속 중인 유저들 */}
      {userList.map((item, index) => (
        <div className="card" key={index}>
          <div className="user_nickname">{item.nickname}</div>

          <div className="character_info">
            <div className="chrarcter_img">
              <img
                src={
                  item.job === "SOLDIER"
                    ? jobImg.soldier
                    : item.job === "FIREFIGHTER"
                      ? jobImg.firefighter
                      : item.job === "REPORTER"
                        ? jobImg.reporter
                        : item.job === "DOCTOR"
                          ? jobImg.doctor
                          : ""
                }
                alt="캐릭터 이미지를 불러올수 없습니다."
              />
            </div>

            <div className="chararter_status">
              <div className="strength">
                <span className="state_name">STRENGTH</span>
                <span className="dash"></span>
                <span className="state">
                  {JSON.parse(item.status).STRENGTH}
                </span>
              </div>

              <div className="agility">
                <span className="state_name">AGILITY</span>
                <span className="dash"></span>
                <span className="state">{JSON.parse(item.status).AGILITY}</span>
              </div>

              <div className="health">
                <span className="state_name">HEALTH</span>
                <span className="dash"></span>
                <span className="state">{JSON.parse(item.status).HEALTH}</span>
              </div>

              <div className="reload">
                <span className="state_name">RELOAD</span>
                <span className="dash"></span>
                <span className="state">{JSON.parse(item.status).RELOAD}</span>
              </div>
            </div>
          </div>

          <div className="buttons">
            {item.nickname === myCharacter.nickname && (
              <button
                id="out"
                onClick={() => {
                  klck("", toggle, setToggle);
                }}
              >
                나가기
              </button>
            )}
            {userList[0].nickname === myCharacter.nickname && index !== 0 && (
              <button
                id="kick"
                onClick={() => {
                  klck(item.nickname, toggle, setToggle);
                }}
              >
                추방
              </button>
            )}
          </div>
        </div>
      ))}

      {/* 🔹 방장일 경우: 빈 슬롯 말고 "게임 시작" 버튼도 따로 혹은 빈 슬롯 외부에 배치 */}
      {/* 여기서는 유저 리스트 외부에 배치하거나, userList.length가 꽉 찼을 때 어떻게 할지 등 UI 기획에 따라 다름 */}
      {/* 일단 우측 하단이나 적절한 곳에 배치한다고 가정 */}

      {/* 🔹 남은 칸에 + 추가 (최대 4명) */}
      {Array.from({ length: Math.max(0, 4 - userList.length) }).map((_, i) =>
        openSlotIndex === i ? (
          // 🔹 친구초대 UI로 변경
          <InviteSlot
            key={`invite-${i}`}
            onCancel={() => setOpenSlotIndex(null)}
            onInvite={(target) => {
              invite(target);
              setOpenSlotIndex(null);
            }}
            userList={userList}
          />
        ) : (
          // 🔹 기본 빈 슬롯 상태
          <div
            className="card empty"
            key={`empty-${i}`}
            onClick={() => setOpenSlotIndex(i)}
          >
            <span className="plus">+</span>
          </div>
        )
      )}
      {/* 채팅창 열기 버튼 */}
      <button className="open-chat-btn" onClick={() => setOpenChat(true)}>
        채팅
      </button>

      {/* 팝업 컴포넌트 */}
      {openChat && (
        <ChatPopup
          myNickname={myCharacter.nickname}
          onClose={() => setOpenChat(false)}
          messages={messages}
          onSend={sendMessage}
        />
      )}

      {/* 🔥 게임 시작 요청 팝업 */}
      {gameStartRequest && (
        <GameStartPopup
          onAccept={async () => {
            try {
              await axios.post(
                `/api/lobby/${roomId}/start-response`,
                { response: "ACCEPT" },
                { withCredentials: true }
              );
              setGameStartRequest(null);
              toast.success("수락했습니다. 다른 플레이어를 기다립니다.");
            } catch (err) {
              console.error(err);
              toast.error("전송 실패");
            }
          }}
          onReject={async () => {
            try {
              await axios.post(
                `/api/lobby/${roomId}/start-response`,
                { response: "REJECT" },
                { withCredentials: true }
              );
              setGameStartRequest(null);
              toast.info("게임 시작을 거절했습니다.");
            } catch (err) {
              console.error(err);
              toast.error("전송 실패");
            }
          }}
        />
      )}
    </fieldset>
  );
}

import keyMapping from "../game/utils/keyMapping";

const getPartyUser = async (setMyCharacter, setRoomId) => {
  try {
    const response = await axios.post(
      "/api/lobby/me",
      {},
      { withCredentials: true }
    );
    console.log(response.data);

    // 키 세팅 서버 동기화
    if (response.data.keySetting) {
      keyMapping.syncWithServer(response.data.keySetting);
    }

    setMyCharacter(response.data.myCharacter);
    setRoomId(response.data.roomId);
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
};

const invite = async (target) => {
  console.log("초대한 닉네임: ", target);
  try {
    const response = await axios.post(
      "/api/lobby/invite",
      { nickname: target },
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);
    toast.success(`${target}님을 초대했습니다.`);
  } catch (error) {
    if (error.response) {
      console.error(
        "서버 오류:",
        error.response.status,
        error.response.statusText
      );
    } else {
      console.error("등록 오류:", error);
    }
  }
};

const klck = async (target, toggle, setToggle) => {
  console.log("추방 닉네임: ", target);
  try {
    const response = await axios.post(
      "/api/lobby/leave",
      { targetNickname: target },
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);
    setToggle(!toggle);
    if (target) {
      toast.error(`${target}님을 퇴장 시켰습니다.`);
    } else {
      toast.success(`퇴장 하였습니다.`);
    }
  } catch (error) {
    if (error.response) {
      console.error(
        "서버 오류:",
        error.response.status,
        error.response.statusText
      );
    } else {
      console.error("등록 오류:", error);
    }
  }
};
