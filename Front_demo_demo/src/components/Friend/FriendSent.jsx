import { useEffect, useState } from "react";
import "../../css/Friend.css";
import axios from "axios";
import { toast } from "react-toastify/unstyled";

export default function FriendSent() {
  const [FriendSentList, setFriendSentList] = useState([]);
  useEffect(() => {
    const load = async () => {
      const list = await getFriendSent();
      setFriendSentList(list);
    };
    load();
  }, []);
  return (
    <div className="friend-list">
      {FriendSentList.length === 0 && (
        <div id="no_friends">보낸 요청이 없습니다</div>
      )}
      {FriendSentList.map((item, index) => {
        return (
          <div className="card" key={index}>
            <div>{item.receiverNickname}</div>
            <div className="buttons">
              <button
                id="cancel"
                onClick={() => {
                  cancel(item.receiverId, setFriendSentList, index);
                }}
              >
                취소
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}

const getFriendSent = async () => {
  try {
    const response = await axios.post(
      "/api/friend-request/sents",
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

const cancel = async (receiverId, setFriendSentList, index) => {
  try {
    const response = await axios.post(
      "/api/friend-request/cancel",
      { receiverId: receiverId },
      { withCredentials: true }
    );

    const result = response.data ?? {};

    console.log(result);
    setFriendSentList((prev) => prev.filter((_, i) => i !== index));
    toast.success("취소 되었습니다.");
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
