"use client";

interface WordDisplayProps {
	words: string[];
}

export function WordDisplay({ words }: WordDisplayProps) {
	return (
		<div className="relative h-20 overflow-hidden">
			<div className="absolute inset-0 flex items-center justify-center">
				{words.map((word, index) => (
					<div
						key={index}
						className="absolute transition-all duration-500 ease-in-out text-3xl"
						style={{
							opacity:
								index === words.length - 1
									? 1
									: Math.max(
											0.2,
											1 - (words.length - 1 - index) * 0.2
									  ),
							transform: `translateX(${
								(index - words.length + 1) * 100
							}px)`,
							color:
								index === words.length - 1 ? "black" : "gray",
						}}
					>
						{word}
					</div>
				))}
			</div>
		</div>
	);
}
