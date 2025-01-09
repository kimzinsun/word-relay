"use client";

import { useState, useEffect } from "react";
import { generateRandomNickname, isValidWord } from "../../utils/gameUtils";
import { getCurrentUsers, incrementUsers, decrementUsers } from "../actions";
import { WordDisplay } from "./WordDisplay";

export default function WordChainGame() {
	const [nickname, setNickname] = useState("");
	const [words, setWords] = useState(["시작"]);
	const [inputWord, setInputWord] = useState("");
	const [message, setMessage] = useState("");
	const [userCount, setUserCount] = useState(0);
	const [rankings] = useState([
		{ rank: 1, name: "player123", score: 2840 },
		{ rank: 2, name: "wordmaster", score: 2710 },
		{ rank: 3, name: "gamer456", score: 2680 },
		{ rank: 4, name: "wordup789", score: 2520 },
		{ rank: 5, name: "chainmaker", score: 2340 },
	]);

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

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (isValidWord(words[words.length - 1], inputWord)) {
			setWords((prev) => [...prev, inputWord]);
			setInputWord("");
			setMessage("");
		} else {
			setMessage("잘못된 단어입니다. 다시 시도해주세요.");
		}
	};

	return (
		<div className="w-full h-full mx-auto p-8 bg-white rounded-lg shadow-xl flex flex-col min-h-screen">
			<div id="header" className="pb-8">
				<div className="flex justify-between items-center">
					<h1 className="text-lg font-semibold mb-4">world-relay</h1>
					<div className="text-right mb-4">
						<span className="text-sm">
							현재 접속자: {userCount}명
						</span>
					</div>
				</div>
			</div>
			<section className="flex-1 flex flex-col justify-center">
				<div className="mb-6 text-center">
					<p className="mb-4 text-lg">현재 단어</p>
					<WordDisplay words={words} />
					<p className="text-gray-500">{nickname}</p>
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
				</div>

				{message && (
					<p className="mt-4 text-red-500 text-lg text-center">
						{message}
					</p>
				)}

				<div className="mt-8 w-full max-w-screen-lg mx-auto">
					<h2 className="text-lg font-semibold mb-4 text-center">
						순위표
					</h2>
					<div className="overflow-hidden rounded-lg border border-gray-200">
						<table className="min-w-full divide-y divide-gray-200">
							<thead className="bg-gray-50">
								<tr>
									<th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
										순위
									</th>
									<th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
										닉네임
									</th>
									<th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
										점수
									</th>
								</tr>
							</thead>
							<tbody className="bg-white divide-y divide-gray-200">
								{rankings.map((rank) => (
									<tr
										key={rank.rank}
										className={
											rank.rank === 1
												? "bg-yellow-50"
												: ""
										}
									>
										<td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
											{rank.rank}위
										</td>
										<td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
											{rank.name}
										</td>
										<td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
											{rank.score.toLocaleString()}
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				</div>
			</section>

			<div id="footer" className="pt-8"></div>
		</div>
	);
}
