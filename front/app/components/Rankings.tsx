interface RankingData {
	rank: number;
	name: string;
	score: number;
}

interface RankingsProps {
	rankings: RankingData[];
	darkMode: boolean;
}

export default function Rankings({ rankings, darkMode }: RankingsProps) {
	return (
		<div className="w-full max-w-screen-lg mx-auto">
			<h2 className="text-lg font-semibold mb-4 text-center">순위표</h2>
			<div
				className={`overflow-hidden rounded-lg border transition-colors duration-300 ${
					darkMode ? "border-gray-700" : "border-gray-200"
				}`}
			>
				<table className="min-w-full divide-y divide-gray-200">
					<thead
						className={`${darkMode ? "bg-gray-800" : "bg-gray-50"}`}
					>
						<tr>
							<th
								className={`px-6 py-3 text-left text-xs font-medium uppercase tracking-wider ${
									darkMode ? "text-gray-300" : "text-gray-500"
								}`}
							>
								순위
							</th>
							<th
								className={`px-6 py-3 text-left text-xs font-medium uppercase tracking-wider ${
									darkMode ? "text-gray-300" : "text-gray-500"
								}`}
							>
								닉네임
							</th>
							<th
								className={`px-6 py-3 text-left text-xs font-medium uppercase tracking-wider ${
									darkMode ? "text-gray-300" : "text-gray-500"
								}`}
							>
								점수
							</th>
						</tr>
					</thead>
					<tbody
						className={`divide-y ${
							darkMode
								? "bg-gray-900 divide-gray-700"
								: "bg-white divide-gray-200"
						}`}
					>
						{rankings.map((rank) => (
							<tr
								key={rank.rank}
								className={
									rank.rank === 1
										? darkMode
											? "bg-yellow-900/20"
											: "bg-yellow-50"
										: ""
								}
							>
								<td
									className={`px-6 py-4 whitespace-nowrap text-sm font-medium ${
										darkMode
											? "text-gray-100"
											: "text-gray-900"
									}`}
								>
									{rank.rank}위
								</td>
								<td
									className={`px-6 py-4 whitespace-nowrap text-sm ${
										darkMode
											? "text-gray-300"
											: "text-gray-500"
									}`}
								>
									{rank.name}
								</td>
								<td
									className={`px-6 py-4 whitespace-nowrap text-sm ${
										darkMode
											? "text-gray-300"
											: "text-gray-500"
									}`}
								>
									{rank.score.toLocaleString()}
								</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</div>
	);
}
