import { useState, useEffect, useCallback, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

interface GameMessage {
	type: "WORD_UPDATE" | "WORD_SUBMIT" | "ERROR";
	payload: any;
}

interface WelcomeMessage {
	nickname: string;
	score: number;
}

export const useStompGameSocket = (serverUrl: string, browserId: string) => {
	const [stompClient, setStompClient] = useState<Client | null>(null);
	const [words, setWords] = useState<string[]>(
		Array(9).fill("").concat(["시작"])
	);
	const [error, setError] = useState<string>("");
	const [isConnected, setIsConnected] = useState(false);
	const [userInfo, setUserInfo] = useState<{
		nickname: string;
		score: number;
	} | null>(null);
	const [messages, setMessages] = useState<
		{ text: string; type: "system" | "user" }[]
	>([]);
	const sessionIdRef = useRef<string>("");

	useEffect(() => {
		// browserId가 비어있으면 연결하지 않음 (SSR 시에는 연결 안함)
		if (!browserId) {
			return;
		}

		// STOMP 클라이언트 생성
		const client = new Client({
			webSocketFactory: () => new SockJS(serverUrl),
			debug: (str) => {
				console.log(str);
			},
			reconnectDelay: 5000,
			heartbeatIncoming: 4000,
			heartbeatOutgoing: 4000,
			onConnect: (frame) => {
				console.log("Connected to STOMP: ", frame);
				setIsConnected(true);

				// 세션 ID 저장 (가능한 경우)
				if (frame.headers["session"]) {
					sessionIdRef.current = frame.headers["session"];
				}

				// 웰컴 메시지 구독
				client.subscribe("/user/queue/welcome", (message) => {
					try {
						const welcomeData = JSON.parse(message.body);
						setUserInfo(welcomeData);
						addMessage(
							`게임에 접속했습니다. 환영합니다, ${welcomeData.nickname}님!`,
							"system"
						);
					} catch (e) {
						console.error("Error parsing welcome message", e);
					}
				});

				// 시스템 메시지 구독 (전체 메시지)
				client.subscribe("/topic/messages", (message) => {
					try {
						const messageData = JSON.parse(message.body);
						addMessage(messageData.message, "system");

						// 단어 업데이트가 포함되어 있으면 단어 목록 업데이트
						if (
							messageData.words &&
							Array.isArray(messageData.words)
						) {
							setWords(messageData.words);
						}
					} catch (e) {
						console.error("Error parsing system message", e);
					}
				});

				// 오류 메시지 구독
				client.subscribe("/user/queue/errors", (message) => {
					const errorMessage = message.body;
					setError(errorMessage);
					addMessage(`오류: ${errorMessage}`, "system");
				});

				// 연결 메시지 전송
				client.publish({
					destination: "/app/game.connect",
					body: JSON.stringify({
						browserId: browserId,
					}),
				});
			},
			onStompError: (frame) => {
				console.error("STOMP Error:", frame);
				setError(
					"연결 오류가 발생했습니다: " + frame.headers["message"]
				);
				setIsConnected(false);
			},
		});

		// 연결 시작
		client.activate();
		setStompClient(client);

		// 컴포넌트 언마운트 시 정리
		return () => {
			if (client.connected) {
				client.deactivate();
			}
		};
	}, [serverUrl, browserId]);

	// 메시지 추가 함수
	const addMessage = useCallback((text: string, type: "system" | "user") => {
		setMessages((prev) => [...prev, { text, type }]);
	}, []);

	// 단어 제출 함수
	const submitWord = useCallback(
		(word: string) => {
			if (stompClient && stompClient.connected) {
				stompClient.publish({
					destination: "/app/game.word",
					body: JSON.stringify({
						sessionId: sessionIdRef.current,
						word: word,
					}),
				});
				addMessage(`입력: ${word}`, "user");
			} else {
				setError("서버와 연결이 끊어졌습니다.");
				addMessage("서버와 연결이 끊어졌습니다.", "system");
			}
		},
		[stompClient, addMessage]
	);

	return {
		words,
		error,
		isConnected,
		submitWord,
		userInfo,
		messages,
	};
};
