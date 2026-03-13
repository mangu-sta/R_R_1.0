import React, { useEffect, useRef, useState } from "react";
import "../css/GameChat.css";

export default function GameChat({ myNickname, messages, onSend, onClose, isOpen }) {
    const [text, setText] = useState("");
    const [transientMessages, setTransientMessages] = useState([]);
    const messagesEndRef = useRef(null);
    const inputRef = useRef(null);
    const prevMessagesLengthRef = useRef(messages.length);

    useEffect(() => {
        if (isOpen) {
            // 채팅창이 열리면 자동으로 입력창에 포커스
            setTimeout(() => inputRef.current?.focus(), 0);
            scrollToBottom();
            // 열렸을 때 transient 메시지는 비우기 (선택사항, 깔끔하게)
            setTransientMessages([]);
        }
    }, [isOpen]);

    useEffect(() => {
        scrollToBottom();

        // 새 메시지가 들어왔고, 채팅창이 닫혀있다면 transient에 추가
        if (messages.length > prevMessagesLengthRef.current) {
            const newMsgs = messages.slice(prevMessagesLengthRef.current);
            if (!isOpen) {
                const now = Date.now();
                const tempMsgs = newMsgs.map((msg, idx) => ({
                    ...msg,
                    id: `${now}-${idx}`,
                }));

                setTransientMessages((prev) => [...prev, ...tempMsgs]);

                // 3초 후 제거 (각각)
                tempMsgs.forEach((tMsg) => {
                    setTimeout(() => {
                        setTransientMessages((prev) => prev.filter((m) => m.id !== tMsg.id));
                    }, 3000);
                });
            }
        }
        prevMessagesLengthRef.current = messages.length;
    }, [messages, isOpen]);

    const scrollToBottom = () => {
        if (isOpen && messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    };

    const handleSend = () => {
        if (!text.trim()) {
            onClose(); // 빈 메시지면 그냥 닫기
            return;
        }
        onSend(text);
        setText("");
        // 전송 후 입력창을 계속 유지할지 닫을지 결정. 보통 엔터로 보내면 유지 or 닫기.
        // 현재 로직상 Enter toggle이므로, 여기선 보내고 닫기를 원했었음 (user request history). 
        // "enter을 치면 이전내용도 다쓸수있게" -> keeps window open? 
        // "채팅은 치면 좌측 하단에 3초정도 떠있다 사라지는 그런느낌을 원해 enter을 치면 이전내용도 다쓸수있게"
        // Interpretation: Enter opens full history. Typing sends. 
        // If user presses Enter while typing... usually sends and KEEPS open or CLOSES?
        // Let's keep it open for continuous chatting if that's standard, but original req was "Enter toggles".
        // Let's assume sending closes it for now based on "onClose()" in original code, OR user might want standard chat behavior.
        // Let's stick to: Enter to send, then Close. User can press Enter again to chat more.
        // Actually, improved UX: Enter sends. ESC closes. Enter on empty input closes.
        // I'll keep the existing "send and close" behavior for now as it matches "Enter toggles" pattern.
        onClose();
    };

    const handleKeyDown = (e) => {
        e.stopPropagation();
        if (e.key === "Enter") {
            handleSend();
        } else if (e.key === "Escape") {
            onClose();
        }
    };

    if (!isOpen) {
        return (
            <div className="game-chat-overlay-container">
                {transientMessages.map((msg) => (
                    <div key={msg.id} className="game-chat-fade-msg">
                        <span style={{ fontWeight: 'bold', marginRight: '5px' }}>
                            {msg.nickname}:
                        </span>
                        <span>{msg.message}</span>
                    </div>
                ))}
            </div>
        );
    }

    return (
        <div className="game-chat-container">
            <div className="game-chat-messages">
                {messages.map((msg, i) => (
                    <div key={i} className={`game-chat-line ${msg.nickname === myNickname ? "my-line" : ""}`}>
                        <span className="game-chat-user">[{msg.nickname}]</span>
                        <span className="game-chat-msg">{msg.message}</span>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>
            <div className="game-chat-input-area">
                <input
                    ref={inputRef}
                    type="text"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="메시지를 입력하세요..."
                    autoComplete="off"
                />
            </div>
        </div>
    );
}
