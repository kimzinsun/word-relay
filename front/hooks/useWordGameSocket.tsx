import { useState, useEffect, useCallback } from 'react';

interface WordGameMessage {
  type: 'WORD_UPDATE' | 'WORD_SUBMIT' | 'ERROR';
  payload: any;
}

export const useWordGameSocket = (serverUrl: string) => {
  const [socket, setSocket] = useState<WebSocket | null>(null);
  const [words, setWords] = useState<string[]>(Array(9).fill("").concat(["시작"]));
  const [error, setError] = useState<string>("");
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const ws = new WebSocket(serverUrl);

    ws.onopen = () => {
      console.log('Connected to word game server');
      setIsConnected(true);
    };

    ws.onmessage = (event) => {
      const message: WordGameMessage = JSON.parse(event.data);
      
      switch (message.type) {
        case 'WORD_UPDATE':
          setWords(message.payload.words);
          break;
        case 'ERROR':
          setError(message.payload.message);
          break;
        default:
          console.warn('Unknown message type:', message.type);
      }
    };

    ws.onclose = () => {
      console.log('Disconnected from word game server');
      setIsConnected(false);
      // 재연결 로직
      setTimeout(() => {
        setSocket(new WebSocket(serverUrl));
      }, 3000);
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setError('연결 오류가 발생했습니다.');
    };

    setSocket(ws);

    return () => {
      ws.close();
    };
  }, [serverUrl]);

  const submitWord = useCallback((word: string) => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({
        type: 'WORD_SUBMIT',
        payload: { word }
      }));
    } else {
      setError('서버와 연결이 끊어졌습니다.');
    }
  }, [socket]);

  return {
    words,
    error,
    isConnected,
    submitWord
  };
};