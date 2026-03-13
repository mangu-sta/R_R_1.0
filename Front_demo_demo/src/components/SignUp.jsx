import "../css/SignUp.css"

import { Link, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { toast } from "react-toastify";
import { Eye, EyeOff, UserPlus } from "lucide-react";
import axios from "axios";

export default function SignUp() {
  const navigate = useNavigate();

  const [nickName, setNickName] = useState("");
  const [nickNameValid, setNickNameValid] = useState([]);
  const [showPassword, setShowPassword] = useState(false);
  const [password, setPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");

  useEffect(() => {
    if (password.length === 0) {
      setPasswordError("");
      return;
    }

    const errors = [];
    if (password.length < 8) {
      errors.push("8자 이상");
    }
    if (!/[A-Z]/.test(password)) {
      errors.push("대문자");
    }
    if (!/[a-z]/.test(password)) {
      errors.push("소문자");
    }
    if (!/[0-9]/.test(password)) {
      errors.push("숫자");
    }
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      errors.push("특수문자");
    }

    if (errors.length > 0) {
      setPasswordError(`${errors.join(", ")}가 포함되어야 합니다.`);
    } else {
      setPasswordError("사용 가능한 비밀번호입니다.");
    }
  }, [password]);

  const signUp = async () => {
    if (!nickName.trim()) {
      toast.error("닉네임을 입력해주세요.");
      return;
    }

    if (!password.trim()) {
      toast.error("비밀번호를 입력해주세요.");
      return;
    }

    if (passwordError && passwordError !== "사용 가능한 비밀번호입니다.") {
      toast.error(passwordError);
      return;
    }

    try {
      const response = await axios.post("/api/signup", {
        nickname: nickName,
        password: password,
      });

      const result = response.data ?? {};

      console.log(result);
      toast.success("회원가입이 완료되었습니다.");
      navigate("/signin");
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

  return (
    <fieldset className="SignUp">
      <div className="header_section">
        <UserPlus size={40} className="header_icon" />
        <div className="header_text">
          <h2>회원가입</h2>
          <p>Rogue Reload의 세계에 오신 것을 환영합니다!</p>
        </div>
      </div>
      <legend>회원가입</legend>
      <input
        type="text"
        id="nickname"
        value={nickName}
        onChange={(e) => {
          setNickName(e.target.value);
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter") signUp();
        }}
        placeholder="닉네임"
      />
      <div className="password_input_container">
        <input
          type={showPassword ? "text" : "password"}
          id="password"
          className="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") signUp();
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
      {passwordError && (
        <div
          className={`password_status ${passwordError === "사용 가능한 비밀번호입니다." ? "valid" : "invalid"
            }`}
        >
          {passwordError}
        </div>
      )}
      <div className="buttons">
        <Link to="/signin" id="login_link">
          로그인
        </Link>
        <button
          id="signup_btn"
          onClick={() => {
            signUp();
          }}
        >
          회원가입
        </button>
      </div>
    </fieldset>
  );
}

