/**
 * 브라우저 ID를 생성하고 관리하는 유틸리티 함수
 */

// 로컬 스토리지 키
const BROWSER_ID_KEY = "word_game_browser_id";

/**
 * 브라우저 ID 생성
 * @returns 랜덤 UUID v4
 */
const generateBrowserId = (): string => {
	return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(
		/[xy]/g,
		function (c) {
			const r = (Math.random() * 16) | 0;
			const v = c === "x" ? r : (r & 0x3) | 0x8;
			return v.toString(16);
		}
	);
};

/**
 * 브라우저 ID 가져오기 (없으면 생성)
 */
export const getBrowserId = (): string => {
	// 서버 사이드 렌더링 대응 - window 객체가 없으면 임시 ID 반환
	if (typeof window === "undefined") {
		return generateBrowserId(); // 서버에서는 임시 ID 생성
	}

	// 브라우저 환경에서는 localStorage 사용
	let browserId = localStorage.getItem(BROWSER_ID_KEY);

	if (!browserId) {
		browserId = generateBrowserId();
		localStorage.setItem(BROWSER_ID_KEY, browserId);
	}

	return browserId;
};
