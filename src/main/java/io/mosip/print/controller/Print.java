package io.mosip.print.controller;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.model.EventModel;
import io.mosip.print.service.PrintService;

@RestController
@RequestMapping(value = "/print")
public class Print {

	/** The printservice. */
	@Autowired
	private PrintService printService;
	
	@Value("${mosip.event.topic}")
	private String topic;

	Logger printLogger = PrintLogger.getLogger(Print.class);


	/**
	 *  Gets the file.
	 *
	 * @param eventModel
	 * @return
	 * @throws Exception
	 */
	@PostMapping(path = "/callback/notifyPrint", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthenticateContentAndVerifyIntent(secret = "${mosip.event.secret}", callback = "/v1/cardprint/print/callback/notifyPrint", topic = "${mosip.event.topic}")
	public ResponseEntity<String> handleSubscribeEvent(@RequestBody EventModel eventModel) throws Exception {
		printLogger.info("Event received from WebSub"+", id: {}",eventModel.getEvent().getId());
		boolean isPrinted = printService.generateCard(eventModel);
		printLogger.info("printing status : {} for event id: {}",isPrinted,eventModel.getEvent().getId());
		return new ResponseEntity<>("request accepted.", HttpStatus.OK);
	}

}
