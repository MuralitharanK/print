package io.mosip.print.config;

import io.mosip.print.service.impl.CbeffImpl;
import io.mosip.print.spi.CbeffUtil;
import io.mosip.vercred.CredentialsVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration
public class PrintConfig {

    @Bean
    @Primary
    public CbeffUtil getCbeffUtil() {
        return new CbeffImpl();
    }

    @Bean
    public CredentialsVerifier credentialsVerifier() {
        return new CredentialsVerifier();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }


}
