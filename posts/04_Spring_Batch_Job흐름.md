# Spring Batch Job Flow

Spring Batch의 Job 안에는 하나 이상의 Step이 존재합니다.
Step은 실제로 배치 작업을 수행하는 단위입니다.

실제 비즈니스 로직은 Step에서 구현되며, Job 내에 여러 개의 Step이 존재하는 경우 이들의 실행 순서 및 조건을 제어하는 흐름(Flow) 관리가 매우 중요합니다.

이번 문서에서는 Spring Batch의 Job 흐름을 처리하는 다양한 방법에 대해 배워봅니다.

## Next (순차적 흐름)

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job stepNextJob() {
        return new JobBuilder("stepNextJob", jobRepository)
                .start(step1())
                .next(step2())
                .next(step3())
                .build();
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step1");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step2() {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step2");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step3() {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step3");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
```

<img src="./imgs/img011.png">

`next()`는 순차적으로 Step들을 연결시킬 때 사용됩니다. (`step1` -> `step2` -> `step3`)

## 조건별 흐름 제어 (Conditional Flow)

`next()` 방식은 순차적으로 Step의 순서를 제어합니다.
하지만, 이전 Step에서 오류가 발생하면 그 뒤에 연결된 Step들은 실행되지 못한다는 특징이 있습니다.  
상황에 따라 앞선 Step의 실행 결과(ExitStatus)에 따라 서로 다른 Step을 실행하거나 분기 처리해야 할 수 있습니다. 이럴 때 조건별 흐름 제어를 활용합니다.

다음과 같이 코드를 작성해 보겠습니다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextConditionalJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job stepNextConditionalJob() {
        return new JobBuilder("stepNextConditionalJob", jobRepository)
                .start(step1())
                    .on("FAILED")
                    .to(step3())
                    .on("*")
                    .end()
                .from(step1())
                    .on("*") // FAILED 이외의 모든 경우
                    .to(step2())
                    .next(step3())
                    .on("*")
                    .end()
                .end()
                .build();
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step1");

                    contribution.setExitStatus(ExitStatus.FAILED);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step2() {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step2");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step3() {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step3");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
```

위 코드는 `step1`의 실행 결과에 따라 다음과 같이 시나리오가 달라집니다.

- **step1 실패 시나리오**: `step1` -> `step3`
- **step1 성공 시나리오**: `step1` -> `step2` -> `step3`

Job Builder에서 흐름을 제어하는 핵심 메서드들은 다음과 같습니다.

- `.on()`
  - 캐치할 `ExitStatus`를 지정합니다.
  - `*`일 경우 모든 `ExitStatus`와 매칭됩니다.

- `.to()`
  - 지정한 상태 조건일 때 이동할 다음 Step을 설정합니다.

- `.from()`
  - 일종의 이벤트 리스너 역할을 수행합니다.
  - 지정한 Step의 실행 결과 상태를 새로 모니터링하여 조건 분기를 이어나갈 수 있습니다.
  - `step1`에 대한 `FAILED` 상태 캐치 설정 후, 추가 분기를 작성하려면 `.from(step1())`을 명시해야 합니다.

- `.end()`
  - `end()`는 `FlowBuilder`를 반환하는 `end()`와 Job 빌드를 종료하는 `end()` 2종류가 있습니다.
  - `.on("*").end()`에 위치한 `end()`는 Flow 구성을 일단 일단락하고 `FlowBuilder`를 반환하는 `end()`입니다.
  - `build()` 직전에 호출되는 `end()`는 전체 Flow 형성을 마감하는 `end()`입니다.

여기서 가장 중요한 점은 `.on()`이 캐치하는 상태값이 `BatchStatus`가 아니라 **`ExitStatus`**라는 점입니다.
따라서 배치 처리 중 분기 조건을 직접 제어하려면 `contribution.setExitStatus(...)`를 통해 `ExitStatus`를 변경해 주어야 합니다.

실행 결과는 다음과 같습니다.

<img src="./imgs/img011.png">

`step1`과 `step3`만 실행된 것을 확인할 수 있습니다.
`step1`에서 지정한 `ExitStatus.FAILED` 조건으로 인해 `step2`가 건너뛰어지고 바로 `step3`가 실행되었습니다.

이번에는 `step1`의 `ExitStatus.FAILED` 설정 코드를 제거하고 재실행하여 정상적으로 `step1` -> `step2` -> `step3` 흐름이 수행되는지 확인해 봅니다.

<img src="./imgs/img013.png" height="180">

## BatchStatus와 ExitStatus의 차이점

- **BatchStatus**: Job이나 Step의 **실행 상태**를 Spring Batch 프레임워크가 DB 메타테이블에 기록하기 위한 Enum 값입니다. (`COMPLETED`, `STARTING`, `STARTED`, `FAILED` 등)
- **ExitStatus**: Step 실행 종료 후의 **결과 조건(종료 코드)**을 나타내는 객체입니다. 

간단히 설명하자면,  
`BatchStatus`는 프레임워크 내부에서 배치의 진행/완료/실패 상태를 관리하기 위한 enum이고,  
`ExitStatus`는 흐름 제어(Flow Control)에 사용되는 종료 문자열 태그입니다.

## Decider (JobExecutionDecider)

앞서 본 조건별 흐름 제어 방식은 Step의 `ExitStatus`를 변경하거나 비즈니스 로직에 분기 코드가 섞이는 단점이 있습니다.
`JobExecutionDecider`를 사용하면 복잡한 분기 로직을 Step과 명확히 분리하여 전문적으로 흐름 제어만 담당하도록 처리할 수 있습니다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeciderJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job deciderJob() {
        return new JobBuilder("deciderJob", jobRepository)
                .start(step1())
                .next(decider())
                .from(decider())
                .on("ODD")
                    .to(oddStep())
                .from(decider())
                .on("EVEN")
                    .to(evenStep())
                .end()
                .build();
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Step1");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step evenStep() {
        return new StepBuilder("evenStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 짝수입니다.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step oddStep() {
        return new StepBuilder("oddStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 홀수입니다.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new OddDecider();
    }

    public static class OddDecider implements JobExecutionDecider {
        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
            Random rand = new Random();

            int randomNumber = rand.nextInt(50) + 1;
            log.info("랜덤숫자: {}", randomNumber);

            if (randomNumber % 2 == 0) {
                return new FlowExecutionStatus("EVEN");
            } else {
                return new FlowExecutionStatus("ODD");
            }
        }
    }
}
```

<img src="./imgs/img014.png" height="150">

`Decider`를 이용하면 아무리 복잡한 분기 로직이 필요하더라도 Step 본연의 역할과 책임(비즈니스 수행)을 건드리지 않고, 흐름 판별 전용 컴포넌트로 깔끔하게 분리할 수 있습니다.

Decider는 Step이 아니므로 `ExitStatus` 대신 **`FlowExecutionStatus`**를 반환하여 흐름을 제어합니다.