package br.com.bussola.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
