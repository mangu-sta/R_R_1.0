import { useEffect, useState } from "react";
import "../../css/Friend.css";
import axios from "axios";

export default function FriendList() {
  const [friendList, setFriendList] = useState([]);
  useEffect(() => {
    const load = async () => {
      const list = await getFriendList();
      setFriendList(list);
    };
    load();
  }, []);
  return (
    <div className="friend-list">
      {friendList.length === 0 && (
        <div id="no_friends">친구신청을 통하여 친구를 만들어보세요</div>
      )}
      {friendList.map((item, index) => {
        return (
          <div className="card" key={index}>
            <div>{item.nickname}</div>
            <div className="buttons">
              <button
                id="delete"
                onClick={() => {
                  deleteFriend(item.nickname, setFriendList, index);
                }}
              >
                삭제
              </button>
              <button
                id="block"
                onClick={() => {
                  blockFriend(item.nickname, setFriendList, index);
                }}
              >
                차단
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}

const getFriendList = async () => {
  try {
    const response = await axios.post(
      "/api/friends",
      {},
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);
    return result;
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
  }
};

const deleteFriend = async (nickname, setFriendList, index) => {
  try {
    const response = await axios.post(
      "/api/friends/delete",
      { friendNickname: nickname },
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);

    setFriendList((prev) => prev.filter((_, i) => i !== index));
    toast.success("삭제 되었습니다.");
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
  }
};
const blockFriend = async (nickname, setFriendList, index) => {
  try {
    const response = await axios.post(
      "/api/friends/block",
      { targetNickname: nickname },
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);

    setFriendList((prev) => prev.filter((_, i) => i !== index));
    toast.success("차단 되었습니다.");
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
  }
};
