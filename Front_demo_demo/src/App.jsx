import "./css/App.css";

import SignIn from "./components/SignIn.jsx";
import SignUp from "./components/SignUp.jsx";
import CreateCharacter from "./components/CreateCharacter.jsx";
import Lobby from "./components/Lobby.jsx";
import Friend from "./components/Friend/Friend.jsx";
import Option from "./components/Option.jsx";
import Swal from "sweetalert2";
import withReactContent from "sweetalert2-react-content";

const MySwal = withReactContent(Swal);

import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { toast } from "react-toastify";

import axios from "axios";

export default function App({ step }) {
  const navigate = useNavigate();
  const [overlay, setOverlay] = useState(null);
  // overlay: null | "friend" | "option"

  // 🔥 ESC 이벤트 등록 (Lobby ESC와 충돌 방지)
  useEffect(() => {
    const handleEsc = (e) => {
      if (Swal.isVisible()) return;

      if (e.key === "Escape") {
        setOverlay((prev) => (prev === null ? "option" : null));
      }

      if (e.key === "u") {
        const active = document.activeElement;
        if (
          active.tagName === "INPUT" ||
          active.tagName === "TEXTAREA" ||
          active.tagName === "SELECT" ||
          active.isContentEditable
        ) {
          return;
        }

        setOverlay((prev) => (prev === null ? "friend" : null));
      }
    };

    window.addEventListener("keydown", handleEsc);

    return () => {
      window.removeEventListener("keydown", handleEsc);
    };
  }, [overlay]);

  const [gameStartHandler, setGameStartHandler] = useState(null);

  let page = null;
  switch (step) {
    case "SignIn":
      page = <SignIn />;
      break;
    case "SignUp":
      page = <SignUp />;
      break;
    case "CreateCharacter":
      page = <CreateCharacter />;
      break;
    case "lobby":
      page = <Lobby setGameStartHandler={setGameStartHandler} />;
      break;
    default:
      page = null;
  }

  // SSE 연결 및 알림
  useEffect(() => {
    let eventSource = null;
    let isCancelled = false;

    const connectSSE = async () => {
      // lobby 단계이거나 로그인이 필요한 단계에서만 SSE 연결 시도
      if (step !== "lobby") return;

      const isSigned = await signCheck();
      if (!isSigned || isCancelled) return;

      console.log("📡 SSE 연결 시도 중...");
      eventSource = new EventSource("/api/notifications/subscribe", {
        withCredentials: true,
      });

      eventSource.onmessage = (event) => {
        console.log("SSE 기본 메시지:", event.data);
      };

      eventSource.addEventListener("notification", (event) => {
        const data = JSON.parse(event.data);
        console.log("📩 알림 도착:", data);

        switch (data.type) {
          case "FRIEND_REQUEST":
            toast.info(data.message);
            break;
          case "FRIEND_ACCEPT":
            toast.success(data.message);
            break;
          case "FRIEND_REJECT":
            toast.warn(data.message);
            break;
          case "FRIEND_CANCEL":
            toast.warn(data.message);
            break;
          case "LOBBY_INVITE":
            MySwal.fire({
              title: "대기실 초대가 도착했습니다",
              html: `<div style="font-size:16px; margin-top:5px;">${data.message}</div>`,
              icon: "info",
              showCancelButton: true,
              confirmButtonText: "수락",
              cancelButtonText: "거절",
              reverseButtons: false,
              backdrop: `rgba(0,0,0,0.4)`,
              allowEnterKey: true,
              allowEscapeKey: true,
              allowOutsideClick: false,
              didOpen: () => {
                const confirmBtn = MySwal.getConfirmButton();
                const cancelBtn = MySwal.getCancelButton();
                const handleKey = (e) => {
                  if (e.key === "Enter") confirmBtn.click();
                  if (e.key === "Escape") cancelBtn.click();
                };
                window.addEventListener("keydown", handleKey);
                MySwal._keydownHandler = handleKey;
              },
              didClose: () => {
                document.body.classList.remove("swal2-height-auto");
                document.body.style.overflow = "";
                if (MySwal._keydownHandler) {
                  window.removeEventListener("keydown", MySwal._keydownHandler);
                  MySwal._keydownHandler = null;
                }
              },
            }).then((result) => {
              if (result.isConfirmed) {
                axios
                  .post(
                    "/api/lobby/invite/accept",
                    { inviteId: data.inviteId, partyId: data.data.partyId },
                    { withCredentials: true }
                  )
                  .then(() => toast.success("초대를 수락했습니다!"))
                  .catch(() => toast.error("수락 처리 실패"));
              }
            });
            break;
          case "CONSOLE":
            console.log(data.message);
            break;
          default:
            console.warn("Unknown message type:", data);
        }
      });

      eventSource.onerror = (error) => {
        // readyState가 CLOSED일 때만 에러로 출력 (단순 재연결 시도는 무시하거나 로그로만)
        if (eventSource.readyState === EventSource.CLOSED) {
          console.error("❌ SSE 연결이 종료되었습니다. (재연결 중지)");
        } else {
          console.warn("📡 SSE 연결 일시 중단: 재연결 시도 중...", error);
        }
      };
    };

    connectSSE();

    return () => {
      isCancelled = true;
      if (eventSource) {
        console.log("🔌 SSE 연결 종료");
        eventSource.close();
      }
    };
  }, [step]); // step이 변경될 때마다 (로그인 후 lobby 진입 등) 재실행

  return (
    <div className="app">
      <div className="header">
        <button
          id="game_start"
          onClick={() => {
            if (gameStartHandler) {
              gameStartHandler();
            } else {
              navigate("/tutorial");
            }
          }}
        >
          게임 시작
        </button>
        <div className="logo">
          <img src="./assets/img/logo/Logo.png" alt="#" />
        </div>
        <div className="right_buttons">
          <a
            href="https://www.notion.so/2b99f33b0f6a80bbb133eade1a8816e8?v=2b99f33b0f6a8115ad16000cba27964e&source=copy_link"
            target="blink"
          >
            <button>노션</button>
          </a>

          <button onClick={() => setOverlay("friend")}>친구</button>
          <button onClick={() => setOverlay("option")}>설정</button>
        </div>
      </div>

      <div className="main">
        <div className="form">{page}</div>

        {/* 블러 오버레이 */}
        {overlay && (
          <div className="blur-bg" onClick={() => setOverlay(null)}></div>
        )}

        {/* 우측 상단 팝업 */}
        {overlay === "friend" && (
          <div className="popup-panel">
            <Friend onClose={() => setOverlay(null)} />
          </div>
        )}

        {overlay === "option" && (
          <div className="popup-panel">
            <Option onClose={() => setOverlay(null)} />
          </div>
        )}
      </div>
    </div>
  );
}

const signCheck = async () => {
  try {
    const response = await axios.post(
      "/api/auth/verify",
      {},
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);
    return true;
  } catch (error) {
    // if (error.response) {
    //   console.error(
    //     "서버 오류:",
    //     error.response.status,
    //     error.response.statusText
    //   );
    // } else {
    //   console.error("등록 오류:", error);
    // }
    return false;
  }
};
