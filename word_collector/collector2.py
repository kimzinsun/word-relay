import mysql.connector
from mysql.connector import Error
from dotenv import load_dotenv
import os
import logging
import pandas as pd
import glob
import re
import time
from functools import wraps

# 로깅 설정
logging.basicConfig(
    filename="dictionary_processor.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)

def retry_on_connection_error(max_retries=3, delay=1):
    def decorator(func):
        @wraps(func)
        def wrapper(self, *args, **kwargs):
            retries = 0
            while retries < max_retries:
                try:
                    if not self.conn.is_connected():
                        self.reconnect()
                    return func(self, *args, **kwargs)
                except (mysql.connector.Error, mysql.connector.errors.OperationalError) as e:
                    retries += 1
                    if retries == max_retries:
                        raise
                    logging.warning(f"Database connection error: {e}. Retrying... ({retries}/{max_retries})")
                    time.sleep(delay)
                    self.reconnect()
            return None
        return wrapper
    return decorator

class DictionaryProcessor:
    def __init__(self):
        load_dotenv()
        self.setup_database()
        self.setup_processor_config()
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
                # 추가된 설정
                "connect_timeout": 28800,  # 8시간
                "pool_size": 5,
                "pool_name": "dictionary_pool"
            }
            self.conn = mysql.connector.connect(**self.db_config)
            self.cursor = self.conn.cursor(buffered=True)
            logging.info("Database connection established")
        except Exception as e:
            logging.error(f"Database connection failed: {e}")
            raise

    def reconnect(self):
        """데이터베이스 재연결"""
        try:
            if hasattr(self, 'conn'):
                self.conn.close()
            self.conn = mysql.connector.connect(**self.db_config)
            self.cursor = self.conn.cursor(buffered=True)
            logging.info("Database reconnection successful")
        except Exception as e:
            logging.error(f"Database reconnection failed: {e}")
            raise

    def setup_processor_config(self):
        """프로세서 설정 초기화"""
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

    @retry_on_connection_error()
    def create_tables(self):
        """초성별 테이블 생성"""
        try:
            for table_name in self.CHOSUNG_TABLE_MAP.values():
                query = f"""
                CREATE TABLE IF NOT EXISTS `{table_name}` (
                    `id` INT AUTO_INCREMENT PRIMARY KEY,
                    `word` VARCHAR(100) NOT NULL,
                    `definition` TEXT NOT NULL,
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
        
        cho_list = list("ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ")
        if 0 <= char_code < len(cho_list):
            return cho_list[char_code]
                
        return None

    def clean_word(self, word):
        """단어 전처리"""
        if pd.isna(word):
            return None
        # 괄호와 숫자 제거 (예: "가(01)" -> "가")
        cleaned = ''.join(char for char in word if not char.isdigit() and char not in '()')
        # 앞뒤 공백 제거
        cleaned = cleaned.strip()
        # 하이픈 제거
        cleaned = cleaned.replace("-", "")
        # ^ 제거
        cleaned = cleaned.replace("^", "")
        # 중간 공백 제거
        cleaned = cleaned.replace(" ", "")
        return cleaned

    def clean_definition(self, definition):
        """뜻풀이 텍스트 정리"""
        if pd.isna(definition):
            return ""
        
        # 「숫자」 패턴 제거
        cleaned = re.sub(r'「\d+」\s*', '', definition)
        
        # 여러 줄바꿈을 하나의 공백으로 변경
        cleaned = re.sub(r'\s+', ' ', cleaned)
        
        # 앞뒤 공백 제거
        cleaned = cleaned.strip()
        
        return cleaned

    def is_pure_hangul(self, word):
        """순수 한글로만 이루어진 단어인지 확인"""
        if not word:
            return False
        for char in word:
            if not ("\uAC00" <= char <= "\uD7A3" or "\u1100" <= char <= "\u11FF"):
                return False
        return True

    @retry_on_connection_error()
    def process_word(self, word, definition):
        """단어 처리 및 데이터베이스 저장"""
        cleaned_word = self.clean_word(word)

        if not cleaned_word or len(cleaned_word) <= 1 or not self.is_pure_hangul(cleaned_word):
            logging.info(f"Skipped word: {word} (invalid or too short)")
            return

        try:
            initial = self.get_initial_consonant(cleaned_word)
            if initial not in self.CHOSUNG_TABLE_MAP:
                logging.warning(f"Invalid initial consonant for word: {cleaned_word}")
                return

            table_name = self.CHOSUNG_TABLE_MAP[initial]

            # 중복 검사
            query = f"SELECT COUNT(*) FROM {table_name} WHERE word = %s"
            self.cursor.execute(query, (cleaned_word,))

            if self.cursor.fetchone()[0] == 0:
                # 새로운 단어 삽입
                self.cursor.execute(
                    f"INSERT INTO {table_name} (word, definition) VALUES (%s, %s)",
                    (cleaned_word, self.clean_definition(definition)),
                )
                self.conn.commit()
                logging.info(f"Inserted: {cleaned_word}")
            else:
                logging.info(f"Skipped duplicate: {cleaned_word}")

        except mysql.connector.Error as e:
            logging.error(f"Database error for word {word}: {e}")
            self.conn.rollback()
            raise

    def process_csv_file(self, csv_path):
        """단일 CSV 파일 처리 - 명사만 필터링하여 저장"""
        try:
            logging.info(f"Processing file: {csv_path}")
            df = pd.read_csv(csv_path, encoding='utf-8')  # 인코딩 명시적 지정
            
            # '품사' 컬럼이 있는지 확인
            if '품사' not in df.columns:
                logging.error(f"'품사' column not found in {csv_path}")
                return
                
            # 명사 필터링 (「명사」 형식으로 되어있는 것 확인)
            df = df[df['품사'].str.contains('「명사」', na=False)]
            
            total_rows = len(df)
            processed = 0

            # 디버깅을 위한 초성별 카운트
            initial_counts = {}

            for index, row in df.iterrows():
                try:
                    word = row['어휘']
                    definition = row['뜻풀이']
                    
                    if pd.isna(word) or pd.isna(definition):
                        continue

                    # 초성 카운트 업데이트
                    cleaned_word = self.clean_word(word)
                    if cleaned_word:
                        initial = self.get_initial_consonant(cleaned_word)
                        if initial:
                            initial_counts[initial] = initial_counts.get(initial, 0) + 1

                    self.process_word(word, definition)

                    processed += 1
                    if processed % 1000 == 0:
                        logging.info(f"Processed {processed}/{total_rows} nouns in {csv_path}")

                except Exception as e:
                    logging.error(f"Error processing row {index} in {csv_path}: {e}")
                    continue

            logging.info(f"초성별 단어 수: {initial_counts}")
            logging.info(f"Completed processing {processed}/{total_rows} nouns in {csv_path}")

        except Exception as e:
            logging.error(f"Error processing CSV file {csv_path}: {e}")

    def process_directory(self, directory_path):
        """디렉토리 내의 모든 CSV 파일 처리"""
        try:
            csv_files = glob.glob(os.path.join(directory_path, "*.csv"))
            total_files = len(csv_files)
            
            if total_files == 0:
                logging.warning(f"No CSV files found in directory: {directory_path}")
                return

            logging.info(f"Found {total_files} CSV files in {directory_path}")

            for i, csv_file in enumerate(csv_files, 1):
                logging.info(f"Processing file {i}/{total_files}: {csv_file}")
                self.process_csv_file(csv_file)

            logging.info(f"Completed processing all {total_files} CSV files")

        except Exception as e:
            logging.error(f"Error processing directory {directory_path}: {e}")
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
    processor = DictionaryProcessor()
    try:
        csv_directory = "dictionary_files"  # CSV 파일들이 있는 디렉토리 경로
        processor.process_directory(csv_directory)
    except KeyboardInterrupt:
        print("\nProcess interrupted by user")
    except Exception as e:
        print(f"Error occurred: {e}")
        logging.error(f"Critical error: {e}")
    finally:
        processor.cleanup()

if __name__ == "__main__":
    main()