"use client";

import { useState, useEffect } from "react";
import { generateRandomNickname, isValidWord } from "../../utils/gameUtils";
import { getCurrentUsers, incrementUsers, decrementUsers } from "../actions";

export default function WordChainGame() {
	const [nickname, setNickname] = useState("");
	const [currentWord, setCurrentWord] = useState("시작");
	const [inputWord, setInputWord] = useState("");
	const [message, setMessage] = useState("");
	const [userCount, setUserCount] = useState(0);

	useEffect(() => {
		setNickname(generateRandomNickname());
		const updateUserCount = async () => {
			const count = await incrementUsers();
			setUserCount(count);
		};
		updateUserCount();

		const interval = setInterval(async () => {
			const count = await getCurrentUsers();
			setUserCount(count);
		}, 5000);

		return () => {
			clearInterval(interval);
			decrementUsers();
		};
	}, []);

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		if (isValidWord(currentWord, inputWord)) {
			setCurrentWord(inputWord);
			setInputWord("");
			setMessage("");
		} else {
			setMessage("잘못된 단어입니다. 다시 시도해주세요.");
		}
	};

	return (
		<div className="w-full h-full mx-auto p-8 bg-white rounded-lg shadow-xl">
			<h1 className="text-lg font-semibold mb-4">world-relay</h1>
			<div className="text-right mb-4">
				<span className="text-sm">현재 접속자: {userCount}명</span>
			</div>
			<div className="mb-6 text-center">
				<p className="mb-1">현재 단어</p>
				<p className="text-4xl font-bold">{currentWord}</p>
				<p className="mt-2 text-gray-500">행복한 사자</p>
			</div>
			<form onSubmit={handleSubmit} className="flex">
				<input
					type="text"
					value={inputWord}
					onChange={(e) => setInputWord(e.target.value)}
					className="flex-grow p-3 text-lg border border-gray-300 rounded-l"
					placeholder="다음 단어를 입력하세요"
				/>
				<button
					type="submit"
					className="bg-black text-white p-3 text-lg rounded-r hover:bg-gray-800"
				>
					제출
				</button>
			</form>
			{message && (
				<p className="mt-4 text-red-500 text-lg text-center">
					{message}
				</p>
			)}
		</div>
	);
}
