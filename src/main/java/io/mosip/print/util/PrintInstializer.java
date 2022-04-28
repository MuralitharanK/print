package io.mosip.print.util;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import io.mosip.print.constant.LoggerFileConstant;
import io.mosip.print.logger.PrintLogger;

@Component
public class PrintInstializer implements ApplicationListener<ApplicationReadyEvent> {
	@Value("${retry-count:3}")
	private int retryCount;

	@Value("${print-websub-resubscription-delay-secs:0}")
	private int reSubscriptionDelaySecs;

	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	/** The Constant BIOMETRICS. */
	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String PRINTINSTIALIZER = "PrintInstializer";

	private Logger logger = PrintLogger.getLogger(PrintInstializer.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (reSubscriptionDelaySecs > 0) {
			logger.info(LoggerFileConstant.SESSIONID.toString(), PRINTINSTIALIZER, ONAPPLICATIONEVENT,
				"Work around for web-sub notification issue after some time.");

		scheduleRetrySubscriptions();
		}
		else {
			logger.info(LoggerFileConstant.SESSIONID.toString(), PRINTINSTIALIZER, ONAPPLICATIONEVENT,

					"Scheduling for re-subscription is Disabled as the re-subsctription delay value is: "
							+ reSubscriptionDelaySecs);

		}
	}

	private void scheduleRetrySubscriptions() {
		logger.info(LoggerFileConstant.SESSIONID.toString(), PRINTINSTIALIZER, ONAPPLICATIONEVENT,
				"Scheduling re-subscription every " + reSubscriptionDelaySecs + " seconds");


		taskScheduler.scheduleAtFixedRate(this::retrySubscriptions, Instant.now().plusSeconds(reSubscriptionDelaySecs),
				Duration.ofSeconds(reSubscriptionDelaySecs));
	}

	private void retrySubscriptions() {
		// Call Init Subscriptions for the count until no error in the subscription
		for (int i = 0; i <= retryCount; i++) {
			if (initSubsriptions()) {
				return;
			}
		}
	}

	private boolean initSubsriptions() {
		try {
			logger.info(LoggerFileConstant.SESSIONID.toString(), PRINTINSTIALIZER, ONAPPLICATIONEVENT,
					"Initializing subscribptions..");
			webSubSubscriptionHelper.initSubsriptions();

			return true;
		} catch (Exception e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), PRINTINSTIALIZER, ONAPPLICATIONEVENT,
					"Initializing subscribptions failed: ");

			return false;
		}
	}
}
