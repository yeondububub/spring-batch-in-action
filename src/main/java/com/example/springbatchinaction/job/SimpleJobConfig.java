package com.example.springbatchinaction.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SimpleJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job simpleJob(Step simpleStep1, Step simpleStepWithError) {
        return new JobBuilder("simpleJob", jobRepository)
                .start(simpleStepWithError)
                .next(simpleStep1)
                .build();
    }

    @Bean
    public Step simpleStep1() {
        return new StepBuilder("simpleStep1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("========================================");
                    log.info(">>>>> Step1");
                    log.info("========================================");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStepWithJobParam(@Value("#{jobParameters['requestDate']}") String requestDate) {
        return new StepBuilder("simpleStepWithJobParam", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("========================================");
                    log.info(">>>>> requestDate={}", requestDate);
                    log.info("========================================");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStepWithError(@Value("#{jobParameters['requestDate']}") String requestDate) {
        return new StepBuilder("simpleStepWithError", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    //throw new IllegalArgumentException("[SimpleJobConfig.simpleStepWithError] 에러발생");
                    log.info("========================================");
                    log.info(">>>>> requestDate={}", requestDate);
                    log.info("========================================");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
