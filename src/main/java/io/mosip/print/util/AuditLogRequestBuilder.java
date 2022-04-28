package io.mosip.print.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.print.constant.ApiName;
import io.mosip.print.constant.AuditLogConstant;
import io.mosip.print.constant.LoggerFileConstant;
import io.mosip.print.core.http.RequestWrapper;
import io.mosip.print.core.http.ResponseWrapper;
import io.mosip.print.dto.AuditRequestDto;
import io.mosip.print.dto.AuditResponseDto;
import io.mosip.print.exception.ApisResourceAccessException;
import io.mosip.print.service.PrintRestClientService;

/**
 * The Class AuditRequestBuilder.
 * 
 * @author Rishabh Keshari
 */
@Component
public class AuditLogRequestBuilder {

	/** The logger. */
	private final Logger printLogger = LoggerFactory.getLogger(AuditLogRequestBuilder.class);

	/** The registration processor rest service. */
	@Autowired
	private PrintRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;

	private static final String AUDIT_SERVICE_ID = "mosip.print.audit.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.print.application.version";
	private static final String DATETIME_PATTERN = "mosip.print.datetime.pattern";

	/**
	 * Creates the audit request builder.
	 *
	 * @param description
	 *            the description
	 * @param eventId
	 *            the event id
	 * @param eventName
	 *            the event name
	 * @param eventType
	 *            the event type
	 * @param registrationId
	 *            the registration id
	 * @return the audit response dto
	 */
	@SuppressWarnings("unchecked")
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
			String eventName, String eventType, String registrationId, ApiName apiname) {
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::entry");

		AuditRequestDto auditRequestDto = new AuditRequestDto();
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		try {
			auditRequestDto.setDescription(description);
			auditRequestDto.setActionTimeStamp(DateUtils.getUTCCurrentDateTimeString());
			auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
			auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
			auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setEventId(eventId);
			auditRequestDto.setEventName(eventName);
			auditRequestDto.setEventType(eventType);
			auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
			auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
			auditRequestDto.setId(registrationId);
			auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
			auditRequestDto.setModuleId(null);
			auditRequestDto.setModuleName(null);
			auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setSessionUserName(null);
			requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
			requestWrapper.setMetadata(null);
			requestWrapper.setRequest(auditRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
			responseWrapper = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService.postApi(apiname, "",
					"", requestWrapper, ResponseWrapper.class);
		} catch (ApisResourceAccessException arae) {

			printLogger.error(arae.getMessage());

		}
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::exit");

		return responseWrapper;
	}

	@SuppressWarnings("unchecked")
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
			String eventName, String eventType, String moduleId, String moduleName, String registrationId) {
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		AuditRequestDto auditRequestDto;
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();

		try {

			auditRequestDto = new AuditRequestDto();
			auditRequestDto.setDescription(description);
			auditRequestDto.setActionTimeStamp(DateUtils.getUTCCurrentDateTimeString());
			auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
			auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
			auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setEventId(eventId);
			auditRequestDto.setEventName(eventName);
			auditRequestDto.setEventType(eventType);
			auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
			auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
			auditRequestDto.setId(registrationId);
			auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
			auditRequestDto.setModuleId(moduleId);
			auditRequestDto.setModuleName(moduleName);
			auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setSessionUserName(null);
			requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
			requestWrapper.setMetadata(null);
			requestWrapper.setRequest(auditRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
			responseWrapper = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
					.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);

		} catch (ApisResourceAccessException arae) {

			printLogger.error(arae.getMessage());

		}
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");

		return responseWrapper;
	}

}
