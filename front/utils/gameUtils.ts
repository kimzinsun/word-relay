export function generateRandomNickname(): string {
  const adjectives = ['행복한', '즐거운', '신나는', '재미있는', '멋진'];
  const nouns = ['사자', '호랑이', '코끼리', '기린', '팬더'];
  
  const randomAdjective = adjectives[Math.floor(Math.random() * adjectives.length)];
  const randomNoun = nouns[Math.floor(Math.random() * nouns.length)];
  
  return `${randomAdjective} ${randomNoun}`;
}

export function isValidWord(prevWord: string, newWord: string): boolean {
  if (newWord.length < 2) return false;
  return prevWord[prevWord.length - 1] === newWord[0];
}

