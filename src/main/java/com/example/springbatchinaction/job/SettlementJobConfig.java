package com.example.springbatchinaction.job;

import com.example.springbatchinaction.domain.Orders;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
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
@StepScope
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public JpaPagingItemReader<Orders> ordersReader(@Value("#{jobparameters['targetDate']}") String targetDate) {
        log.info("[Reader] 정산 집계 대상 날짜: {}", targetDate);

        return new JpaPagingItemReaderBuilder<Orders>()
                .name("ordersReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT o FROM ORDERS o WHERE o.orderDate = :targetDate ORDER BY o.id")
                .parameterValues(Collections.singletonMap("targetDate", LocalDate.parse(targetDate)))
                .build();
    }
}
