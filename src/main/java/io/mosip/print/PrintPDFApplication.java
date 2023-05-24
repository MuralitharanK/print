package io.mosip.print;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = { "io.mosip.print.*", "${mosip.auth.adapter.impl.basepackage}"  }, exclude = { SecurityAutoConfiguration.class, DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		CacheAutoConfiguration.class })
//
//@SpringBootApplication(scanBasePackages = { "io.mosip.print.*", "${mosip.auth.adapter.impl.basepackage}"  }, exclude = { DataSourceAutoConfiguration.class,
//		HibernateJpaAutoConfiguration.class,
//		CacheAutoConfiguration.class })
@EnableScheduling
@EnableAsync
public class PrintPDFApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrintPDFApplication.class, args);
	}

}
