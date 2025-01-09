import requests
import xml.etree.ElementTree as ET
import mysql.connector
from dotenv import load_dotenv
import os
import time
from datetime import datetime, timedelta
import logging
import json

# 로깅 설정
logging.basicConfig(
    filename='dictionary_crawler.log',
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

class DictionaryCrawler:
    def __init__(self):
        load_dotenv()
        self.api_key = os.getenv("OPENDIT_ACCESS_KEY")
        self.api_url = "https://opendict.korean.go.kr/api/search"
        self.setup_database()
        self.setup_crawler_config()
        
    def setup_database(self):
        """데이터베이스 연결 설정"""
        try:
            self.db_config = {
                "user": os.getenv("DB_USER"),
                "password": os.getenv("DB_PASSWORD"),
                "host": os.getenv("DB_HOST"),
                "port": os.getenv("DB_PORT"),
                "database": os.getenv("DB_NAME"),
            }
            self.conn = mysql.connector.connect(**self.db_config)
            self.cursor = self.conn.cursor(buffered=True)
            logging.info("Database connection established")
        except Exception as e:
            logging.error(f"Database connection failed: {e}")
            raise

    def setup_crawler_config(self):
        """크롤러 설정 초기화"""
        self.MAX_REQUESTS_PER_DAY = 1000
        self.BATCH_SIZE = 100
        self.current_requests = 0
        self.last_request_date = None
        
        # 진행 상황 파일에서 복구
        self.progress = self.load_progress()
        
        self.CHOSUNG = {
            'ㄱ': 0, 'ㄲ': 1, 'ㄴ': 2, 'ㄷ': 3, 'ㄸ': 4,
            'ㄹ': 5, 'ㅁ': 6, 'ㅂ': 7, 'ㅃ': 8, 'ㅅ': 9,
            'ㅆ': 10, 'ㅇ': 11, 'ㅈ': 12, 'ㅉ': 13, 'ㅊ': 14,
            'ㅋ': 15, 'ㅌ': 16, 'ㅍ': 17, 'ㅎ': 18
        }

    def load_progress(self):
        """진행 상황 로드"""
        try:
            with open('progress.json', 'r') as f:
                return json.load(f)
        except FileNotFoundError:
            return {
                'current_cho': 0,
                'current_syllable': 0,
                'current_start': 1
            }

    def save_progress(self):
        """진행 상황 저장"""
        with open('progress.json', 'w') as f:
            json.dump(self.progress, f)

    def generate_hangul_syllables(self, cho):
        """한글 음절 생성"""
        syllables = []
        cho_val = self.CHOSUNG[cho]
        for jung in range(21):
            for jong in range(28):
                syllable = chr(0xAC00 + (cho_val * 21 + jung) * 28 + jong)
                syllables.append(syllable)
        return syllables

    def wait_for_next_day(self):
        """API 제한 도달 시 다음날까지 대기"""
        now = datetime.now()
        tomorrow = now + timedelta(days=1)
        next_day = tomorrow.replace(hour=0, minute=0, second=0, microsecond=0)
        wait_seconds = (next_day - now).total_seconds()
        logging.info(f"Waiting until next day: {wait_seconds/3600:.2f} hours")
        time.sleep(wait_seconds)

    def make_api_request(self, params):
        """API 요청 처리"""
        now = datetime.now()
        
        if self.last_request_date and self.last_request_date.date() != now.date():
            self.current_requests = 0
            logging.info("Reset daily request counter")

        if self.current_requests >= self.MAX_REQUESTS_PER_DAY:
            logging.info("Daily limit reached")
            self.wait_for_next_day()
            self.current_requests = 0

        try:
            response = requests.get(self.api_url, params=params, timeout=10)
            response.raise_for_status()
            self.current_requests += 1
            self.last_request_date = now
            
            logging.info(f"API request {self.current_requests}/{self.MAX_REQUESTS_PER_DAY}")
            time.sleep(1)  # API 부하 방지
            return response
            
        except requests.exceptions.RequestException as e:
            logging.error(f"API request failed: {e}")
            raise

    def process_word(self, word, definition):
        """단어 처리 및 데이터베이스 저장"""
        word_without_hyphen = word.replace("-", "")
        try:
            self.cursor.execute("SELECT COUNT(*) FROM dictionary WHERE word = %s", 
                              (word_without_hyphen,))
            if self.cursor.fetchone()[0] == 0:
                self.cursor.execute(
                    'INSERT INTO dictionary (word, definition) VALUES (%s, %s)',
                    (word_without_hyphen, definition)
                )
                self.conn.commit()
                logging.info(f"Inserted: {word_without_hyphen}")
            else:
                logging.info(f"Skipped duplicate: {word_without_hyphen}")
                
        except mysql.connector.Error as e:
            logging.error(f"Database error: {e}")
            self.conn.rollback()

    def crawl(self):
        """메인 크롤링 프로세스"""
        initial = ['ㄱ']  
        
        try:
            for cho_idx, cho in enumerate(initial):
                if cho_idx < self.progress['current_cho']:
                    continue

                syllables = self.generate_hangul_syllables(cho)
                
                for syl_idx, word in enumerate(syllables):
                    if (cho_idx == self.progress['current_cho'] and 
                        syl_idx < self.progress['current_syllable']):
                        continue

                    current_start = (self.progress['current_start'] 
                                   if cho_idx == self.progress['current_cho'] 
                                   and syl_idx == self.progress['current_syllable'] 
                                   else 1)

                    while True:
                        try:
                            params = {
                                "key": self.api_key,
                                "q": word,
                                "req_type": "xml",
                                "target": 1,
                                "type1": ["word"],
                                "type2": ["native"],
                                "start": current_start,
                                "num": self.BATCH_SIZE,
                                "method": "start",
                                "part": "word",
                                "advanced": "y",
                                "pos": [1],
                            }

                            response = self.make_api_request(params)
                            root = ET.fromstring(response.content)
                            
                            items = root.findall("./item")
                            if not items:
                                logging.info(f"No results for {word}")
                                break

                            for item in items:
                                word_elem = item.find('word')
                                for sense in item.findall('sense'):
                                    definition = sense.find('definition').text
                                    self.process_word(word_elem.text, definition)

                            if len(items) < self.BATCH_SIZE:
                                break

                            current_start += self.BATCH_SIZE
                            
                            # 진행상황 업데이트
                            self.progress.update({
                                'current_cho': cho_idx,
                                'current_syllable': syl_idx,
                                'current_start': current_start
                            })
                            self.save_progress()

                        except Exception as e:
                            logging.error(f"Error processing word {word}: {e}")
                            raise

        except KeyboardInterrupt:
            logging.info("Process interrupted by user")
            raise
        
        finally:
            self.cleanup()

    def cleanup(self):
        """리소스 정리"""
        try:
            if hasattr(self, 'cursor') and self.cursor:
                self.cursor.close()
            if hasattr(self, 'conn') and self.conn:
                self.conn.close()
            logging.info("Cleanup completed")
        except Exception as e:
            logging.error(f"Cleanup error: {e}")

def main():
    crawler = DictionaryCrawler()
    try:
        crawler.crawl()
    except KeyboardInterrupt:
        print("\nProcess interrupted by user")
        print(f"Progress saved at: 초성 '{crawler.progress['current_cho']}', "
              f"음절 {crawler.progress['current_syllable']}, "
              f"페이지 {crawler.progress['current_start']}")
    except Exception as e:
        print(f"Error occurred: {e}")
        logging.error(f"Critical error: {e}")
    finally:
        crawler.cleanup()

if __name__ == "__main__":
    main()