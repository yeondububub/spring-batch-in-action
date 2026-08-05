package com.example.springbatchinaction.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScopeConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

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
}
