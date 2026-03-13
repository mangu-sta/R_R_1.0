import "../css/InviteSlot.css"

import { useEffect, useState } from "react";
import axios from "axios";

export default function InviteSlot({ userList, onCancel, onInvite }) {
  const [friendList, setFriendList] = useState([]);

  useEffect(() => {
    const load = async () => {
      const list = await getFriendList(userList);
      setFriendList(list);
    };
    load();
  }, [userList]);

  return (
    <div className="inviteCard">
      {friendList.length === 0 && (
        <div id="no_friends">친구신청을 통하여 친구를 만들어보세요</div>
      )}
      {friendList.map((item, index) => {
        return (
          <div className="card" key={index}>
            <div className="invite-name">{item.nickname}</div>
            <div className="invite-buttons">
              <button
                id="invite"
                onClick={() => {
                  onInvite(item.nickname);
                }}
              >
                초대
              </button>
            </div>
          </div>
        );
      })}
      <button className="cancel_btn" onClick={onCancel}>
        취소
      </button>
    </div>
  );
}

const getFriendList = async (userList) => {
  try {
    const response = await axios.post(
      "/api/friends",
      {},
      { withCredentials: true }
    );

    const result = response.data ?? [];

    console.log("전체 친구 목록:", result);

    // 🔥 현재 파티에 있는 유저들 제외
    const filtered = result.filter(
      (friend) => !userList.some((u) => u.nickname === friend.nickname)
    );

    console.log("파티 제외한 친구 목록:", filtered);

    return filtered;

  } catch (error) {
    if (error.response) {
      if (error.response.status === 401) {
        console.error("서버 오류:", 401, error.response.statusText);
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
    return []; // 오류 시 빈 배열
  }
};
