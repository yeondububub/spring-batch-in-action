package com.example.springbatchinaction.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ProcessorWriterJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private static final int CHUNK_SIZE = 5;

    @Bean
    public Job processorWriterJob() {
        return new JobBuilder("processorWriterJob", jobRepository)
                .start(processorWriterStep())
                .build();
    }

    @Bean
    public Step processorWriterStep() {
        return new StepBuilder("processorWriterStep", jobRepository)
                .<Integer, String>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(simpleNumberReader())
                .processor(compositeProcessor())
                .writer(compositeWriter())
                .build();
    }

    @Bean
    public ListItemReader<Integer> simpleNumberReader() {
        return new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5, 10, 20, 30));
    }

    @Bean
    public CompositeItemProcessor<Integer, String> compositeProcessor() {
        List<ItemProcessor<?, ?>> delegates = Arrays.asList(
                (ItemProcessor<Integer, Integer>) item -> (item % 2 == 0) ? item : null,
                (ItemProcessor<Integer, String>) item -> "Processed Value: " + (item * 100)
        );

        CompositeItemProcessor<Integer, String> compositeProcessor = new CompositeItemProcessor<>();
        compositeProcessor.setDelegates(delegates);
        return compositeProcessor;
    }

    @Bean
    public CompositeItemWriter<String> compositeWriter() {
        List<ItemWriter<? super String>> delegates = Arrays.asList(
                chunk -> log.info("[Writer 1 - DB/Log] Writing Chunk: {}", chunk.getItems()),
                chunk -> log.info("[Writer 2 - Audit] Total Items Count: {}", chunk.size())
        );

        CompositeItemWriter<String> compositeWriter = new CompositeItemWriter<>();
        compositeWriter.setDelegates(delegates);
        return compositeWriter;
    }
}
