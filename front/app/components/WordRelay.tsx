"use client";

import { useState, useEffect, useRef } from "react";
import { getCurrentUsers, incrementUsers, decrementUsers } from "../actions";
import WordDisplay from "./WordDisplay";
import Rankings from "./Rankings";
import { Moon, Sun } from "lucide-react";
import { useStompGameSocket } from "@/hooks/useStompGameSocket";
import { useRankingsSSE } from "@/hooks/useRankingsSSE";
import { getBrowserId } from "@/utils/browserIdUtils";

// 테스트 페이지와 동일한 연결 주소 사용
const STOMP_URL = "http://localhost:8080/ws/game"; // SockJS 엔드포인트
const SSE_URL = "http://localhost:8080/ranking"; // SSE 엔드포인트

export default function WordRelay() {
	const [inputWord, setInputWord] = useState("");
	const [userCount, setUserCount] = useState(0);
	const [darkMode, setDarkMode] = useState(false);
	const [browserId, setBrowserId] = useState("");
	const messagesEndRef = useRef<HTMLDivElement>(null);

	const {
		words,
		error: socketError,
		isConnected,
		submitWord,
		userInfo,
		messages,
	} = useStompGameSocket(STOMP_URL, browserId);

	const { rankings, error: rankingsError } = useRankingsSSE(SSE_URL);

	// 메시지 영역 자동 스크롤
	useEffect(() => {
		if (messagesEndRef.current) {
			messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
		}
	}, [messages]);

	useEffect(() => {
		// 브라우저 ID 설정 (클라이언트 사이드에서만 실행)
		setBrowserId(getBrowserId());

		const updateUserCount = async () => {
			const count = await incrementUsers();
			setUserCount(count);
		};
		updateUserCount();

		const interval = setInterval(async () => {
			const count = await getCurrentUsers();
			setUserCount(count);
		}, 5000);

		const prefersDark = window.matchMedia(
			"(prefers-color-scheme: dark)"
		).matches;
		setDarkMode(prefersDark);

		return () => {
			clearInterval(interval);
			decrementUsers();
		};
	}, []);

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (inputWord.trim()) {
			submitWord(inputWord);
			setInputWord("");
		}
	};

	const toggleDarkMode = () => {
		setDarkMode(!darkMode);
	};

	return (
		<div
			className={`w-full h-full mx-auto p-8 rounded-lg shadow-xl flex flex-col min-h-screen transition-colors duration-300 ${
				darkMode ? "bg-gray-900 text-white" : "bg-white text-black"
			}`}
		>
			<div id="header" className="pb-6">
				<div className="flex justify-between items-center">
					<h1 className="text-2xl font-semibold">Word Relay Game</h1>
					<div className="flex items-center gap-4">
						<div
							className={`h-3 w-3 rounded-full ${
								isConnected ? "bg-green-500" : "bg-red-500"
							}`}
							title={isConnected ? "연결됨" : "연결 중..."}
						/>
						<button
							onClick={toggleDarkMode}
							className={`p-2 rounded-full ${
								darkMode
									? "bg-gray-700 text-yellow-300"
									: "bg-gray-200 text-gray-700"
							}`}
						>
							{darkMode ? <Sun size={20} /> : <Moon size={20} />}
						</button>
						<span
							className={`text-sm ${
								darkMode ? "text-gray-300" : "text-gray-600"
							}`}
						>
							현재 접속자: {userCount}명
						</span>
					</div>
				</div>
			</div>

			{/* 게임 정보 카드 */}
			<div
				className={`p-4 rounded-lg mb-6 ${
					darkMode ? "bg-gray-800" : "bg-gray-100"
				}`}
			>
				<div className="font-medium">
					닉네임: {userInfo?.nickname || "연결 중..."}
				</div>
				<div className="font-medium">점수: {userInfo?.score || 0}</div>
			</div>

			<section className="flex-1 flex flex-col">
				{/* 현재 단어 영역 */}
				<div className="mb-6 text-center">
					<p className="mb-1 text-lg">현재 단어</p>
					<WordDisplay words={words} darkMode={darkMode} />
				</div>

				{/* 메시지 영역 */}
				<div
					className={`mb-6 border rounded-lg p-4 h-64 overflow-y-auto ${
						darkMode
							? "border-gray-700 bg-gray-800"
							: "border-gray-200"
					}`}
				>
					{messages.map((msg, index) => (
						<div
							key={index}
							className={`py-2 px-3 my-1 rounded ${
								msg.type === "system"
									? darkMode
										? "bg-blue-900/30"
										: "bg-blue-50"
									: darkMode
									? "bg-green-900/30"
									: "bg-green-50"
							}`}
						>
							{msg.text}
						</div>
					))}
					<div ref={messagesEndRef} />
				</div>

				{/* 단어 입력 폼 */}
				<div className="w-full flex items-center justify-center mb-6">
					<form onSubmit={handleSubmit} className="flex w-full">
						<input
							type="text"
							value={inputWord}
							onChange={(e) => setInputWord(e.target.value)}
							className={`flex-grow p-3 text-lg border rounded-l transition-colors duration-300 ${
								darkMode
									? "bg-gray-800 border-gray-700 text-white placeholder-gray-400"
									: "bg-white border-gray-300 text-black placeholder-gray-500"
							}`}
							placeholder="단어를 입력하세요..."
							disabled={!isConnected}
						/>
						<button
							type="submit"
							className={`p-3 text-lg rounded-r transition-colors duration-300 ${
								darkMode
									? "bg-green-600 hover:bg-green-700 text-white"
									: "bg-green-600 hover:bg-green-700 text-white"
							}`}
							disabled={!isConnected}
						>
							입력
						</button>
					</form>
				</div>

				{/* 오류 메시지 */}
				{(socketError || rankingsError) && (
					<p className="my-4 text-red-500 text-lg text-center">
						{socketError || rankingsError}
					</p>
				)}

				{/* 랭킹 표시 */}
				{rankings.length > 0 && (
					<Rankings rankings={rankings} darkMode={darkMode} />
				)}
			</section>

			<div id="footer" className="pt-6">
				<p
					className={`text-center text-sm ${
						darkMode ? "text-gray-400" : "text-gray-500"
					}`}
				>
					{isConnected ? "서버에 연결됨" : "서버 연결 중..."}
				</p>
			</div>
		</div>
	);
}
