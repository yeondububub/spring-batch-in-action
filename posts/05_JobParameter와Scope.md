# JobParameter와 Scope

## JobParameter

Spring Batch의 경우 외부 혹은 내부에서 파라미터를 받아 여러 Batch 컴포넌트에서 사용할 수 있게 지원하고 있습니다.
이 파라미터를 Job Parameter라고 부릅니다.  
Job Parameter를 사용하기 위해선 항상 Spring Batch 전용 Scope를 선언해야하며 @StepScope와 @JobScope 2가지가 있습니다.

```java
@Value("#{jobParameters[파라미터명]}")
```

**JobScope**

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

**StepScope**

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
        return ((contribution, chunkContext) ->  {
            log.info(">>>>> scopeStep2Tesklet");
            log.info("requestDate: {}", requestDate);
            return RepeatStatus.FINISHED;
        });
    }
```

@JobScope는 Step 선언문에서 사용 가능하고, @StepScope는 Tasklet이나 ItemReader, ItemWriter, ItemProcessor에서 사용할 수 있습니다.

## @StepScope @JobScope

Spring Batch는 @StepScope와 @JobScope 라는 특별한 Bean Scope를 지원합니다. Spring Bean의 기본 Scope는 singleton입니다.
아래처럼 Spring Batch 컴포넌트 (Tasklet, ItemReader, ItemWriter, ItemProcessor 등)에 @StepScope를 사용하게 되면 
Spring Batch가 Spring 컨테이너를 통해 지정된 Step의 실행시점에 해당 컴포넌트를 Spring Bean으로 생성합니다.
마찬가지로 @JobScope는 Job 실행시점에 Bean이 생성 됩니다.
즉, Bean의 생성 시점을 지정된 Scope가 실행되는 시점으로 지연시킵니다.