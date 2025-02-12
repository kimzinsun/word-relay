import requests
import xml.etree.ElementTree as ET
import mysql.connector
from dotenv import load_dotenv
import os
import time
from datetime import datetime, timedelta
import logging
import json
import unicodedata

# 로깅 설정
logging.basicConfig(
    filename="dictionary_crawler.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)


class DictionaryCrawler:
    def __init__(self):
        load_dotenv()
        self.api_key = os.getenv("OPENDIT_ACCESS_KEY")
        self.api_url = "https://stdict.korean.go.kr/api/search.do"
        self.setup_database()
        self.setup_crawler_config()
        self.create_tables()

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
        self.MAX_REQUESTS_PER_DAY = 40000
        self.BATCH_SIZE = 100
        self.current_requests = 0
        self.last_request_date = None

        # 진행 상황 파일에서 복구
        self.progress = self.load_progress()

        # 초성-테이블명 매핑 정의 추가
        self.CHOSUNG_TABLE_MAP = {
            "ㄱ": "dict_g",
            "ㄲ": "dict_gg",
            "ㄴ": "dict_n",
            "ㄷ": "dict_d",
            "ㄸ": "dict_dd",
            "ㄹ": "dict_r",
            "ㅁ": "dict_m",
            "ㅂ": "dict_b",
            "ㅃ": "dict_bb",
            "ㅅ": "dict_s",
            "ㅆ": "dict_ss",
            "ㅇ": "dict_ng",
            "ㅈ": "dict_j",
            "ㅉ": "dict_jj",
            "ㅊ": "dict_ch",
            "ㅋ": "dict_k",
            "ㅌ": "dict_t",
            "ㅍ": "dict_p",
            "ㅎ": "dict_h",
        }

        self.CHOSUNG = {
            "ㄱ": 0,
            "ㄲ": 1,
            "ㄴ": 2,
            "ㄷ": 3,
            "ㄸ": 4,
            "ㄹ": 5,
            "ㅁ": 6,
            "ㅂ": 7,
            "ㅃ": 8,
            "ㅅ": 9,
            "ㅆ": 10,
            "ㅇ": 11,
            "ㅈ": 12,
            "ㅉ": 13,
            "ㅊ": 14,
            "ㅋ": 15,
            "ㅌ": 16,
            "ㅍ": 17,
            "ㅎ": 18,
        }

    def create_tables(self):
        """초성별 테이블 생성"""
        try:
            # 모든 테이블에 대해 생성 쿼리 실행
            for table_name in self.CHOSUNG_TABLE_MAP.values():
                query = f"""
                CREATE TABLE IF NOT EXISTS `{table_name}` (
                    `id` INT AUTO_INCREMENT PRIMARY KEY,
                    `word` VARCHAR(100) NOT NULL,
                    `definition` TEXT NOT NULL,
                    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY `word_idx` (`word`)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """
                self.cursor.execute(query)

            self.conn.commit()
            logging.info("All dictionary tables created successfully")
        except Exception as e:
            logging.error(f"Error creating tables: {e}")
            raise        

    def get_initial_consonant(self, word):
        """단어의 초성 추출"""
        if not word:
            return None
        
        char_code = ord(word[0]) - 0xAC00
        
        if char_code < 0:
            return None
            
        char_code = char_code // 28 // 21
        
        # CHOSUNG의 키 목록에서 해당 인덱스의 초성을 찾음
        for cho, idx in self.CHOSUNG.items():
            if idx == char_code:
                return cho
                
        return None

    def load_progress(self):
        """진행 상황 로드"""
        try:
            with open("progress.json", "r") as f:
                return json.load(f)
        except FileNotFoundError:
            return {"current_cho": 0, "current_syllable": 0, "current_start": 1}

    def save_progress(self):
        """진행 상황 저장"""
        with open("progress.json", "w") as f:
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

    def make_api_request(self, q, page=1):
        """API 요청 처리"""
        now = datetime.now()
        
        if self.last_request_date and self.last_request_date.date() != now.date():
            self.current_requests = 0
            logging.info("Reset daily request counter")

        if self.current_requests >= self.MAX_REQUESTS_PER_DAY:
            logging.info("Daily limit reached")
            self.wait_for_next_day()
            self.current_requests = 0

        max_retries = 3
        retry_delay = 5
        
        for attempt in range(max_retries):
            try:
                # API 문서에 맞게 파라미터 구성
                params = {
                    "key": self.api_key,
                    "q": q,
                    "req_type": "xml",
                    "target": "1",
                    "start": str(page),  # 페이지 번호 직접 사용
                    "num": str(self.BATCH_SIZE),
                    "method": "start",
                    "part": "word",
                    "sort": "dict",
                    "type1": "word",
                    "type2": "native",
                    "advanced": "y",
                    "pos": "1",
                }
                
                response = requests.get(
                    self.api_url,
                    params=params,
                    timeout=30
                )
                
                # 디버깅을 위한 URL 로깅
                logging.debug(f"Request URL: {response.url}")
                response.raise_for_status()
                
                # API 응답 검증
                root = ET.fromstring(response.content)
                error = root.find('.//error')
                if error is not None:
                    error_code = error.find('error_code').text
                    error_message = error.find('message').text
                    raise Exception(f"API Error: {error_code} - {error_message}")
                
                self.current_requests += 1
                self.last_request_date = now
                
                logging.info(f"API request {self.current_requests}/{self.MAX_REQUESTS_PER_DAY}")
                time.sleep(1)
                
                return root
                
            except requests.exceptions.Timeout:
                if attempt < max_retries - 1:
                    wait_time = retry_delay * (attempt + 1)
                    logging.warning(f"Request timed out. Retrying in {wait_time} seconds... (Attempt {attempt + 1}/{max_retries})")
                    time.sleep(wait_time)
                else:
                    logging.error("Max retries reached. Request failed.")
                    raise
                    
            except Exception as e:
                logging.error(f"API request failed: {e}")
                # 응답 내용 로깅
                if 'response' in locals():
                    logging.error(f"Response content: {response.content}")
                raise

    def is_pure_hangul(self, word):
        """순수 한글로만 이루어진 단어인지 확인"""
        for char in word:
            if not (
                "\uAC00" <= char <= "\uD7A3"
                or "\u1100" <= char <= "\u11FF"
                or char == "-"
                or char == " "
            ):
                return False
        return True

    def clean_word(self, word):
        """단어 전처리"""
        # 앞뒤 공백 제거
        cleaned = word.strip()
        # 하이픈 제거
        cleaned = cleaned.replace("-", "")
        # ^ 제거
        cleaned = cleaned.replace("^", "")
        # 중간 공백 제거
        cleaned = cleaned.replace(" ", "")
        return cleaned

    def process_word(self, word, definition):
        """단어 처리 및 데이터베이스 저장"""
        # 단어 전처리
        cleaned_word = self.clean_word(word)

        # 필터링 조건 검사
        if len(cleaned_word) <= 1 or not self.is_pure_hangul(cleaned_word):
            logging.info(f"Skipped word: {word} (length <= 1 or contains non-Hangul)")
            return

        try:
            # 초성 추출
            initial = self.get_initial_consonant(cleaned_word)
            if initial not in self.CHOSUNG_TABLE_MAP:
                logging.warning(f"Invalid initial consonant for word: {cleaned_word}")
                return

            # 해당 초성의 테이블명 가져오기
            table_name = self.CHOSUNG_TABLE_MAP[initial]

            # 중복 검사
            query = f"SELECT COUNT(*) FROM {table_name} WHERE word = %s"
            self.cursor.execute(query, (cleaned_word,))

            if self.cursor.fetchone()[0] == 0:
                # 새로운 단어 삽입
                self.cursor.execute(
                    f"INSERT INTO {table_name} (word, definition) VALUES (%s, %s)",
                    (cleaned_word, definition),
                )
                self.conn.commit()
                logging.info(f"Inserted: {cleaned_word}")
            else:
                logging.info(f"Skipped duplicate: {cleaned_word}")

        except mysql.connector.Error as e:
            logging.error(f"Database error for word {word}: {e}")
            self.conn.rollback()

    def crawl_single_word(self, word):
        """단일 검색어에 대한 크롤링 처리"""
        try:
            # 첫 요청으로 전체 결과 수 확인
            root = self.make_api_request(word, 1)
            total_elem = root.find(".//total")

            if total_elem is None:
                logging.error(f"Invalid API response for {word}: missing total count")
                return

            total_count = int(total_elem.text)

            if total_count == 0:
                logging.info(f"No results for {word}")
                return

            logging.info(f"Total results for {word}: {total_count}")

            # 전체 페이지 수 계산 (100개씩 표시)
            total_pages = (total_count + self.BATCH_SIZE - 1) // self.BATCH_SIZE
            seen_words = set()
            processed_count = 0

            # 각 페이지 처리
            for page in range(1, total_pages + 1):
                # 페이지 번호를 직접 start 파라미터로 사용
                root = self.make_api_request(word, page)

                items = root.findall(".//item")
                if not items:
                    logging.warning(f"No items found on page {page} for {word}")
                    break

                page_processed = 0

                for item in items:
                    word_elem = item.find("word")
                    if word_elem is not None and word_elem.text:
                        word_text = word_elem.text

                        if word_text in seen_words:
                            continue

                        seen_words.add(word_text)
                        page_processed += 1

                        for sense in item.findall("sense"):
                            definition = sense.find("definition")
                            if definition is not None and definition.text:
                                self.process_word(word_text, definition.text)

                processed_count += page_processed
                logging.info(
                    f"Processed page {page}/{total_pages} for {word}. Total processed: {processed_count}/{total_count}"
                )

                if page_processed == 0:
                    break

            if processed_count < total_count:
                logging.warning(
                    f"Warning: Only processed {processed_count} out of {total_count} results for {word}"
                )
            else:
                logging.info(
                    f"Successfully processed all {processed_count} results for {word}"
                )

        except Exception as e:
            logging.error(f"Error processing word {word}: {e}")
            raise

    def crawl(self):
        """메인 크롤링 프로세스"""
        initial = [
            "ㄱ",
            "ㄴ",
            "ㄷ",
            "ㄹ",
            "ㅁ",
            "ㅂ",
            "ㅅ",
            "ㅇ",
            "ㅈ",
            "ㅊ",
            "ㅋ",
            "ㅌ",
            "ㅍ",
            "ㅎ",
        ]

        try:
            for cho_idx, cho in enumerate(initial):
                if cho_idx < self.progress["current_cho"]:
                    continue

                syllables = self.generate_hangul_syllables(cho)

                for syl_idx, word in enumerate(syllables):
                    if (
                        cho_idx == self.progress["current_cho"]
                        and syl_idx < self.progress["current_syllable"]
                    ):
                        continue

                    try:
                        self.crawl_single_word(word)

                        # 진행상황 업데이트
                        self.progress.update(
                            {
                                "current_cho": cho_idx,
                                "current_syllable": syl_idx,
                                "current_start": 1,  # 새 단어 시작시 1로 리셋
                            }
                        )
                        self.save_progress()

                    except Exception as e:
                        logging.error(f"Error processing syllable {word}: {e}")
                        raise

        except KeyboardInterrupt:
            logging.info("Process interrupted by user")
            raise

        finally:
            self.cleanup()

    def cleanup(self):
        """리소스 정리"""
        try:
            if hasattr(self, "cursor") and self.cursor:
                self.cursor.close()
            if hasattr(self, "conn") and self.conn:
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
        print(
            f"Progress saved at: 초성 '{list(crawler.CHOSUNG.keys())[crawler.progress['current_cho']]}', "
            f"음절 {crawler.progress['current_syllable']}"
        )
    except Exception as e:
        print(f"Error occurred: {e}")
        logging.error(f"Critical error: {e}")
    finally:
        crawler.cleanup()


if __name__ == "__main__":
    main()

