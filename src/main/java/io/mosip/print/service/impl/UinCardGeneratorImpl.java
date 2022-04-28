package io.mosip.print.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.print.constant.ApiName;
import io.mosip.print.constant.LoggerFileConstant;
import io.mosip.print.constant.PDFGeneratorExceptionCodeConstant;
import io.mosip.print.constant.UinCardType;
import io.mosip.print.core.http.RequestWrapper;
import io.mosip.print.core.http.ResponseWrapper;
import io.mosip.print.dto.ErrorDTO;
import io.mosip.print.dto.PDFSignatureRequestDto;
import io.mosip.print.dto.SignatureResponseDto;
import io.mosip.print.exception.ApisResourceAccessException;
import io.mosip.print.exception.PDFGeneratorException;
import io.mosip.print.exception.PDFSignatureException;
import io.mosip.print.exception.PlatformErrorMessages;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.service.PrintRestClientService;
import io.mosip.print.service.UinCardGenerator;
import io.mosip.print.spi.PDFGenerator;
import io.mosip.print.util.DateUtils;
import io.mosip.print.util.RestApiClient;

/**
 * The Class UinCardGeneratorImpl.
 * 
 * @author M1048358 Alok
 */
@Component
public class UinCardGeneratorImpl implements UinCardGenerator<byte[]> {

	/** The pdf generator. */
	@Autowired
	private PDFGenerator pdfGenerator;

	/** The print logger. */
	private Logger printLogger = PrintLogger.getLogger(UinCardGeneratorImpl.class);

	private static final String DATETIME_PATTERN = "mosip.print.datetime.pattern";


	@Value("${mosip.print.service.uincard.lowerleftx}")
	private int lowerLeftX;

	@Value("${mosip.print.service.uincard.lowerlefty}")
	private int lowerLeftY;

	@Value("${mosip.print.service.uincard.upperrightx}")
	private int upperRightX;

	@Value("${mosip.print.service.uincard.upperrighty}")
	private int upperRightY;

	@Value("${mosip.print.service.uincard.signature.reason}")
	private String reason;


	@Autowired
	private PrintRestClientService<Object> restClientService;

	@Autowired
	private Environment env;


	ObjectMapper mapper = new ObjectMapper();
	
	
	@Autowired
	private RestApiClient restApiClient;


	@Override
	public byte[] generateUinCard(InputStream in, UinCardType type, String password)
			throws ApisResourceAccessException {
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"UinCardGeneratorImpl::generateUinCard()::entry");
        byte[] pdfSignatured=null;
		ByteArrayOutputStream out = null;
		try {
			out = (ByteArrayOutputStream) pdfGenerator.generate(in);
			PDFSignatureRequestDto request = new PDFSignatureRequestDto(lowerLeftX, lowerLeftY, upperRightX,
					upperRightY, reason, 1, password);
			request.setApplicationId("KERNEL");
		  	request.setReferenceId("SIGN");
			request.setData(Base64.encodeBase64String(out.toByteArray()));
		  	DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

		  	request.setTimeStamp(DateUtils.getUTCCurrentDateTimeString());
			RequestWrapper<PDFSignatureRequestDto> requestWrapper = new RequestWrapper<>();

			requestWrapper.setRequest(request);
			requestWrapper.setRequesttime(localdatetime);
			ResponseWrapper<?> responseWrapper;
			SignatureResponseDto signatureResponseDto;

			 responseWrapper= (ResponseWrapper<?>)restClientService.postApi(ApiName.PDFSIGN, null, null,
					requestWrapper, ResponseWrapper.class,MediaType.APPLICATION_JSON);


			if (responseWrapper.getErrors() != null && !responseWrapper.getErrors().isEmpty()) {
				ErrorDTO error = responseWrapper.getErrors().get(0);
			    throw new PDFSignatureException(error.getMessage());
			}
			signatureResponseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
					SignatureResponseDto.class);

			pdfSignatured = Base64.decodeBase64(signatureResponseDto.getData());

		} catch (IOException | PDFGeneratorException e) {
			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", PlatformErrorMessages.PRT_PRT_PDF_NOT_GENERATED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getMessage() + ExceptionUtils.getStackTrace(e));
		} 
			  catch (ApisResourceAccessException e) {
					e.printStackTrace();
					printLogger.error(LoggerFileConstant.SESSIONID.toString(),
			 LoggerFileConstant.REGISTRATIONID.toString(), "",
						PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.name() + e.getMessage()
			 + ExceptionUtils.getStackTrace(e)); throw new PDFSignatureException(
			 e.getMessage() + ExceptionUtils.getStackTrace(e)); }
			 
				printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"UinCardGeneratorImpl::generateUinCard()::exit");

		return pdfSignatured;
	}

}
