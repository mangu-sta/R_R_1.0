import { useEffect, useState } from "react";
import "../../css/Friend.css";

import List from "./FriendList";
import Add from "./FriendAdd";
import Request from "./FriendRequest";
import Sent from "./FriendSent";

export default function Friend({ onClose }) {
  const [pageName, setPageName] = useState("list");
  const [page, setPage] = useState(<List />);
  useEffect(() => {
    switch (pageName) {
      case "list":
        setPage(<List />);
        break;
      case "add":
        setPage(<Add />);
        break;
      case "request":
        setPage(<Request />);
        break;
        case "sent":
        setPage(<Sent />);
        break;
      default:
        setPage(<List />);
        break;
    }
  }, [pageName]);
  return (
    <div className="friend-content">
      <div className="title">
        친구
        <button onClick={onClose}>✕</button>
      </div>
      <div className="friend-main">
        <div className="buttons">
          <div
            className={pageName === "list" ? "selected" : ""}
            onClick={() => setPageName("list")}
          >
            친구목록
          </div>
          <div
            className={pageName === "request" ? "selected" : ""}
            onClick={() => setPageName("request")}
          >
            받은요청
          </div>
          <div
            className={pageName === "add" ? "selected" : ""}
            onClick={() => setPageName("add")}
          >
            친구신청
          </div>
          <div
            className={pageName === "sent" ? "selected" : ""}
            onClick={() => setPageName("sent")}
          >
            보낸요청
          </div>
        </div>
        {page}
      </div>
    </div>
  );
}
