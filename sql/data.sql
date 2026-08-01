-- 1. 기존 데이터 깨끗하게 비우기 (중복 방지)
TRUNCATE TABLE orders;
TRUNCATE TABLE settlement;

-- 2. 프로시저 정의 (100만건 더미데이터 생성)
DELIMITER $$

DROP PROCEDURE IF EXISTS fill_dummy_data$$

CREATE PROCEDURE fill_dummy_data()
BEGIN
    DECLARE i INT DEFAULT 1;

    -- 속도를 위해 오토커밋 끄고 트랜잭션 시작
    SET autocommit = 0;

    WHILE i <= 1000000 DO
        INSERT INTO orders (customer_name, store_name, amount, order_date)
        VALUES (
            CONCAT('고객_', i), -- 고객_1, 고객_2 ...
            CASE FLOOR(1 + RAND() * 5) -- 가게명 랜덤 선택
                WHEN 1 THEN '엽기떡볶이'
                WHEN 2 THEN '교촌치킨'
                WHEN 3 THEN '피자헛'
                WHEN 4 THEN '스타벅스'
                ELSE '김밥천국'
            END,
            FLOOR(1000 + RAND() * 50000), -- 1000 ~ 51000원 사이 랜덤 금액
            CASE
                -- 70% 확률로 "7일 전" 데이터 생성 (배치 대상)
                WHEN RAND() < 0.7 THEN DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
                -- 30% 확률로 "1~10일 전" 랜덤 날짜 생성 (배치 대상 아님)
                ELSE DATE_SUB(CURRENT_DATE, INTERVAL FLOOR(1 + RAND() * 10) DAY)
            END
        );

        SET i = i + 1;

        -- 1천건마다 커밋 (메모리 부하 방지)
        IF i % 1000 = 0 THEN
            COMMIT;
END IF;
END WHILE;

COMMIT;
SET autocommit = 1;
END$$

DELIMITER ;

-- 프로시저 실행
CALL fill_dummy_data();

-- 데이터 확인
select count(*) from orders;

-- 참고) 프로시저 삭제
-- DROP PROCEDURE IF EXISTS fill_dummy_data;