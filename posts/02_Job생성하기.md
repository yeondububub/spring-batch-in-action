# Job 실행해보기

이번에는 간단하게 Spring Batch Job을 생성하고 실행해보겠습니다.

## 의존성 설정하기

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.mysql:mysql-connector-j'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-batch-jdbc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testCompileOnly 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

기존에는 @EnableBatchProcessing를 붙혀줘야 했지만  
스프링 부트 3.0 이상 및 스프링 배치 5.0 이상 환경에서는 @EnableBatchProcessing을 쓰지 않는 것이 기본 권장 사항이다.

## Simple Job 생성하기

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class SimpleJobConfig {

    @Bean
    public Job simpleJob(JobRepository jobRepository, Step simpleStep1) {
        return new JobBuilder("simpleJob", jobRepository)
                .start(simpleStep1)
                .build();
    }

    @Bean
    public Step simpleStep1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("simpleStep1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("========================================");
                    log.info(">>>>>>> Step1");
                    log.info("========================================");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
```

- `JobBuilder("simpleJob", jobRepository)`
  - `simpleJob` 이름으로 Batch Job을 생성합니다.

- `StepBuilder("simpleStep1", jobRepository)`
  - `simpleStep1` 이름으로 step을 생성합니다.

- `.tasklet((contribution, chunkContext))`
  - Step 안에서 수행될 기능들을 명시합니다.

코드를 보면 Job 안에는 여러 Step이 존재하고, Step 안에 Tasklet이 존재합니다.

- Job
    - 배치 처리의 가장 큰 단위(전체 흐름)
    - 예시 : 매일 자정에 처리되는 ‘일일 계좌 잔액 정합성 검증’ Job

- Step
    - Job을 구성하는 세부 단계이자, 실제 작업이 일어나는 실행 단위
    - 하나의 Job은 여러 개의 Step으로 구성될 수 있음
    - 예시 : 계좌 잔액 정합성 검증 Step
        - Read (읽기): 원장 DB에서 고객 계좌의 '전날 마감 잔액'과 '오늘 하루 동안 발생한 입출금 거래 내역'을 읽어오기
        - Process (가공): (전날 마감 잔액 + 당일 입금액 - 당일 출금액)을 계산한 뒤, 이 값이 현재 시스템에 기록된 '최종 잔액'과 정확히 일치하는지 비교하여 검증
        - Write (쓰기): 일치하는 경우 해당 계좌의 검증 상태를 '정상'으로 업데이트하고, 불일치할 경우 '이상 탐지(오류) 테이블'에 별도로 저장하여 담당자가 확인할 수 있도록 기록하기

## Mysql에서 스프링 배치 구동하기

Spring Batch를 구동하기 위해서는 여러 메타 데이터 테이블이 필요합니다.

Spring Batch의 메타 데이터는 다음과 같은 내용들을 담고 있습니다.

- 이전에 실행한 Job이 어떤 것들이 있는지
- 최근 실패한 Batch Parameter가 어떤것들이 있고, 성공한 Job은 어떤것들이 있는지
- 다시 실행한다면 어디서 부터 시작하면 될지
- 어떤 Job에 어떤 Step들이 있었고, Step들 중 성공한 Step과 실패한 Step들은 어떤것들이 있는지

등등 Batch 어플리케이션을 운영하기 위한 메타데이터가 여러 테이블에 나눠져 있습니다.

다음 코드를 yml 설정파일에 작성하면 테이블을 자동으로 생성해줍니다.

```yml
sping:
  batch:
    jdbc:
      initialize-schema: always
```