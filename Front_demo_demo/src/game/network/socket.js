import SockJS from "sockjs-client";
import Stomp from "stompjs";

import axios from "axios";

export const connectSocket = ({
    roomId,
    onMessage,
    onConnect,
    onError,
}) => {
    const socket = new SockJS(`http://${window.location.hostname}:8080/ws/lobby`);
    const client = Stomp.over(socket);
    // client.debug = (str) => console.log("[STOMP Debug] ", str); // 🔥 로우(Raw) 메시지 디버그 로그 켜기

    client.connect(
        {},
        async () => {
            console.log("📡 Lobby WebSocket 연결 성공");

            // 🔥 게임용 토픽('/topic/game/')과 로비용 토픽('/topic/lobby/')을 모두 구독
            // 로비용 토픽을 구독해야 멤버의 입장/퇴장(LEAVE/KICK) 메시지를 받을 수 있음
            const subGame = client.subscribe(`/topic/game/${roomId}`, (msg) => {
                if (onMessage) onMessage(JSON.parse(msg.body));
            });

            const subLobby = client.subscribe(`/topic/lobby/${roomId}`, (msg) => {
                if (onMessage) onMessage(JSON.parse(msg.body));
            });

            // 🔥 [추가] 개인용 초기화 메시지 큐 구독 (INIT 패킷 수신용)
            const subInit = client.subscribe(`/user/queue/game/init`, (msg) => {
                const data = JSON.parse(msg.body);
                // 백엔드 GameInitStateDto에 type="INIT"이 없을 경우를 대비한 보완
                if (!data.type) data.type = "INIT";
                if (onMessage) onMessage(data);
            });

            // 관리 편의를 위해 구독들을 하나로 묶음
            const combinedSubscription = {
                unsubscribe: () => {
                    subGame.unsubscribe();
                    subLobby.unsubscribe();
                    subInit.unsubscribe();
                }
            };

            if (onConnect) onConnect(client, combinedSubscription);

            try {
                // 이전에 있던 자동 start-request 코드는 삭제함
                // 필요시 여기서 초기화 API 등을 호출할 수 있음
            } catch (error) {
                console.error(error);
            }
        },
        (error) => {
            console.error("❌ WebSocket 연결 실패:", error);
            if (onError) onError(error);
        }
    );

    return client;
};
