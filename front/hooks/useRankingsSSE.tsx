import { useState, useEffect, useRef } from "react";
import { getBrowserId } from "@/utils/browserIdUtils";

interface RankingData {
	rank: number;
	name: string;
	score: number;
}

export const useRankingsSSE = (sseUrl: string) => {
	const [rankings, setRankings] = useState<RankingData[]>([]);
	const [error, setError] = useState<string>("");
	const [isConnected, setIsConnected] = useState<boolean>(false);
	const [isConnecting, setIsConnecting] = useState<boolean>(false);
	const eventSourceRef = useRef<EventSource | null>(null);
	const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
	const reconnectCountRef = useRef<number>(0);
	const maxReconnectAttempts = 5;

	// 연결 함수
	const connectToSSE = () => {
		try {
			const browserId = getBrowserId();
			const url = `${sseUrl}?browserId=${browserId}`;

			// 연결 시도 상태 업데이트
			setIsConnecting(true);
			setError("");

			// 기존 연결 종료
			if (eventSourceRef.current) {
				console.log("Closing existing SSE connection");
				eventSourceRef.current.close();
				eventSourceRef.current = null;
			}

			console.log(`Connecting to SSE: ${url}`);

			// 새 연결 생성
			const eventSource = new EventSource(url, {
				withCredentials: true,
			});

			eventSourceRef.current = eventSource;

			// 연결 성공 이벤트
			eventSource.onopen = () => {
				console.log("SSE connection opened");
				setIsConnecting(false);
				setIsConnected(true);
				reconnectCountRef.current = 0; // 연결 성공 시 재시도 카운트 초기화
			};

			// 메시지 수신 이벤트
			eventSource.addEventListener("connect", (event) => {
				console.log("Initial connection event received:", event);
			});

			// 랭킹 데이터 수신 이벤트
			eventSource.addEventListener("rankings", (event) => {
				try {
					console.log("Rankings data received");
					const data = JSON.parse(event.data);
					setRankings(data);
				} catch (e) {
					console.error("Error parsing rankings data:", e);
				}
			});

			// 기본 메시지 이벤트 (이름 없는 이벤트)
			eventSource.onmessage = (event) => {
				try {
					console.log("Generic message received:", event.data);
				} catch (e) {
					console.error("Error handling message:", e);
				}
			};

			// 오류 이벤트
			eventSource.onerror = (event) => {
				console.error("SSE connection error:", event);
				setIsConnecting(false);
				setIsConnected(false);
				setError("순위 서버 연결에 실패했습니다. 재연결 중...");

				// 오류 세부 정보 기록
				if (event instanceof Event) {
					const target = event.target as EventSource;
					if (target) {
						console.log(
							"EventSource readyState:",
							target.readyState
						);
					}
				}

				// 연결 종료
				eventSource.close();
				eventSourceRef.current = null;

				// 재연결 시도 횟수 제한
				if (reconnectCountRef.current < maxReconnectAttempts) {
					reconnectCountRef.current += 1;

					// 지수 백오프로 재연결 지연 시간 증가 (1초, 2초, 4초, 8초, 16초)
					const delay = Math.min(
						1000 * Math.pow(2, reconnectCountRef.current - 1),
						16000
					);

					console.log(
						`Attempting to reconnect in ${delay}ms (attempt ${reconnectCountRef.current}/${maxReconnectAttempts})`
					);

					if (reconnectTimeoutRef.current) {
						clearTimeout(reconnectTimeoutRef.current);
					}

					reconnectTimeoutRef.current = setTimeout(() => {
						connectToSSE();
					}, delay);
				} else {
					setError(
						"최대 재연결 시도 횟수를 초과했습니다. 수동으로 재연결해 주세요."
					);
				}
			};
		} catch (e: any) {
			console.error("Error creating SSE connection:", e);
			setIsConnecting(false);
			setIsConnected(false);
			setError(`SSE 연결을 생성하는 데 실패했습니다: ${e.message}`);
		}
	};

	// 초기 연결 및 정리
	useEffect(() => {
		// 초기 연결
		connectToSSE();

		// 클린업
		return () => {
			console.log("Cleaning up SSE connection");
			if (eventSourceRef.current) {
				eventSourceRef.current.close();
				eventSourceRef.current = null;
			}
			if (reconnectTimeoutRef.current) {
				clearTimeout(reconnectTimeoutRef.current);
				reconnectTimeoutRef.current = null;
			}
		};
	}, [sseUrl]);

	// 수동 재연결 함수
	const reconnect = () => {
		console.log("Manual reconnect requested");
		reconnectCountRef.current = 0; // 수동 재연결 시 카운트 초기화

		if (reconnectTimeoutRef.current) {
			clearTimeout(reconnectTimeoutRef.current);
			reconnectTimeoutRef.current = null;
		}

		connectToSSE();
	};

	return {
		rankings,
		error,
		isConnected,
		isConnecting,
		reconnect,
	};
};
