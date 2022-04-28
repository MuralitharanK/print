package io.mosip.print.controller;

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
import io.mosip.print.exception.RegPrintAppException;
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


	/**
	 * Gets the file.
	 *
	 * @param printRequest the print request DTO
	 * @param token        the token
	 * @param errors       the errors
	 * @param printRequest the print request DTO
	 * @return the file
	 * @throws Exception
	 * @throws RegPrintAppException the reg print app exception
	 */
	@PostMapping(path = "/callback/notifyPrint", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthenticateContentAndVerifyIntent(secret = "${mosip.event.secret}", callback = "/v1/cardprint/print/callback/notifyPrint", topic = "${mosip.event.topic}")
	public ResponseEntity<String> handleSubscribeEvent(@RequestBody EventModel eventModel) throws Exception {
		printService.generateCard(eventModel);
		return new ResponseEntity<>("successfully printed", HttpStatus.OK);
	}

}
