package study.microservices.core.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("study")
public class ReviewServiceApplication {
	private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

	private final Integer threadPoolSize;

	private final Integer taskQueueSize;

	@Autowired
	public ReviewServiceApplication(
			@Value("${app.threadPoolSize:10}") Integer threadPoolSize,
			@Value("${app.taskQueueSize:100}") Integer taskQueueSize
	) {
		this.threadPoolSize = threadPoolSize;
		this.taskQueueSize = taskQueueSize;
	}

	@Bean
	public Scheduler jdbcScheduler() {
		LOG.info("Create a jdbcScheduler with threadPoolSize = {} and taskQueueSize = {}", threadPoolSize, taskQueueSize);
		return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
	}

	public static void main(String[] args) {
		// Start up the microservice and retrieve the context
		ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);

		// Print out the database connection of the microservice
		String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
		LOG.info("Connected to MySQL: " + mysqlUri);
	}

}
