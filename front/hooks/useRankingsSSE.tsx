import { useState, useEffect } from 'react';

export interface RankingData {
  rank: number;
  name: string;
  score: number;
}

export const useRankingsSSE = (sseUrl: string) => {
  const [rankings, setRankings] = useState<RankingData[]>([]);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    const eventSource = new EventSource(sseUrl);

    eventSource.onmessage = (event) => {
      try {
        const newRankings = JSON.parse(event.data);
        setRankings(newRankings);
      } catch (err) {
        console.error('Error parsing rankings:', err);
        setError('순위 데이터를 불러오는데 실패했습니다.');
      }
    };

    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      setError('순위 업데이트 연결에 문제가 발생했습니다.');
      eventSource.close();
      
      // 재연결 시도
      setTimeout(() => {
        new EventSource(sseUrl);
      }, 5000);
    };

    return () => {
      eventSource.close();
    };
  }, [sseUrl]);

  return { rankings, error };
};