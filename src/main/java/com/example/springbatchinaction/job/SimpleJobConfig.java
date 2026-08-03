package com.example.springbatchinaction.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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
                    log.info(">>>>> Step1");
                    log.info("========================================");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
