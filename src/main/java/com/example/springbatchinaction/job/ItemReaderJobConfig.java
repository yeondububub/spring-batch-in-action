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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ItemReaderJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job jpaPagingItemReaderJob() {
        return new JobBuilder("jpaPagingItemReaderJob", jobRepository)
                .start(jpaPagingStep())
                .build();
    }

    @Bean
    public Step jpaPagingStep() {
        return new StepBuilder("jpaPagingStep", jobRepository)
                .<Orders, Settlement>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(jpaPagingItemReader(null))
                .processor(ordersToSettlementProcessor())
                .writer(settlementItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Orders> jpaPagingItemReader(
            @Value("#{jobParameters['orderDate']}") String orderDate) {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("orderDate", LocalDate.parse(orderDate));

        return new JpaPagingItemReaderBuilder<Orders>()
                .name("jpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT o FROM Orders o WHERE o.orderDate = :orderDate ORDER BY o.id")
                .parameterValues(parameters)
                .build();
    }

    @Bean
    public ItemProcessor<Orders, Settlement> ordersToSettlementProcessor() {
        return order -> {
            int fee = (int) (order.getAmount() * 0.03);
            int settlementAmount = order.getAmount() - fee;
            return new Settlement(order.getId(), order.getStoreName(), settlementAmount, LocalDate.now());
        };
    }

    @Bean
    public JpaItemWriter<Settlement> settlementItemWriter() {
        return new JpaItemWriterBuilder<Settlement>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
