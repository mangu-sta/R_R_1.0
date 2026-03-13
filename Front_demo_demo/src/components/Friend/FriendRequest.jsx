import { useEffect, useState } from "react";
import "../../css/Friend.css";
import axios from "axios";
import { toast } from "react-toastify/unstyled";

export default function FriendRequest() {
  const [friendRequestList, setFriendRequestList] = useState([]);
  useEffect(() => {
    const load = async () => {
      const list = await getFriendRequest();
      setFriendRequestList(list);
    };
    load();
  }, []);
  return (
    <div className="friend-list">
      {friendRequestList.length === 0 && (
        <div id="no_friends">받은 요청이 없습니다</div>
      )}
      {friendRequestList.map((item, index) => {
        return (
          <div className="card" key={index}>
            <div>{item.senderNickname}</div>
            <div className="buttons">
              <button
                id="accept"
                onClick={() => {
                  accept(item.senderId, setFriendRequestList, index);
                }}
              >
                수락
              </button>
              <button
                id="deny"
                onClick={() => {
                  deny(item.senderId, setFriendRequestList, index);
                }}
              >
                거절
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}

const getFriendRequest = async () => {
  try {
    const response = await axios.post(
      "/api/friend-request/received",
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

const accept = async (senderId, setFriendRequestList, index) => {
  try {
    const response = await axios.post(
      "/api/friend-request/accept",
      {"senderId": senderId},
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);
    setFriendRequestList(prev => prev.filter((_, i) => i !== index))
    toast.success("친구가 추가되었습니다.");
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
const deny = async (senderId, setFriendRequestList, index) => {
  try {
    const response = await axios.post(
      "/api/friend-request/reject",
      {"senderId": senderId},
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);

    setFriendRequestList(prev => prev.filter((_, i) => i !== index))
    toast.success("거절되었습니다.");
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
