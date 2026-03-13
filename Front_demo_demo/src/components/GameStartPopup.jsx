import React from "react";
import "../css/GameStartPopup.css";

export default function GameStartPopup({ onAccept, onReject }) {
    return (
        <div className="gamestart-modal">
            <div className="gamestart-box">
                <h2 className="gamestart-title">게임 시작 요청</h2>
                <p className="gamestart-desc">
                    파티장이 게임 시작을 요청했습니다.<br />
                    준비가 되셨다면 수락을 눌러주세요.
                </p>
                <div className="gamestart-buttons">
                    <button className="confirm-btn" onClick={onAccept}>
                        수락
                    </button>
                    <button className="cancel-btn" onClick={onReject}>
                        거절
                    </button>
                </div>
            </div>
        </div>
    );
}
