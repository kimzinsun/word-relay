"use client";

import { useState, useEffect } from "react";
import { getCurrentUsers, incrementUsers, decrementUsers } from "../actions";
import WordDisplay from "./WordDisplay";
import Rankings from "./Rankings";
import { Moon, Sun } from "lucide-react";
import { useWordGameSocket } from "@/hooks/useWordGameSocket";
import { useRankingsSSE } from "@/hooks/useRankingsSSE";

const WEBSOCKET_URL = "ws://your-server-url/word-game";
const SSE_URL = "http://your-server-url/rankings";

export default function WordRelay() {
	const [inputWord, setInputWord] = useState("");
	const [userCount, setUserCount] = useState(0);
	const [darkMode, setDarkMode] = useState(false);

	const {
		words,
		error: socketError,
		isConnected,
		submitWord,
	} = useWordGameSocket(WEBSOCKET_URL);
	const { rankings, error: rankingsError } = useRankingsSSE(SSE_URL);

	useEffect(() => {
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
		submitWord(inputWord);
		setInputWord("");
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
			<div id="header" className="pb-8">
				<div className="flex justify-between items-center">
					<h1 className="text-lg font-semibold mb-4">word-relay</h1>
					<div className="flex items-center gap-4">
						<div
							className={`h-2 w-2 rounded-full ${
								isConnected ? "bg-green-500" : "bg-red-500"
							}`}
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

			<section className="flex-1 flex flex-col justify-center">
				<div className="mb-6 text-center">
					<p className="mb-1 text-lg">현재 단어</p>
					<WordDisplay words={words} darkMode={darkMode} />
				</div>

				<div className="w-full flex items-center justify-center mb-8">
					<form
						onSubmit={handleSubmit}
						className="flex max-w-screen-lg min-w-[50vw]"
					>
						<input
							type="text"
							value={inputWord}
							onChange={(e) => setInputWord(e.target.value)}
							className={`flex-grow p-3 text-lg border rounded-l transition-colors duration-300 ${
								darkMode
									? "bg-gray-800 border-gray-700 text-white placeholder-gray-400"
									: "bg-white border-gray-300 text-black placeholder-gray-500"
							}`}
							placeholder="다음 단어를 입력하세요"
						/>
						<button
							type="submit"
							className={`p-3 text-lg rounded-r transition-colors duration-300 ${
								darkMode
									? "bg-blue-600 hover:bg-blue-700 text-white"
									: "bg-black hover:bg-gray-800 text-white"
							}`}
						>
							제출
						</button>
					</form>
				</div>

				{(socketError || rankingsError) && (
					<p className="mt-4 text-red-500 text-lg text-center">
						{socketError || rankingsError}
					</p>
				)}

				<Rankings rankings={rankings} darkMode={darkMode} />
			</section>

			<div id="footer" className="pt-8"></div>
		</div>
	);
}
