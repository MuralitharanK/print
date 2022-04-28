package io.mosip.print.init;

import java.util.Date;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import io.mosip.print.constant.LoggerFileConstant;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.util.WebSubSubscriptionHelper;

@Component
public class SetupPrint 
implements ApplicationListener<ApplicationReadyEvent> {

	private Logger logger = PrintLogger.getLogger(SetupPrint.class);

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;
  
	@Value("${mosip.event.delay}")
	private int taskSubsctiptionDelay;

	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;
  
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		logger.info(LoggerFileConstant.SESSIONID.toString(), "onApplicationEvent", this.getClass().getSimpleName(),
				"Scheduling event subscriptions after (milliseconds): " + taskSubsctiptionDelay);
		taskScheduler.schedule(this::initSubsriptions, new Date(System.currentTimeMillis() + taskSubsctiptionDelay));
	}

	private void initSubsriptions() {
		logger.info(LoggerFileConstant.SESSIONID.toString(), "initSubsriptions", this.getClass().getSimpleName(),
				"Initializing subscribptions..");
		webSubSubscriptionHelper.initSubsriptions();
	}

}