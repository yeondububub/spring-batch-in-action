package com.example.springbatchinaction.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobOperator jobOperator;
    private final Job settlementJob;

    // second, minute, hour, day of month, month, day of week
    @Scheduled(cron = "0 0 4 * * *")
    public void runJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", LocalDate.now().minusDays(7).toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("스케쥴러 작동! 배치를 실행합니다.");

            jobOperator.start(settlementJob, jobParameters);
        } catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException |
                 InvalidJobParametersException | JobRestartException e) {
            log.error("배치 실행 중 예외가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("알 수 없는 에러 발생");
        }
    }
}