interface WordDisplayProps {
	words: string[];
	darkMode?: boolean;
}

export default function WordDisplay({
	words,
	darkMode = false,
}: WordDisplayProps) {
	const displayWords = words.slice(-10);
	const lastWordIndex = displayWords.length - 1;
	const currentWord = displayWords[lastWordIndex];

	const minGap = 150;
	const charWidth = 25;
	const currentWordLength = currentWord?.length || 0;
	const gap = Math.max(minGap, currentWordLength * charWidth);

	return (
		<div className="relative h-20 overflow-hidden w-full">
			<div className="absolute inset-0 flex items-center justify-center">
				{displayWords.map((word, index) => {
					const isLastWord = index === lastWordIndex;
					const relativePosition = lastWordIndex - index;

					const displayWord = isLastWord
						? word
						: word.length > 4
						? word.slice(0, 4) + ".."
						: word;

					const position = isLastWord ? 0 : -(relativePosition * gap);

					return (
						<div
							key={index}
							className="absolute transition-all duration-500 ease-in-out text-3xl whitespace-nowrap"
							style={{
								opacity: isLastWord
									? 1
									: Math.max(
											0.3,
											1 - relativePosition * 0.15
									  ),
								transform: `translateX(${position}px) scale(${
									isLastWord ? 1 : 0.8
								})`,
								color: isLastWord
									? darkMode
										? "white"
										: "black"
									: darkMode
									? "#6B7280"
									: "gray",
								maxWidth: isLastWord ? "none" : "100px",
								overflow: "hidden",
								textOverflow: "ellipsis",
								transformOrigin: "center",
								zIndex: isLastWord ? 10 : 1,
							}}
						>
							{displayWord}
						</div>
					);
				})}
			</div>
		</div>
	);
}
