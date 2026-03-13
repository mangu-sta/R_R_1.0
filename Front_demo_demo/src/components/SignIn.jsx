import "../css/SignIn.css"

import { Link, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { toast } from "react-toastify";
import { Eye, EyeOff, LogIn } from "lucide-react";
import axios from "axios";

// var date = new Date();

export default function Login() {
  const navigate = useNavigate();

  const [nickName, setNickName] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [password, setPassword] = useState("");
  return (
    <fieldset className="SignIn">
      <div className="header_section">
        <LogIn size={40} className="header_icon" />
        <div className="header_text">
          <h2>로그인</h2>
          <p>전장으로 복귀할 준비가 되셨나요?</p>
        </div>
      </div>
      <legend>로그인</legend>
      <input
        type="text"
        id="nickname"
        value={nickName}
        onChange={(e) => setNickName(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") signIn(nickName, password, navigate);
        }}
        placeholder="닉네임"
      />
      <div className="password_input_container">
        <input
          type={showPassword ? "text" : "password"}
          id="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") signIn(nickName, password, navigate);
          }}
        />
        <button
          type="button"
          className="toggle_btn"
          onClick={() => setShowPassword(!showPassword)}
          aria-label="비밀번호 보기"
        >
          {showPassword ? <Eye size={28} /> : <EyeOff size={28} />}
        </button>
      </div>
      <div className="buttons">
        <Link to="/signUp" id="regist_link">
          회원가입
        </Link>
        <button
          id="signin_btn"
          onClick={() => {
            signIn(nickName, password, navigate);
          }}
        >
          로그인
        </button>
      </div>
    </fieldset>
  );
}

const signIn = async (nickname, password, navigate) => {
  console.log("nickname: [" + nickname + "] password: [" + password + "]")
  try {
    const response = await axios.post("/api/signin", {
      "nickname": nickname,
      "password": password,
    });

    const result = response.data ?? {};

    console.log(result);
    if (result.success) {
      // date.setTime(date.getTime() + 7 * 24 * 60 * 60 * 1000);
      // document.cookie = `nickname=${nickname}; expires=${date.toUTCString()}; path=/`;
      toast.success("로그인이 되었습니다.");
      if (result.hasCharacter) {
        navigate("/Lobby");
      } else {
        navigate("/createcharacter");
      }
    }
  } catch (error) {
    if (error.response) {
      if (error.response.status === 401) {
        toast.error("닉네임 혹은 비밀번호가 틀립니다.")
      } else {
        console.error(
          "서버 오류:",
          error.response.status,
          error.response.statusText
        );
      }
    } else {
      console.error("등록 오류:", error);
    }
  }
};
