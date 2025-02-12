CREATE TABLE winning_last_word (
    id SERIAL PRIMARY KEY,
    last_char VARCHAR(10) NOT NULL
);

INSERT INTO winning_last_word (last_char)
SELECT DISTINCT last_char FROM winning_words
;
