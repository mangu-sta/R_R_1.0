import "../css/CreateCharacter.css";

import { Link, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { toast } from "react-toastify";
import { ChevronLeft, ChevronRight } from "lucide-react";
import axios from "axios";

export default function CreateCharacter() {
  const navigate = useNavigate();

  const [jobImg] = useState({
    soldier: "./assets/img/character/soldier_profile.png",
    firefighter: "./assets/img/character/firefighter_profile.png",
    reporter: "./assets/img/character/reporter_profile.png",
    doctor: "./assets/img/character/doctor_profile.png",
  });

  const jobList = ["SOLDIER", "FIREFIGHTER", "REPORTER", "DOCTOR"];
  const [job, setJob] = useState(jobList[0]);
  const statusList = [
    { strength: 4, agility: 4, health: 4, reload: 8 },
    { strength: 8, agility: 4, health: 4, reload: 4 },
    { strength: 4, agility: 8, health: 4, reload: 4 },
    { strength: 4, agility: 4, health: 8, reload: 4 },
  ];
  const [status, setStatus] = useState(statusList[0]);
  const [centerIndex, setCenterIndex] = useState(0);

  // ----------------------------
  // 🔥 변화량 계산용 훅
  // ----------------------------
  const prevStatus = useRef(status);
  const [diff, setDiff] = useState({});

  useEffect(() => {
    const newDiff = {};
    const before = prevStatus.current;
    const after = status;

    Object.keys(after).forEach((key) => {
      if (before[key] !== after[key]) {
        newDiff[key] = after[key] - before[key];
      }
    });

    setDiff(newDiff);
    prevStatus.current = after;
  }, [status]);

  // centerIndex 변경 → status & job 변경
  useEffect(() => {
    setJob(jobList[centerIndex]);
    setStatus(statusList[centerIndex]);
  }, [centerIndex]);

  useEffect(() => {
    console.log("now Job: " + job);
  }, [job]);

  // -----------------------------
  // 캐러셀 useEffect
  // -----------------------------
  useEffect(() => {
    const items = Array.from(
      document.querySelectorAll(".createCharacter .select .character")
    );
    if (items.length === 0) return;

    function applyClasses() {
      const len = items.length;

      items.forEach((el, i) => {
        el.className = "character";

        const d = (i - centerIndex + len) % len;

        if (d === 0) el.classList.add("is-center");
        else if (d === 1) el.classList.add("is-right");
        else if (d === 2) el.classList.add("is-far-right");
        else if (d === len - 1) el.classList.add("is-left");
        else if (d === len - 2) el.classList.add("is-far-left");
        else el.style.opacity = "0";

        el.style.left = "50%";
        el.style.top = "50%";
      });
    }

    applyClasses();
  }, [centerIndex]);

  return (
    <fieldset className="createCharacter">
      <div className="characterContainer">
        <div className="select">
          <div className="character" id="soldier">
            <img src={jobImg.soldier} alt="Soldier" />
          </div>
          <div className="character" id="firefighter">
            <img src={jobImg.firefighter} alt="firefighter" />
          </div>
          <div className="character" id="reporter">
            <img src={jobImg.reporter} alt="reporter" />
          </div>
          <div className="character" id="doctor">
            <img src={jobImg.doctor} alt="doctor" />
          </div>
        </div>

        {/* ----------------------------------- */}
        {/* 🔥 변경된 스탯 표시 영역 */}
        {/* ----------------------------------- */}
        <div className="status">
          <span className={"strength"+(job==="FIREFIGHTER"?" up":"")}>
            힘: {status.strength}
          </span>

          <span className={"agility"+(job==="REPORTER"?" up":"")}>
            속도: {status.agility}
          </span>

          <span className={"health"+(job==="DOCTOR"?" up":"")}>
            체력: {status.health}
          </span>

          <span className={"reload"+(job==="SOLDIER"?" up":"")}>
            재장전: {status.reload}
          </span>
        </div>
      </div>

      <div
        className="carousel-controls"
        style={{ textAlign: "center", marginTop: "12px" }}
      >
        <button onClick={() => setCenterIndex((prev) => (prev - 1 + 4) % 4)}>
          <ChevronLeft />
        </button>
        <button onClick={() => setCenterIndex((prev) => (prev + 1) % 4)}>
          <ChevronRight />
        </button>
      </div>

      <button
        id="select_btn"
        onClick={() => {
          selectJob(job, navigate);
        }}
      >
        직업 선택
      </button>
    </fieldset>
  );
}

/* 서버 업로드 로직 */
const selectJob = async (job, navigate) => {
  try {
    const response = await axios.post(
      "/api/character/create",
      { "job":job },
      { withCredentials: true }
    );

    toast.success("직업이 선택되었습니다.");
    navigate("/Lobby");
  } catch (error) {
    console.error("등록 오류:", error);
  }
};
