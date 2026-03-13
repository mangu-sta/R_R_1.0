import { useEffect, useRef, useState } from "react";
import "../css/ChatPopup.css";

export default function ChatPopup({ myNickname, onClose, messages, onSend }) {
  const [text, setText] = useState("");
  const messagesEndRef = useRef(null);

  const handleSend = () => {
    if (!text.trim()) return;
    onSend(text);
    setText("");
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <div className="chat-modal">
      <div className="chat-box">
        <div className="chat-header">
          <span>채팅</span>
          <button className="close-btn" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="chat-messages">
          {messages.map((msg, i) => (
            <div
              key={i}
              className={
                "chat-message " +
                (msg.nickname === myNickname ? "my-chat-message" : "")
              }
            >
              <div className="chat-user">{msg.nickname}</div>
              <div className="chat-msg">{msg.message}</div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        <div className="chat-input-area">
          <input
            type="text"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
            placeholder="메시지를 입력하세요"
          />
          <button className="send-btn" onClick={handleSend}>
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
