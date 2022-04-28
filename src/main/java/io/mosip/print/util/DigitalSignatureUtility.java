package io.mosip.print.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.print.constant.ApiName;
import io.mosip.print.constant.LoggerFileConstant;
import io.mosip.print.core.http.RequestWrapper;
import io.mosip.print.core.http.ResponseWrapper;
import io.mosip.print.dto.SignRequestDto;
import io.mosip.print.dto.SignResponseDto;
import io.mosip.print.exception.ApisResourceAccessException;
import io.mosip.print.exception.DigitalSignatureException;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.service.PrintRestClientService;

@Component
public class DigitalSignatureUtility {

	@Autowired
	private PrintRestClientService<Object> printRestService;
	
	/** The print logger. */
	Logger printLogger = PrintLogger.getLogger(DigitalSignatureUtility.class);

	@Autowired
	private Environment env;

	@Autowired
	ObjectMapper mapper;

	private static final String DIGITAL_SIGNATURE_ID = "mosip.registration.processor.digital.signature.id";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";

	public String getDigitalSignature(String data) {
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"DigitalSignatureUtility::getDigitalSignature()::entry");

		SignRequestDto dto=new SignRequestDto();
		dto.setData(data);
		RequestWrapper<SignRequestDto> request=new RequestWrapper<>();
		request.setRequest(dto);
		request.setId(env.getProperty(DIGITAL_SIGNATURE_ID));
		request.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		request.setRequesttime(localdatetime);
		request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

		try {
			ResponseWrapper<SignResponseDto> response = (ResponseWrapper) printRestService
					.postApi(ApiName.DIGITALSIGNATURE, "", "", request, ResponseWrapper.class);

			if (response.getErrors() != null && response.getErrors().size() > 0) {
				response.getErrors().stream().forEach(r -> {
					printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
							"DigitalSignatureUtility::getDigitalSignature():: error with error message " + r.getMessage());
				});
			}

			SignResponseDto signResponseDto = mapper.readValue(mapper.writeValueAsString(response.getResponse()), SignResponseDto.class);
			
			printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"DigitalSignatureUtility::getDigitalSignature()::exit");

			return signResponseDto.getSignature();
		} catch (ApisResourceAccessException | IOException e) {
			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"DigitalSignatureUtility::getDigitalSignature():: error with error message " + e.getMessage());
			throw new DigitalSignatureException(e.getMessage(), e);
		}

	}
}

