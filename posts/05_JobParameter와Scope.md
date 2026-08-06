# JobParameter와 Scope

## JobParameter

Spring Batch는 외부(CLI Arguments, Scheduler 등)나 내부에서 파라미터를 받아 배치 컴포넌트(Tasklet, ItemReader, ItemWriter 등)에서 활용할 수 있는 기능을 제공합니다.
이 파라미터를 **Job Parameter**라고 부릅니다.  

Job Parameter를 SpEL(Spring Expression Language) 표현식(`@Value("#{jobParameters['파라미터명']}")`)을 사용하여 Bean에 바인딩(Late Binding)하려면, **Spring Batch 전용 Scope인 `@StepScope` 또는 `@JobScope`를 반드시 선언**해야 합니다.

```java
@Value("#{jobParameters['파라미터명']}")
```

### @JobScope 사용 예시

`@JobScope`는 Step 선언부 메서드에 사용할 수 있습니다.

```java
    @Bean
    public Job scopeJob() {
        return new JobBuilder("scopeJob", jobRepository)
                .start(scopeStep1(null))
                .next(scopeStep2())
                .build();
    }

    @Bean
    @JobScope
    public Step scopeStep1(@Value("#{jobParameters['requestDate']}") String requestDate) {
        return new StepBuilder("scopeStep1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> scopeStep1");
                    log.info("requestDate: {}", requestDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
```

### @StepScope 사용 예시

`@StepScope`는 Tasklet, ItemReader, ItemWriter, ItemProcessor 등의 세부 컴포넌트 Bean 선언부에 사용할 수 있습니다.

```java
    @Bean
    public Step scopeStep2() {
        return new StepBuilder("scopeStep2", jobRepository)
                .tasklet(scopeStep2Tasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet scopeStep2Tasklet(@Value("#{jobParameters['requestDate']}") String requestDate) {
        return ((contribution, chunkContext) -> {
            log.info(">>>>> scopeStep2Tasklet");
            log.info("requestDate: {}", requestDate);
            return RepeatStatus.FINISHED;
        });
    }
```

## @StepScope와 @JobScope의 역할 및 장점

Spring Batch는 `@StepScope`와 `@JobScope`라는 특별한 Bean Scope를 제공합니다.  
기본적인 Spring Bean의 생성 범위(Scope)는 싱글톤(Singleton)입니다. 애플리케이션이 구동(Context 로딩)될 때 생성됩니다.

하지만 배치 컴포넌트에 `@StepScope`나 `@JobScope`를 적용하면 다음과 같은 변화와 장점이 생깁니다.

1. **지연 바인딩 (Late Binding / Lazy Initialization)**
   - Bean의 생성 시점을 애플리케이션 시작 시점이 아니라, **해당 Step이나 Job이 실행되는 시점으로 지연**시킵니다.
   - 덕분에 애플리케이션 구동 시점이 아닌, 실제 배치 실행 시점에 외부에서 전달받은 명령줄 인자(CLI Parameter)나 스케줄러 파라미터를 읽어와 파라미터로 할당할 수 있습니다.

2. **병렬 처리 안전성 (Thread Safety & State Isolation)**
   - 동일한 컴포넌트(예: PagingReader)를 여러 Step에서 병렬로 구동할 때, 각 Step 실행마다 별도의 Bean 인스턴스가 독립적으로 생성되므로 멤버 변수 상태가 섞이지 않고 안전하게 관리됩니다.