import React, { useState, useEffect } from "react";
import keyMapping from "../game/utils/keyMapping";
import "../css/KeySetting.css";
import { toast } from "react-toastify";
import axios from "axios";

const KEY_LABELS = {
    moveUp: "위로 이동",
    moveDown: "아래로 이동",
    moveLeft: "왼쪽으로 이동",
    moveRight: "오른쪽으로 이동",
    fire: "발사",
    aim: "조준",
    run: "달리기",
    reload: "재장전",
    chat: "채팅",
    skill: "스킬",
    melee: "근접 공격",
    fireMode: "발사 모드 (단발/연사)"
};

export default function KeySetting({ onBack, isLoggedIn }) {
    const [keys, setKeys] = useState(keyMapping.getKeys());
    const [listeningAction, setListeningAction] = useState(null);

    // 내부 값 -> 표시용 텍스트 변환
    const formatDisplayKey = (key) => {
        if (!key) return "";

        const displayMap = {
            "ZERO": "0", "ONE": "1", "TWO": "2", "THREE": "3", "FOUR": "4",
            "FIVE": "5", "SIX": "6", "SEVEN": "7", "EIGHT": "8", "NINE": "9",
            "NUMPAD_ZERO": "N0", "NUMPAD_ONE": "N1", "NUMPAD_TWO": "N2", "NUMPAD_THREE": "N3", "NUMPAD_FOUR": "N4",
            "NUMPAD_FIVE": "N5", "NUMPAD_SIX": "N6", "NUMPAD_SEVEN": "N7", "NUMPAD_EIGHT": "N8", "NUMPAD_NINE": "N9",
            "LeftClick": "L-Click",
            "RightClick": "R-Click",
            "SPACE": "Space",
            "SHIFT": "Shift",
            "CTRL": "Ctrl",
            "ENTER": "Enter",
            "ESC": "Esc",
            "TAB": "Tab",
            "BACKTICK": "`",
            "MINUS": "-",
            "EQUALS": "=",
            "OPEN_BRACKET": "[",
            "CLOSED_BRACKET": "]",
            "BACK_SLASH": "\\",
            "SEMICOLON": ";",
            "QUOTE": "'",
            "COMMA": ",",
            "PERIOD": ".",
            "SLASH": "/"
        };

        return displayMap[key] || key;
    };

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (!listeningAction) return;

            e.preventDefault();
            const rawKey = e.key;
            const code = e.code;

            // 숫자 키 맵핑 (0-9 -> ZERO-NINE)
            const numberMap = {
                "0": "ZERO", "1": "ONE", "2": "TWO", "3": "THREE", "4": "FOUR",
                "5": "FIVE", "6": "SIX", "7": "SEVEN", "8": "EIGHT", "9": "NINE"
            };

            const numpadMap = {
                "Numpad0": "NUMPAD_ZERO", "Numpad1": "NUMPAD_ONE", "Numpad2": "NUMPAD_TWO",
                "Numpad3": "NUMPAD_THREE", "Numpad4": "NUMPAD_FOUR", "Numpad5": "NUMPAD_FIVE",
                "Numpad6": "NUMPAD_SIX", "Numpad7": "NUMPAD_SEVEN", "Numpad8": "NUMPAD_EIGHT",
                "Numpad9": "NUMPAD_NINE"
            };

            const specialKeyMap = {
                "`": "BACKTICK", "~": "BACKTICK",
                "-": "MINUS", "_": "MINUS",
                "=": "EQUALS", "+": "EQUALS",
                "[": "OPEN_BRACKET", "{": "OPEN_BRACKET",
                "]": "CLOSED_BRACKET", "}": "CLOSED_BRACKET",
                "\\": "BACK_SLASH", "|": "BACK_SLASH",
                ";": "SEMICOLON", ":": "SEMICOLON",
                "'": "QUOTE", "\"": "QUOTE",
                ",": "COMMA", "<": "COMMA",
                ".": "PERIOD", ">": "PERIOD",
                "/": "SLASH", "?": "SLASH"
            };

            let newKey = "";
            if (numpadMap[code]) {
                newKey = numpadMap[code];
            } else if (specialKeyMap[rawKey]) {
                newKey = specialKeyMap[rawKey];
            } else {
                newKey = numberMap[rawKey] ||
                    (rawKey === " " ? "SPACE" : rawKey === "Tab" ? "TAB" : rawKey.length === 1 ? rawKey.toUpperCase() : rawKey.toUpperCase());
            }

            // 기타 특수키 보정
            if (newKey === "CONTROL") newKey = "CTRL";
            if (newKey === "ARROWUP") newKey = "UP";
            if (newKey === "ARROWDOWN") newKey = "DOWN";
            if (newKey === "ARROWLEFT") newKey = "LEFT";
            if (newKey === "ARROWRIGHT") newKey = "RIGHT";
            if (newKey === "ESCAPE") newKey = "ESC";

            setKeys(prev => {
                const displayKey = formatDisplayKey(newKey);
                const duplicateAction = Object.keys(prev).find(action => prev[action] === newKey && action !== listeningAction);
                if (duplicateAction) {
                    const currentBinding = prev[listeningAction];
                    toast.info(`'${displayKey}' 키가 이미 '${KEY_LABELS[duplicateAction]}'에 할당되어 있어 서로 교체되었습니다.`);
                    return { ...prev, [listeningAction]: newKey, [duplicateAction]: currentBinding };
                }
                return { ...prev, [listeningAction]: newKey };
            });
            setListeningAction(null);
        };

        const handleMouseDown = (e) => {
            if (!listeningAction) return;

            let mouseKey = "";
            if (e.button === 0) mouseKey = "LeftClick";
            else if (e.button === 2) mouseKey = "RightClick";
            else return;

            e.preventDefault();
            setKeys(prev => {
                const duplicateAction = Object.keys(prev).find(action => prev[action] === mouseKey && action !== listeningAction);
                if (duplicateAction) {
                    const currentBinding = prev[listeningAction];
                    toast.info(`'${mouseKey}' 키가 이미 '${KEY_LABELS[duplicateAction]}'에 할당되어 있어 서로 교체되었습니다.`);
                    return { ...prev, [listeningAction]: mouseKey, [duplicateAction]: currentBinding };
                }
                return { ...prev, [listeningAction]: mouseKey };
            });
            setListeningAction(null);
        };

        if (listeningAction) {
            window.addEventListener("keydown", handleKeyDown);
            window.addEventListener("mousedown", handleMouseDown);
        }

        // 전체 컨텍스트 메뉴 방지
        const preventMenu = (e) => e.preventDefault();
        window.addEventListener("contextmenu", preventMenu);

        return () => {
            window.removeEventListener("keydown", handleKeyDown);
            window.removeEventListener("mousedown", handleMouseDown);
            window.removeEventListener("contextmenu", preventMenu);
        };
    }, [listeningAction]);

    const handleSave = async () => {
        if (!isLoggedIn) {
            toast.error("키 설정을 저장하려면 로그인이 필요합니다.");
            return;
        }

        try {
            // 로컬 저장
            keyMapping.saveKeys(keys);

            // 백엔드 동기화
            await axios.post(
                "/api/key-config",
                { keyConfig: JSON.stringify(keys) },
                { withCredentials: true }
            );

            toast.success("키 설정이 서버에 저장되었습니다.");
            onBack();
        } catch (error) {
            console.error("키 설정 저장 오류:", error);
            toast.error("서버 저장 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="KeySetting-content">
            <div className="title">
                키 설정
                <button className="back_btn" onClick={onBack}>✕</button>
            </div>

            <div className="key_list">
                {Object.entries(KEY_LABELS).map(([action, label]) => (
                    <div className="key_item" key={action}>
                        <span className="label">{label}</span>
                        <button
                            className={`key_btn ${listeningAction === action ? "listening" : ""}`}
                            onClick={() => {
                                if (listeningAction === action) {
                                    setListeningAction(null);
                                } else {
                                    setListeningAction(action);
                                }
                            }}
                        >
                            {listeningAction === action ? "키 입력 대기 중..." : formatDisplayKey(keys[action])}
                        </button>
                    </div>
                ))}
            </div>

            <div className="footer">
                <button className="save_btn" onClick={handleSave}>저장하기</button>
                <button className="reset_btn" onClick={() => setKeys({ ...keyMapping.getKeys() })}>초기화</button>
            </div>
        </div>
    );
}
