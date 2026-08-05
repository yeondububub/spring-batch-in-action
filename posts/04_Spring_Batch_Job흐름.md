# Spring Batch Job Flow

Spring Batch의 Job에는 Step이 존재한다.
Step은 실제 Batch 작업을 수행하는 역할을 합니다.

실제 비즈니스 로직은 Step에서 구현된다.  
Job에는 여러개의 Step이 존재하기 때문에 순서를 처리하는것이 중요하다.

이번에는 이러한 스프링 배치의 흐름을 처리하는 방법에 대해 배워본다.

## Next

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job simpleJob() {
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

`next()`는 순차적으로 Step들 연결시킬때 사용됩니다.

## 조건별 흐름 제어

Next는 순차적으로 Step의 순서를 제어합니다.
하지만, 앞의 step에서 오류가 나면 나머지 뒤에 있는 step 들은 실행되지 못한다는 단점이 존재합니다.  
이럴 경우를 대비해 Spring Batch Job에서는 조건별로 Step을 사용할 수 있습니다.

다음과 같이 코드가 있다고 가정해봅시다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextConditionalJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job simpleJob() {
        return new JobBuilder("stepNextConditionalJob", jobRepository)
                .start(step1())
                    .on("FAILED")
                    .to(step3())
                    .on("*")
                    .end()
                .from(step1())
                    .on("*") // FAILED이외 모든 경우
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

위의 코드는 step1이 실패하냐 성공하냐에 따라 실행과정이 달라집니다..

- step1 실패 시나리오: step1 -> step3
- step1 성공 시나리오: step1 -> step2 -> step3

이런 흐름을 job에서 다음 코드를 통해 제어를 합니다.

- `.on()`
  - 캐치할 ExitStatus 설정한다.
  - `*` 일 경우 모든 ExitStatus가 지정된다.

- `to()`
  - 다음으로 이동할 Step 설정한다.

- `from()`
  - 일종의 이벤트 리스너 역할을 수행한다.
  - 상태값을 보고 일치하는 상태라면 to()에 포함된 step을 호출합니다.
  - step1의 이벤트 캐치가 FAILED로 되있는 상태에서 추가로 이벤트 캐치하려면 from을 써야만 함

- `end()`
  - end는 FlowBuilder를 반환하는 end와 FlowBuilder를 종료하는 end 2개가 있음
  - on("*")뒤에 있는 end는 FlowBuilder를 반환하는 end
  - build() 앞에 있는 end는 FlowBuilder를 종료하는 end
  - FlowBuilder를 반환하는 end 사용시 계속해서 from을 이어갈 수 있음

여기서 중요한 점은 `on`이 캐치하는 상태값이 `BatchStatus`가 아닌 `ExitStatus`라는 점입니다.
그래서 분기처리를 위해 상태값 조정이 필요하시다면 `ExitStatus`를 조정해야합니다.

실행 결과는 다음과 같이 나오게 됩니다.

<img src="./imgs/img012.png">

step1과 step3만 실행된 것을 확인할 수 있습니다.
ExitStatus.FAILED로 인해 step2가 무시되고 실행되었습니다.

이번에는 step1의 `ExitStatus.FAILED`를 제거해서 step1->step2->step3이 수행되도록 진행해보겠습니다.

<img src="./imgs/img013.png" height="180">

## BatchStatus와 ExitStatus의 차이점

BatchStatus는 Job 또는 Step 의 실행 결과를 Spring에서 기록할 때 사용하는 Enum으로 `COMPLETED`, `STARTING`, `STARTED` 등이 있습니다.

ExitStatus는 다음과 같이 Step의 실행 후 상태를 말합니다.

```java
public static final ExitStatus COMPLETED = new ExitStatus("COMPLETED");
```

간단히 설명해서
BatchStatus는 프레임워크가 배치의 실행 상태를 관리하기 위한 내부 Enum 값이며,  
ExitStatus는 실행이 끝난 후 어떤 결과가 나왔는지 정의하는 종료 코드(문자열)입니다.

## Decide

Decide는 Step들의 Flow속에서 분기만 담당합니다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeciderJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job simpleJob() {
        return new JobBuilder("DeciderJobConfiguration", jobRepository)
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
                })
                .build();
    }

    @Bean
    public Step oddStep() {
        return new StepBuilder("oddStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 홀수입니다.");
                    return RepeatStatus.FINISHED;
                })
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

            if(randomNumber % 2 == 0) {
                return new FlowExecutionStatus("EVEN");
            } else {
                return new FlowExecutionStatus("ODD");
            }
        }
    }
}
```

<img src="./imgs/img014.png" height="150">

decider를 통해 복잡한 분기로직이 필요하더라도 Step과는 명확히 역할과 책임이 분리된 채로 진행할 수 있게 되었습니다.

여기서는 Step으로 처리하는게 아니기 때문에 ExitStatus가 아닌 FlowExecutionStatus로 상태를 관리합니다.