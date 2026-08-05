## 프로젝트 개요

- 프로젝트명 : 일별 가맹점 정산 배치 시스템 구축
- 목표 : 매일 발생하는 대량의 주문데이터를 확인하여, 정산 주기가 도래한 건에 대해 수수료를 제외하고 가맹점주에게 정산해주는 자동화 배치 시스템을 구현한다.

## 비즈니스 시나리오

- 배달 플랫폼은 고객이 결제한 금액을 즉시 점주에게 주지 않고, 일정 기간 보관한다. (고객의 환불 요청이나 구매 확정 기간 때문
- 우리의 정산 정책은 `‘주문 발생일로부터 7일 뒤’` 에 정산하는 것이다.
- `매일 새벽 4시`, 어제 까지 구매 확정이 난 주문 건들을 모아 수수료를 제외하고 정산 테이블에 데이터를 생성한다.

---

## 상세 요구사항(기능적 요구 사항)

- 대상 데이터 선정(Reader)
    - 전체 주문 데이터 중, 오늘 날짜 기준으로 정확히 `7일 전`에 생성된 주문 데이터를 조회해야 한다.
    - 예를 들어, 오늘이 2025년 1월 28일 이라면, 2025년 1월 21일 주문 건만 가져와야 한다.
- 정산 금액 계산(Processor)
    - 플랫폼 이용 수수료는 주문 금액의 `3%`로 설정한다.
    - 정산 금액 = 주문 금액 - (주문 금액 * 0.03)
- 데이터 저장(Writer)
    - 계산된 정산 내역은 settlement(정산) 테이블에 저장 되어야 한다.
- 자동 실행(Scheduler)
    - 이 배치프로그램은 별도의 실행 명령 없이 `매일 새벽 04:00에 자동으로 실행`되어야 한다.

## 상세 요구사항(비기능적 요구 사항)

- 개발 환경
    - Language: Java 21
    - Framework: Spring Boot 4.0.2 (Spring Batch 6.x)
    - Build Tool: Gradle
    - Database: MySQL (JPA 사용)
- 아키텍처
    - 대용량 처리를 고려하여 **`Chunk 지향 처리 (Chunk-oriented Processing)`** 방식을 사용한다.
    - **`Chunk Size`**는 **`1000`**으로 설정한다.
- 확장성 고려
    - 향후 서버 확장(Scale-out) 및 젠킨스(Jenkins) 연동을 고려하여, `외부 파라미터(CLI Arguments)로 날짜를 입력받아 실행할 수 있도록 구현` 해야 한다.
    - `JobParameters` 를 활용하여 멱등성(같은 파라미터로 다시 실행해도 안전함)을 유지하거나, 재시도 가능하게 설계한다.

---

## 테이블 명세서

- 주문 테이블: orders
    - `id(PK)`: 식별자
    - `customer_name`: 주문자
    - `store_name`: 가맹점명
    - `amount`: 결제금액
    - `order_date`: 주문일자
- 정산 테이블: settlement
    - `id(PK)`: 식별자
    - `order_id`: 주문번호
    - `store_name`: 가맹점명
    - `settlement_amount`: 최종정산금액
    - `settlement_date`: 정산처리일

## 데이터 흐름도

- 주문 테이블(orders)
    - 주문번호 : 1
    - 주문자 : 김시연
    - 가맹점명 : 서대문 엽기 떡볶이
    - 결제금액 : 10000원
    - 주문일자 : 2025-01-21
- Batch Job 실행 - 2025-01-28 새벽 4시
- ItemReader : 2025-01-21 주문건 조회
- ItemProcessor : 수수료 300원 차감(10,000 * 0.03) ⇒ 9700원 확정
- itemWriter : 정산 테이블에 insert
- 정산 테이블(Settlement)
    - 정산번호 : 1
    - 주문번호 : 1
    - 가맹점명: 서대문 엽기 떡볶이
    - 최종 정산금액 : 9,700원
    - 정산처리일 : 2025-01-28

## 배치 작업 흐름

- 데이터 흐름 분석
  - 개별 반복 구간 (One-by-One)
    - **ItemReader:** DB에서 데이터를 **단건(1건)** 읽어옵
      - 참고) DB에서 가져오는 단위: 1000개 (pageSize), Spring Batch 내부에서 처리되는 단위: 1개씩
        - ItemReader 에서 pageSize 를 `1000` 으로 설정했으므로, DB에서 SELECT 는 1000개씩 조회한다. ( 1000개를 한 번에 가져와서 메모리에 올려 둔다 )
        - 그리고 나서, 내부적으로 ItemReader.read() 시, read()는 호출될 때마다 1건씩 반환. ( read() 호출될 때마다 하나씩 꺼내서 반환 )
    - **ItemProcessor:** 읽어온 데이터를 **단건(1건)** 가공함
    - **Buffer(임시 저장소):** 가공된 데이터를 메모리 상의 리스트(Chunk)에 차곡차곡 쌓아놓음.
      - 아직 DB에 저장하지 않음
      - Chunk Size가 10이라면, 이 과정을 10번 반복.

  - 일괄 처리 구간 (Bulk Write)
    - **ItemWriter:** Chunk가 꽉 차면, **리스트(List) 통째로** 넘겨받음.
    - **Commit:** Writer가 `saveAll()` 등을 수행하면, 그때 트랜잭션이 **단 1번 커밋됨**
  - 여기서 가장 중요한점은 ItemReader 와 ItemProcessor 는 데이터를 한개씩 처리한다.
    - 데이터를 하나씩 처리 후 우리가 설정한 chunk 사이즈에 맞는 청크 1개가 생성되면, 이후에 ItemWriter가 청크 단위로 쓰기 작업을 시작한다.

## 소스코드

- [SettlementJobConfig.java](../src/main/java/com/example/springbatchinaction/job/SettlementJobConfig.java)