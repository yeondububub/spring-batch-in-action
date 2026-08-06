package com.example.springbatchinaction.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
//@Configuration
public class ChunkOrientedJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job chunkOrientedJob() {
        return new JobBuilder("chunkOrientedJob", jobRepository)
                .start(chunkStep())
                .build();
    }

    @Bean
    public Step chunkStep() {
        return new StepBuilder("chunkStep", jobRepository)
                .<Integer, String>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public ItemReader<Integer> itemReader() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 20);
        return new ListItemReader<>(numbers);
    }

    @Bean
    public ItemProcessor<Integer, String> itemProcessor() {
        return item -> {
            if (item > 10) {
                log.info("[Processor] Filtered (item > 10): {}", item);
                return null;
            }
            String processedValue = "Processed Item Value: " + (item * 10);
            log.info("[Processor] Processed: {} -> {}", item, processedValue);
            return processedValue;
        };
    }

    @Bean
    public ItemWriter<String> itemWriter() {
        return chunk -> {
            log.info("================ Bulk Writer Start ================");
            for (String item : chunk) {
                log.info("[Writer] Writing Item: {}", item);
            }
            log.info("================ Bulk Writer End (Transaction Commit) ================");
        };
    }
}
