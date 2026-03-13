import "../css/Option.css";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useState, useEffect } from "react";

import KeySetting from "./KeySetting";
import SoundSetting from "./SoundSetting";
import keyMapping from "../game/utils/keyMapping";

export default function Option({ onClose }) {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showKeySetting, setShowKeySetting] = useState(false);
  const [showSoundSetting, setShowSoundSetting] = useState(false);

  useEffect(() => {
    const checkLoginStatus = async () => {
      try {
        const response = await axios.post(
          "/api/auth/verify",
          {},
          { withCredentials: true }
        );
        console.log("로그인 상태 확인:", response.data);
        if (response.data && response.data.valid === true) {
          setIsLoggedIn(true);
        } else {
          setIsLoggedIn(false);
        }
      } catch (error) {
        setIsLoggedIn(false);
      }
    };

    checkLoginStatus();
  }, []);

  const handleLogout = async () => {
    try {
      // 🔥 로그아웃 시 로비 퇴장(게임 나가기)도 같이 처리
      try {
        await axios.post("/api/lobby/leave", { targetNickname: "" }, { withCredentials: true });
      } catch (err) {
        // 로비에 없거나 에러가 나도 로그아웃 진행
        console.warn("Leave lobby failed (ignoring):", err);
      }

      await axios.post("/api/signout", {}, { withCredentials: true });
      keyMapping.resetToDefault();
      toast.success("로그아웃되었습니다.");
      onClose();
      navigate("/signin");
    } catch (error) {
      console.error("로그아웃 오류:", error);
      toast.error("로그아웃 중 오류가 발생했습니다.");
    }
  };

  const handleSaveGame = async () => {
    if (!isLoggedIn) {
      toast.error("게임을 저장하려면 로그인이 필요합니다.");
      return;
    }

    try {
      await axios.post("/api/game/save", {}, { withCredentials: true });
      toast.success("게임 상태가 성공적으로 저장되었습니다.");
    } catch (error) {
      console.error("게임 저장 오류:", error);
      toast.error("게임 저장 중 오류가 발생했습니다.");
    }
  };

  const handleExitGame = async () => {
    try {
      await axios.post("/api/lobby/leave", { targetNickname: "" }, { withCredentials: true });
      toast.success("방에서 퇴장하였습니다.");
      onClose();
      navigate("/lobby");
    } catch (error) {
      console.error("퇴장 오류:", error);
      // 에러가 나더라도 일단 로비로 이동 시도
      navigate("/lobby");
    }
  };

  if (showKeySetting) {
    return <KeySetting onBack={() => setShowKeySetting(false)} isLoggedIn={isLoggedIn} />;
  }

  if (showSoundSetting) {
    return <SoundSetting onBack={() => setShowSoundSetting(false)} />;
  }

  return (
    <div className="Option-content">
      <div className="title">
        옵션 목록
        <button onClick={onClose} className="close_btn">
          ✕
        </button>
      </div>
      <div className="option_list">
        <div className="option_item" onClick={() => setShowKeySetting(true)} style={{ cursor: 'pointer' }}>
          <p>키 설정</p>
        </div>
        <div className="option_item" onClick={() => setShowSoundSetting(true)} style={{ cursor: 'pointer' }}>
          <p>사운드 설정</p>
        </div>

        {isLoggedIn && (
          <div className="option_item save_game_item" onClick={handleSaveGame} style={{ cursor: 'pointer' }}>
            <p>게임 저장</p>
          </div>
        )}

        <div className="option_item exit_item" onClick={handleExitGame} style={{ cursor: 'pointer' }}>
          <p>나가기</p>
        </div>
      </div>
      {isLoggedIn && (
        <div className="footer">
          <button className="logout_btn" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      )}
    </div>
  );
}
