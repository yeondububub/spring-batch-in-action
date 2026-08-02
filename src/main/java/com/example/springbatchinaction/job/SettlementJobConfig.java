package com.example.springbatchinaction.job;

import com.example.springbatchinaction.domain.Orders;
import com.example.springbatchinaction.domain.Settlement;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaPagingItemReader<Orders> ordersReader(@Value("#{jobParameters['targetDate']}") String targetDate) {
        log.info("[Reader] 정산 집계 대상 날짜: {}", targetDate);

        return new JpaPagingItemReaderBuilder<Orders>()
                .name("ordersReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT o FROM Orders o WHERE o.orderDate = :targetDate ORDER BY o.id")
                .parameterValues(Collections.singletonMap("targetDate", LocalDate.parse(targetDate)))
                .build();
    }

    @Bean
    public ItemProcessor<Orders, Settlement> settlementProcessor() {
        log.info("[Processor] 정산 금액 계산");

        return item -> {
            int fee = (int) (item.getAmount() * 0.03);
            int settlementAmount = item.getAmount() - fee;

            return new Settlement(item.getId(), item.getStoreName(), settlementAmount, LocalDate.now());
        };
    }

    @Bean
    public JpaItemWriter<Settlement> settlementWriter() {
        log.info("[Writer] 등록 처리");

        return new JpaItemWriterBuilder<Settlement>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    // Job 등록
    @Bean
    public Job settlementJob(Step settlementStep) {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementStep)
                .build();
    }

    // Step 등록
    @Bean
    public Step settlementStep(JpaPagingItemReader<Orders> ordersReader) {
        return new StepBuilder("settlementStep", jobRepository)
                .<Orders, Settlement>chunk(1000)
                .transactionManager(transactionManager)
                .reader(ordersReader)
                .processor(settlementProcessor())
                .writer(settlementWriter())
                .build();
    }
}
