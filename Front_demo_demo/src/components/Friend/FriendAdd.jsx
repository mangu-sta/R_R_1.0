import "../../css/Friend.css";
import { toast } from "react-toastify";
import axios from "axios";
import { useState } from "react";

export default function FriendAdd() {
  const [friendName, setFriendName] = useState("");
  return (
    <div className="friend-add">
      <input
        type="text"
        onChange={(e) => setFriendName(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") sendFriendRequest(friendName, setFriendName);
        }}
      />
      <button
        onClick={(e) => {
          sendFriendRequest(friendName, setFriendName);
        }}
      >
        전송
      </button>
    </div>
  );
}

const sendFriendRequest = async (friendName, setFriendName) => {
  // console.log(friendName);
  try {
    const response = await axios.post(
      "/api/friend-request/send",
      {"receiverNickname":friendName},
      { withCredentials: true }
    );
    setFriendName("");
    toast.success(`${friendName}님 에게 친구신청을 완료하였습니다.`)
  } catch (error) {
    if (error.response) {
      if (error.response.status === 400 || error.response.status === 404) {
        toast.error(error.response.statusText.message);
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

const deleteFriend = () => {};
const blockFriend = () => {};
