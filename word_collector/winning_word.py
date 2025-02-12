import mysql.connector
from dotenv import load_dotenv
import os
import logging

# 로깅 설정
logging.basicConfig(
    filename="winning_words_finder.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)

class WinningWordFinder:
    def __init__(self):
        load_dotenv()
        self.setup_database()
        self.create_winning_words_table()

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

    def create_winning_words_table(self):
        """한방단어 테이블 생성"""
        try:
            query = """
            CREATE TABLE IF NOT EXISTS winning_words (
                id INT AUTO_INCREMENT PRIMARY KEY,
                word VARCHAR(100) NOT NULL,
                last_char VARCHAR(10) NOT NULL,
                UNIQUE KEY word_idx (word)
            ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """
            self.cursor.execute(query)
            self.conn.commit()
            logging.info("Winning words table created successfully")
        except Exception as e:
            logging.error(f"Error creating table: {e}")
            raise

    def check_word_exists(self, char):
        """특정 글자로 시작하는 단어가 있는지 확인"""
        # 초성 매핑
        tables_to_check = []

        if '가' <= char <= '깋':
            tables_to_check = ['dict_g']
        elif '까' <= char <= '낗':
            tables_to_check = ['dict_gg']
        elif '나' <= char <= '닣':
            tables_to_check = ['dict_n']
        elif '다' <= char <= '딯':
            tables_to_check = ['dict_d']
        elif '따' <= char <= '띻':
            tables_to_check = ['dict_dd']
        elif '라' <= char <= '맇':
            tables_to_check = ['dict_r']
        elif '마' <= char <= '밓':
            tables_to_check = ['dict_m']
        elif '바' <= char <= '빟':
            tables_to_check = ['dict_b']
        elif '빠' <= char <= '삫':
            tables_to_check = ['dict_bb']
        elif '사' <= char <= '싷':
            tables_to_check = ['dict_s']
        elif '싸' <= char <= '앃':
            tables_to_check = ['dict_ss']
        elif '아' <= char <= '잏':
            tables_to_check = ['dict_ng']
        elif '자' <= char <= '짛':
            tables_to_check = ['dict_j']
        elif '짜' <= char <= '찧':
            tables_to_check = ['dict_jj']
        elif '차' <= char <= '칳':
            tables_to_check = ['dict_ch']
        elif '카' <= char <= '킿':
            tables_to_check = ['dict_k']
        elif '타' <= char <= '팋':
            tables_to_check = ['dict_t']
        elif '파' <= char <= '핗':
            tables_to_check = ['dict_p']
        elif '하' <= char <= '힣':
            tables_to_check = ['dict_h']

        if not tables_to_check:
            return False

        try:
            for table_name in tables_to_check:
                # 해당 글자로 시작하는 단어 찾기
                query = f"SELECT COUNT(*) FROM {table_name} WHERE word LIKE %s"
                self.cursor.execute(query, (f"{char}%",))
                count = self.cursor.fetchone()[0]
                if count > 0:
                    return True
            return False
        except Exception as e:
            logging.error(f"Error checking word existence for {char}: {e}")
            return False

    def find_words_ending_with(self, char):
        """특정 글자로 끝나는 단어들 찾기"""
        winning_words = []
        for table_name in [
            'dict_g', 'dict_gg', 'dict_n', 'dict_d', 'dict_dd',
            'dict_r', 'dict_m', 'dict_b', 'dict_bb', 'dict_s',
            'dict_ss', 'dict_ng', 'dict_j', 'dict_jj', 'dict_ch',
            'dict_k', 'dict_t', 'dict_p', 'dict_h'
        ]:
            try:
                query = f"SELECT word FROM {table_name} WHERE word LIKE %s"
                self.cursor.execute(query, (f"%{char}",))
                words = self.cursor.fetchall()
                winning_words.extend([word[0] for word in words])
            except Exception as e:
                logging.error(f"Error finding words ending with {char} in {table_name}: {e}")
        return winning_words

    def find_winning_words(self):
        """한방단어 찾기"""
        try:
            # 가~힣까지 순회
            for code in range(ord('가'), ord('힣') + 1):
                char = chr(code)
                
                # 진행 상황 로깅 (100자마다)
                if code % 100 == 0:
                    progress = (code - ord('가')) / (ord('힣') - ord('가')) * 100
                    logging.info(f"Progress: {progress:.2f}% (checking: {char})")

                # 해당 글자로 시작하는 단어가 없는 경우
                if not self.check_word_exists(char):
                    # 해당 글자로 끝나는 단어들 찾기
                    winning_words = self.find_words_ending_with(char)
                    
                    # 데이터베이스에 저장
                    for word in winning_words:
                        try:
                            self.cursor.execute(
                                "INSERT IGNORE INTO winning_words (word, last_char) VALUES (%s, %s)",
                                (word, char)
                            )
                            logging.info(f"Found winning word: {word} (ends with {char})")
                        except Exception as e:
                            logging.error(f"Error inserting word {word}: {e}")
                            continue

                    self.conn.commit()

            logging.info("Completed finding winning words")
        except Exception as e:
            logging.error(f"Error in find_winning_words: {e}")
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
    finder = WinningWordFinder()
    try:
        finder.find_winning_words()
    except KeyboardInterrupt:
        print("\nProcess interrupted by user")
    except Exception as e:
        print(f"Error occurred: {e}")
        logging.error(f"Critical error: {e}")
    finally:
        finder.cleanup()

if __name__ == "__main__":
    main()