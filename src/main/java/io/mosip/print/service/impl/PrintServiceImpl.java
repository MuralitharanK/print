package io.mosip.print.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.print.constant.*;
import io.mosip.print.core.http.RequestWrapper;
import io.mosip.print.dto.*;
import io.mosip.print.entity.MspCardEntity;
import io.mosip.print.exception.*;
import io.mosip.print.idrepo.dto.IdResponseDTO1;
import io.mosip.print.logger.LogDescription;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.model.CredentialStatusEvent;
import io.mosip.print.model.EventModel;
import io.mosip.print.model.StatusEvent;
import io.mosip.print.repository.MspCardRepository;
import io.mosip.print.service.PrintRestClientService;
import io.mosip.print.service.PrintService;
import io.mosip.print.service.UinCardGenerator;
import io.mosip.print.spi.CbeffUtil;
import io.mosip.print.spi.QrCodeGenerator;
import io.mosip.print.util.*;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PrintServiceImpl implements PrintService{

	private String topic="CREDENTIAL_STATUS_UPDATE";
	
	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	@Autowired
	CryptoUtil cryptoUtil;

	@Autowired
	private RestApiClient restApiClient;

	@Autowired
	private CryptoCoreUtil cryptoCoreUtil;

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/** The primary lang. */
	@Value("${mosip.primary-language}")
	private String primaryLang;

	/** The secondary lang. */
	@Value("${mosip.secondary-language}")
	private String secondaryLang;

	/** The Constant UIN_CARD_TEMPLATE. */
	private static final String UIN_CARD_TEMPLATE = "RPR_UIN_CARD_TEMPLATE";

	/** The Constant MASKED_UIN_CARD_TEMPLATE. */
	private static final String MASKED_UIN_CARD_TEMPLATE = "RPR_MASKED_UIN_CARD_TEMPLATE";

	/** The Constant FACE. */
	private static final String FACE = "Face";

	/** The Constant UIN_CARD_PDF. */
	private static final String UIN_CARD_PDF = "uinPdf";

	/** The Constant UIN_TEXT_FILE. */
	private static final String UIN_TEXT_FILE = "textFile";

	/** The Constant APPLICANT_PHOTO. */
	private static final String APPLICANT_PHOTO = "ApplicantPhoto";

	/** The Constant QRCODE. */
	private static final String QRCODE = "QrCode";

	/** The Constant UINCARDPASSWORD. */
	private static final String UINCARDPASSWORD = "mosip.registration.processor.print.service.uincard.password";

	/** The print logger. */
	Logger printLogger = PrintLogger.getLogger(PrintServiceImpl.class);

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The template generator. */
	@Autowired
	private TemplateGenerator templateGenerator;

	/** The utilities. */
	@Autowired
	private Utilities utilities;

	/** The uin card generator. */
	@Autowired
	private UinCardGenerator<byte[]> uinCardGenerator;

	/** The rest client service. */
	@Autowired
	private PrintRestClientService<Object> restClientService;


	/** The qr code generator. */
	@Autowired
	private QrCodeGenerator<QrVersion> qrCodeGenerator;

	/** The Constant INDIVIDUAL_BIOMETRICS. */
	private static final String INDIVIDUAL_BIOMETRICS = "individualBiometrics";

	/** The Constant VID_CREATE_ID. */
	public static final String VID_CREATE_ID = "registration.processor.id.repo.generate";

	/** The Constant REG_PROC_APPLICATION_VERSION. */
	public static final String REG_PROC_APPLICATION_VERSION = "registration.processor.id.repo.vidVersion";

	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.print.datetime.pattern";

	private static final String NAME = "name";

	public static final String VID_TYPE = "registration.processor.id.repo.vidType";

	/** The cbeffutil. */
	@Autowired
	private CbeffUtil cbeffutil;

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;
	
	@Autowired
	private PublisherClient<String, Object, HttpHeaders> pb;
	
	@Value("${mosip.datashare.partner.id}")
	private String partnerId;

	@Value("${mosip.datashare.policy.id}")
	private String policyId;

	@Autowired
	@Qualifier("mspCardRepository")
	MspCardRepository mspCardRepository;

	@Autowired
	ObjectMapper mapper;

	public void generateCard(EventModel eventModel) throws Exception {
		System.out.println("Received Request");
		Map<String, byte[]> byteMap = new HashMap<>();
		String decodedCrdential = null;
		String credential = null;
		if (eventModel.getEvent().getDataShareUri() == null || eventModel.getEvent().getDataShareUri().isEmpty()) {
			credential = eventModel.getEvent().getData().get("credential").toString();
		} else {
			String dataShareUrl = eventModel.getEvent().getDataShareUri();
			URI dataShareUri = URI.create(dataShareUrl);
			credential = restApiClient.getApi(dataShareUri, String.class);
		}

		String ecryptionPin = eventModel.getEvent().getData().get("protectionKey").toString();
		decodedCrdential = cryptoCoreUtil.decrypt(credential);
		System.out.println("Decryption Completed");

		Map proofMap = new HashMap<String, String>();
		proofMap = (Map) eventModel.getEvent().getData().get("proof");
		String sign = proofMap.get("signature").toString();
		Map<String, Object> attributes = getDocuments(decodedCrdential,
				eventModel.getEvent().getData().get("credentialType").toString(), ecryptionPin,
				eventModel.getEvent().getTransactionId(), getSignature(sign, credential), "UIN", false, eventModel.getEvent().getId(),
				eventModel.getEvent().getData().get("registrationId").toString(), eventModel.getEvent().getData().get("vid").toString());

		System.out.println("Attributes Populated");

		String printid = (String) eventModel.getEvent().getId();

		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		obj.put("photo",attributes.get(APPLICANT_PHOTO));
		obj.put("qrCode",attributes.get(QRCODE));
		obj.put("address", ((attributes.get("address") != null && !attributes.get("address").equals("")) ? attributes.get("address").toString() : "N/A"));
		obj.put("locality", ((attributes.get("locality") != null && !attributes.get("locality").equals("")) ? attributes.get("locality").toString() : "N/A"));
		obj.put("city", ((attributes.get("city") != null && !attributes.get("city").equals("")) ? attributes.get("city").toString() : "N/A"));
		obj.put("state", ((attributes.get("state") != null && !attributes.get("state").equals("")) ? attributes.get("state").toString() : "N/A"));
		obj.put("postalCode", ((attributes.get("postalCode") != null && !attributes.get("postalCode").equals("")) ? attributes.get("postalCode").toString() : "N/A"));
		obj.put("gender", ((attributes.get("gender") != null && !attributes.get("gender").equals("")) ? attributes.get("gender").toString() : "N/A"));
		obj.put("fullName", ((attributes.get("fullName") != null && !attributes.get("fullName").equals("")) ? attributes.get("fullName").toString() : "N/A"));
		obj.put("dateOfBirth", ((attributes.get("dateOfBirth") != null && !attributes.get("dateOfBirth").equals("")) ? attributes.get("dateOfBirth").toString() : "N/A"));
		obj.put("phone", ((attributes.get("phone") != null && !attributes.get("phone").equals("")) ? attributes.get("phone").toString() : "N/A"));
		obj.put("vid", ((attributes.get("VID") != null && !attributes.get("VID").equals("")) ? attributes.get("VID").toString() : "N/A"));
		obj.put("UIN", ((attributes.get("UIN") != null && !attributes.get("UIN").equals("")) ? attributes.get("UIN").toString() : "N/A"));
		obj.put("email", ((attributes.get("email") != null && !attributes.get("email").equals("")) ? attributes.get("email").toString() : "N/A"));

		String woenc = obj.toJSONString();
		System.out.println("Json Populated");

		MspCardEntity mspCardEntity = new MspCardEntity();
		mspCardEntity.setJsonData(woenc);
		mspCardEntity.setRequestId(printid);
		mspCardEntity.setStatus(90);
		UUID uuid=UUID.randomUUID();
		mspCardEntity.setId(uuid.toString());
		mspCardRepository.create(mspCardEntity);
		System.out.println("Job Completed");

	}

	private String getSignature(String sign, String crdential) {
		String signHeader = sign.split("\\.")[0];
		String signData = sign.split("\\.")[2];
		String signature = signHeader + "." + crdential + "." + signData;
		return signature;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.print.service.PrintService#
	 * getDocuments(io.mosip.registration.processor.core.constant.IdType,
	 * java.lang.String, java.lang.String, boolean)
	 */

	@SuppressWarnings("rawtypes")
	private Map<String, Object> getDocuments(String credential, String credentialType, String encryptionPin,
			String requestId, String sign,
			String cardType,
			boolean isPasswordProtected, String refId, String registrationId, String vid) {
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"PrintServiceImpl::getDocuments()::entry");


		String credentialSubject;
		Map<String, byte[]> byteMap = new HashMap<>();
		String uin = null;
		LogDescription description = new LogDescription();
		String password = null;
		String individualBio = null;
		Map<String, Object> attributes = new LinkedHashMap<>();
		boolean isTransactionSuccessful = false;
		IdResponseDTO1 response = null;
		String template = UIN_CARD_TEMPLATE;
		byte[] pdfbytes = null;
		try {

			credentialSubject = getCrdentialSubject(credential);
			org.json.JSONObject credentialSubjectJson = new org.json.JSONObject(credentialSubject);
			org.json.JSONObject decryptedJson = decryptAttribute(credentialSubjectJson, encryptionPin, credential);
			individualBio = decryptedJson.getString("biometrics");
			String individualBiometric = new String(individualBio);
			uin = decryptedJson.getString("UIN");
			if (isPasswordProtected) {
				password = getPassword(uin);
			}

			boolean isPhotoSet = setApplicantPhoto(individualBiometric, attributes);
			if (!isPhotoSet) {
				printLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), uin +
						PlatformErrorMessages.PRT_PRT_APPLICANT_PHOTO_NOT_SET.name());
			}
			setTemplateAttributes(decryptedJson.toString(), attributes);
			attributes.put(IdType.UIN.toString(), uin);
			attributes.put(IdType.VID.toString(), vid);

			byte[] textFileByte = createTextFile(decryptedJson.toString());
			byteMap.put(UIN_TEXT_FILE, textFileByte);

			boolean isQRcodeSet = setQrCode(decryptedJson.toString(), attributes);
			if (!isQRcodeSet) {
				printLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), uin +
						PlatformErrorMessages.PRT_PRT_QRCODE_NOT_SET.name());
			}


			printStatusUpdate(requestId, credentialType, uin, refId, registrationId);
			isTransactionSuccessful = true;

		} catch (VidCreationException e) {
			e.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_PRT_VID_CREATION_ERROR.getMessage());
			description.setCode(PlatformErrorMessages.PRT_PRT_VID_CREATION_ERROR.getCode());
			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN", PlatformErrorMessages.PRT_PRT_QRCODE_NOT_GENERATED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(e.getErrorCode(), e.getErrorText());

		}

		catch (QrcodeGenerationException e) {
			e.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_PRT_QR_CODE_GENERATION_ERROR.getMessage());
			description.setCode(PlatformErrorMessages.PRT_PRT_QR_CODE_GENERATION_ERROR.getCode());
			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN",
					PlatformErrorMessages.PRT_PRT_QRCODE_NOT_GENERATED.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getErrorText());

		} catch (UINNotFoundInDatabase e) {
			e.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_PRT_UIN_NOT_FOUND_IN_DATABASE.getMessage());
			description.setCode(PlatformErrorMessages.PRT_PRT_UIN_NOT_FOUND_IN_DATABASE.getCode());

			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN".toString(),
					PlatformErrorMessages.PRT_PRT_UIN_NOT_FOUND_IN_DATABASE.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getErrorText());

		} catch (TemplateProcessingFailureException e) {
			e.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_TEM_PROCESSING_FAILURE.getMessage());
			description.setCode(PlatformErrorMessages.PRT_TEM_PROCESSING_FAILURE.getCode());

			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN",
					PlatformErrorMessages.PRT_TEM_PROCESSING_FAILURE.name() + ExceptionUtils.getStackTrace(e));
			throw new TemplateProcessingFailureException(PlatformErrorMessages.PRT_TEM_PROCESSING_FAILURE.getMessage());

		} catch (PDFGeneratorException e) {
			e.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_PRT_PDF_NOT_GENERATED.getMessage());
			description.setCode(PlatformErrorMessages.PRT_PRT_PDF_NOT_GENERATED.getCode());

			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN",
					PlatformErrorMessages.PRT_PRT_PDF_NOT_GENERATED.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getErrorText());

		} catch (PDFSignatureException e) {
			e.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.getCode());

			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN".toString(),
					PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFSignatureException(PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.getMessage());

		} catch (Exception ex) {
			ex.printStackTrace();
			description.setMessage(PlatformErrorMessages.PRT_PRT_PDF_GENERATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.PRT_PRT_PDF_GENERATION_FAILED.getCode());
			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"UIN", description + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					ex.getMessage() + ExceptionUtils.getStackTrace(ex));

		} finally {
			String eventId = "";
			String eventName = "";
			String eventType = "";
			if (isTransactionSuccessful) {
				description.setMessage(PlatformSuccessMessages.RPR_PRINT_SERVICE_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_PRINT_SERVICE_SUCCESS.getCode());

				eventId = EventId.RPR_402.toString();
				eventName = EventName.UPDATE.toString();
				eventType = EventType.BUSINESS.toString();
			} else {
				description.setMessage(PlatformErrorMessages.PRT_PRT_PDF_GENERATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.PRT_PRT_PDF_GENERATION_FAILED.getCode());

				eventId = EventId.RPR_405.toString();
				eventName = EventName.EXCEPTION.toString();
				eventType = EventType.SYSTEM.toString();
			}
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_PRINT_SERVICE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.PRINT_SERVICE.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, uin);
		}
		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getDocuments()::exit");

		return attributes;
	}

	/**
	 * Gets the id repo response.
	 *
	 * @param idType
	 *            the id type
	 * @param idValue
	 *            the id value
	 * @return the id repo response
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	/*
	 * private IdResponseDTO1 getIdRepoResponse(String idType, String idValue)
	 * throws ApisResourceAccessException { List<String> pathsegments = new
	 * ArrayList<>(); pathsegments.add(idValue);
	 * 
	 * IdResponseDTO1 response; if (idType.equalsIgnoreCase(IdType.UIN.toString()))
	 * { response = (IdResponseDTO1)
	 * restClientService.getApi(ApiName.IDREPOGETIDBYUIN, pathsegments, "", null,
	 * IdResponseDTO1.class); } else { response = (IdResponseDTO1)
	 * restClientService.getApi(ApiName.RETRIEVEIDENTITYFROMRID, pathsegments, "",
	 * null, IdResponseDTO1.class); }
	 * 
	 * if (response == null || response.getResponse() == null) {
	 * printLogger.error(LoggerFileConstant.SESSIONID.toString(),
	 * LoggerFileConstant.REGISTRATIONID.toString(), idValue,
	 * PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.name()); throw new
	 * IDRepoResponseNull(PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.getCode
	 * ()); }
	 * 
	 * return response; }
	 */
	/**
	 * Creates the text file.
	 *
	 * @param jsonString
	 *            the attributes
	 * @return the byte[]
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private byte[] createTextFile(String jsonString) throws IOException {

		LinkedHashMap<String, String> printTextFileMap = new LinkedHashMap<>();
		JSONObject demographicIdentity = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
		if (demographicIdentity == null)
			throw new IdentityNotFoundException(PlatformErrorMessages.PRT_PIS_IDENTITY_NOT_FOUND.getMessage());
		String printTextFileJson = Utilities.getJson1(utilities.getConfigServerFileStorageURL(),
				utilities.getRegistrationProcessorPrintTextFile());

		JSONObject printTextFileJsonObject = JsonUtil.objectMapperReadValue(printTextFileJson, JSONObject.class);
		Set<String> printTextFileJsonKeys = printTextFileJsonObject.keySet();
		for (String key : printTextFileJsonKeys) {
			String printTextFileJsonString = JsonUtil.getJSONValue(printTextFileJsonObject, key);
			for (String value : printTextFileJsonString.split(",")) {
				Object object = demographicIdentity.get(value);
				if (object instanceof ArrayList) {
					JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
					JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
					for (JsonValue jsonValue : jsonValues) {
						if (jsonValue.getLanguage().equals(primaryLang))
							printTextFileMap.put(value + "_" + primaryLang, jsonValue.getValue());
						if (jsonValue.getLanguage().equals(secondaryLang))
							printTextFileMap.put(value + "_" + secondaryLang, jsonValue.getValue());

					}

				} else if (object instanceof LinkedHashMap) {
					JSONObject json = JsonUtil.getJSONObject(demographicIdentity, value);
					printTextFileMap.put(value, (String) json.get(VALUE));
				} else {
					printTextFileMap.put(value, (String) object);

				}
			}

		}

		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		String printTextFileString = gson.toJson(printTextFileMap);
		return printTextFileString.getBytes();
	}

	/**
	 * Sets the qr code.
	 *
	 * @param qrString the text file byte
	 * @param attributes   the attributes
	 * @return true, if successful
	 * @throws QrcodeGenerationException                          the qrcode
	 *                                                            generation
	 *                                                            exception
	 * @throws IOException                                        Signals that an
	 *                                                            I/O exception has
	 *                                                            occurred.
	 * @throws QrcodeGenerationException
	 */
	private boolean setQrCode(String qrString, Map<String, Object> attributes)
			throws QrcodeGenerationException, IOException, QrcodeGenerationException {
		boolean isQRCodeSet = false;
		JSONObject qrJsonObj = JsonUtil.objectMapperReadValue(qrString, JSONObject.class);
		qrJsonObj.remove("biometrics");
		// String digitalSignaturedQrData =
		// digitalSignatureUtility.getDigitalSignature(qrString);
		// JSONObject textFileJson = new JSONObject();
		// textFileJson.put("digitalSignature", digitalSignaturedQrData);
		// Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		// String printTextFileString = gson.toJson(textFileJson);
		byte[] qrCodeBytes = qrCodeGenerator.generateQrCode(qrJsonObj.toString(), QrVersion.V30);
		if (qrCodeBytes != null) {
			String imageString = Base64.encodeBase64String(qrCodeBytes);
			attributes.put(QRCODE, "data:image/png;base64," + imageString);
			isQRCodeSet = true;
		}

		return isQRCodeSet;
	}

	private byte[] generateQrCode(String qrString)
			throws QrcodeGenerationException, IOException, QrcodeGenerationException {
		JSONObject qrJsonObj = JsonUtil.objectMapperReadValue(qrString, JSONObject.class);
		qrJsonObj.remove("biometrics");
		byte[] qrCodeBytes = qrCodeGenerator.generateQrCode(qrJsonObj.toString(), QrVersion.V30);

		return qrCodeBytes;
	}

	/**
	 * Sets the applicant photo.
	 *
	 * @param individualBio
	 *            the response
	 * @param attributes
	 *            the attributes
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	private boolean setApplicantPhoto(String individualBio, Map<String, Object> attributes) throws Exception {
		String value = individualBio;
		boolean isPhotoSet = false;

		if (value != null) {
			CbeffToBiometricUtil util = new CbeffToBiometricUtil(cbeffutil);
			List<String> subtype = new ArrayList<>();
			byte[] photoByte = util.getImageBytes(value, FACE, subtype);
			if (photoByte != null) {
				/*
				 * DataInputStream dis = new DataInputStream(new
				 * ByteArrayInputStream(photoByte)); int skippedBytes =
				 * dis.skipBytes(headerLength);
				 * 
				 * if (skippedBytes != 0) {
				 * printLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				 * LoggerFileConstant.REGISTRATIONID.toString(), "",
				 * "bytes skipped for image "); } String data =
				 * DatatypeConverter.printBase64Binary(IOUtils.toByteArray(dis));
				 */

				String data = java.util.Base64.getEncoder().encodeToString(extractFaceImageData(photoByte));
				attributes.put(APPLICANT_PHOTO, "data:image/png;base64," + data);
				isPhotoSet = true;
			}
		}
		return isPhotoSet;
	}

	/**
	 * Gets the artifacts.
	 *
	 * @param jsonString the id json string
	 * @param attribute    the attribute
	 * @return the artifacts
	 * @throws IOException    Signals that an I/O exception has occurred.
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	private void setTemplateAttributes(String jsonString, Map<String, Object> attribute)
			throws IOException, ParseException {
		try {
			JSONObject demographicIdentity = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
			if (demographicIdentity == null)
				throw new IdentityNotFoundException(PlatformErrorMessages.PRT_PIS_IDENTITY_NOT_FOUND.getMessage());

			String mapperJsonString = Utilities.getJson1(utilities.getConfigServerFileStorageURL(),
					utilities.getGetRegProcessorIdentityJson());
			JSONObject mapperJson = JsonUtil.objectMapperReadValue(mapperJsonString, JSONObject.class);
			JSONObject mapperIdentity = JsonUtil.getJSONObject(mapperJson,
					utilities.getGetRegProcessorDemographicIdentity());

			List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
			for (String key : mapperJsonKeys) {
				LinkedHashMap<String, String> jsonObject = JsonUtil.getJSONValue(mapperIdentity, key);
				Object obj = null;
				String values = jsonObject.get(VALUE);
				for (String value : values.split(",")) {
					// Object object = demographicIdentity.get(value);
					Object object = demographicIdentity.get(value);
					if (object != null) {
						try {
						obj = new JSONParser().parse(object.toString());
						} catch (Exception e) {
							obj = object;
						}
					
					if (obj instanceof JSONArray) {
						// JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
						JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, (JSONArray) obj);
						for (JsonValue jsonValue : jsonValues) {
							if (jsonValue.getLanguage().equals(primaryLang))
								attribute.put(value + "_" + primaryLang, jsonValue.getValue());
							if (jsonValue.getLanguage().equals(secondaryLang))
								attribute.put(value + "_" + secondaryLang, jsonValue.getValue());

						}

					} else if (object instanceof JSONObject) {
						JSONObject json = (JSONObject) object;
						attribute.put(value, (String) json.get(VALUE));
					} else {
						attribute.put(value, String.valueOf(object));
					}
				}
					
				}
			}

		} catch (JsonParseException | JsonMappingException e) {
			printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					null, "Error while parsing Json file" + ExceptionUtils.getStackTrace(e));
			throw new ParsingException(PlatformErrorMessages.PRT_RGS_JSON_PARSING_EXCEPTION.getMessage(), e);
		}
	}

	/**
	 * Mask string.
	 *
	 * @param uin
	 *            the uin
	 * @param maskLength
	 *            the mask length
	 * @param maskChar
	 *            the mask char
	 * @return the string
	 */
	private String maskString(String uin, int maskLength, char maskChar) {
		if (uin == null || "".equals(uin))
			return "";

		if (maskLength == 0)
			return uin;

		StringBuilder sbMaskString = new StringBuilder(maskLength);

		for (int i = 0; i < maskLength; i++) {
			sbMaskString.append(maskChar);
		}

		return sbMaskString.toString() + uin.substring(0 + maskLength);
	}

	/**
	 * Gets the vid.
	 *
	 * @param uin the uin
	 * @return the vid
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws VidCreationException        the vid creation exception
	 * @throws IOException                 Signals that an I/O exception has
	 *                                     occurred.
	 */
	private String getVid(String uin) throws ApisResourceAccessException, VidCreationException, IOException {
		String vid;
		VidRequestDto vidRequestDto = new VidRequestDto();
		RequestWrapper<VidRequestDto> request = new RequestWrapper<>();
		VidResponseDTO vidResponse;
		vidRequestDto.setUIN(uin);
		vidRequestDto.setVidType(env.getProperty(VID_TYPE));
		request.setId(env.getProperty(VID_CREATE_ID));
		request.setRequest(vidRequestDto);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		request.setRequesttime(localdatetime);
		request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getVid():: post CREATEVID service call started with request data : "
						+ JsonUtil.objectMapperObjectToJson(vidRequestDto));

		vidResponse = (VidResponseDTO) restClientService.postApi(ApiName.CREATEVID, "", "", request,
				VidResponseDTO.class);

		printLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getVid():: post CREATEVID service call ended successfully");

		if (vidResponse.getErrors() != null && !vidResponse.getErrors().isEmpty()) {
			throw new VidCreationException(PlatformErrorMessages.PRT_PRT_VID_EXCEPTION.getCode(),
					PlatformErrorMessages.PRT_PRT_VID_EXCEPTION.getMessage());

		} else {
			vid = vidResponse.getResponse().getVid();
		}

		return vid;
	}

	/**
	 * Gets the password.
	 *
	 * @param uin
	 *            the uin
	 * @return the password
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private String getPassword(String uin) throws ApisResourceAccessException, IOException {
		JSONObject jsonObject = utilities.retrieveIdrepoJson(uin);

		String[] attributes = env.getProperty(UINCARDPASSWORD).split("\\|");
		List<String> list = new ArrayList<>(Arrays.asList(attributes));

		Iterator<String> it = list.iterator();
		String uinCardPd = "";

		while (it.hasNext()) {
			String key = it.next().trim();

			Object object = JsonUtil.getJSONValue(jsonObject, key);
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(jsonObject, key);
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				uinCardPd = uinCardPd.concat(getParameter(jsonValues, primaryLang));

			} else if (object instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(jsonObject, key);
				uinCardPd = uinCardPd.concat((String) json.get(VALUE));
			} else {
				uinCardPd = uinCardPd.concat((String) object);
			}

		}

		return uinCardPd;
	}

	/**
	 * Gets the parameter.
	 *
	 * @param jsonValues
	 *            the json values
	 * @param langCode
	 *            the lang code
	 * @return the parameter
	 */
	private String getParameter(JsonValue[] jsonValues, String langCode) {

		String parameter = null;
		if (jsonValues != null) {
			for (int count = 0; count < jsonValues.length; count++) {
				String lang = jsonValues[count].getLanguage();
				if (langCode.contains(lang)) {
					parameter = jsonValues[count].getValue();
					break;
				}
			}
		}
		return parameter;
	}

	public byte[] extractFaceImageData(byte[] decodedBioValue) {

		try (DataInputStream din = new DataInputStream(new ByteArrayInputStream(decodedBioValue))) {

			byte[] format = new byte[4];
			din.read(format, 0, 4);
			byte[] version = new byte[4];
			din.read(version, 0, 4);
			int recordLength = din.readInt();
			short numberofRepresentionRecord = din.readShort();
			byte certificationFlag = din.readByte();
			byte[] temporalSequence = new byte[2];
			din.read(temporalSequence, 0, 2);
			int representationLength = din.readInt();
			byte[] representationData = new byte[representationLength - 4];
			din.read(representationData, 0, representationData.length);
			try (DataInputStream rdin = new DataInputStream(new ByteArrayInputStream(representationData))) {
				byte[] captureDetails = new byte[14];
				rdin.read(captureDetails, 0, 14);
				byte noOfQualityBlocks = rdin.readByte();
				if (noOfQualityBlocks > 0) {
					byte[] qualityBlocks = new byte[noOfQualityBlocks * 5];
					rdin.read(qualityBlocks, 0, qualityBlocks.length);
				}
				short noOfLandmarkPoints = rdin.readShort();
				byte[] facialInformation = new byte[15];
				rdin.read(facialInformation, 0, 15);
				if (noOfLandmarkPoints > 0) {
					byte[] landmarkPoints = new byte[noOfLandmarkPoints * 8];
					rdin.read(landmarkPoints, 0, landmarkPoints.length);
				}
				byte faceType = rdin.readByte();
				byte imageDataType = rdin.readByte();
				byte[] otherImageInformation = new byte[9];
				rdin.read(otherImageInformation, 0, otherImageInformation.length);
				int lengthOfImageData = rdin.readInt();

				byte[] image = new byte[lengthOfImageData];
				rdin.read(image, 0, lengthOfImageData);

				return image;
			}
		} catch (Exception ex) {
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					ex.getMessage() + ExceptionUtils.getStackTrace(ex));
		}
	}


	private String getCrdentialSubject(String crdential) {
		org.json.JSONObject jsonObject = new org.json.JSONObject(crdential);
		String credentialSubject = jsonObject.get("credentialSubject").toString();
		return credentialSubject;
	}

	private void printStatusUpdate(String requestId, String credentialType, String uin, String printRefId, String registrationId)
			throws DataShareException, ApiNotAccessibleException, IOException, Exception {
		CredentialStatusEvent creEvent = new CredentialStatusEvent();
		LocalDateTime currentDtime = DateUtils.getUTCCurrentDateTime();
		StatusEvent sEvent = new StatusEvent();
		sEvent.setId(UUID.randomUUID().toString());
		sEvent.setRequestId(requestId);
		sEvent.setStatus("printing");
		sEvent.setUrl("");
		sEvent.setTimestamp(Timestamp.valueOf(currentDtime).toString());
		creEvent.setPublishedOn(new DateTime().toString());
		creEvent.setPublisher("PRINT_SERVICE");
		creEvent.setTopic(topic);
		creEvent.setEvent(sEvent);
		webSubSubscriptionHelper.printStatusUpdateEvent(topic, creEvent);
	}

	public org.json.JSONObject decryptAttribute(org.json.JSONObject data, String encryptionPin, String credential) {

		org.json.JSONObject jsonObj = new org.json.JSONObject(credential);

		String strq = null;
		org.json.JSONArray jsonArray = (org.json.JSONArray) jsonObj.get("protectedAttributes");
		if (!jsonArray.isEmpty()) {
		for (Object str : jsonArray) {

				CryptoWithPinRequestDto cryptoWithPinRequestDto = new CryptoWithPinRequestDto();
				CryptoWithPinResponseDto cryptoWithPinResponseDto = new CryptoWithPinResponseDto();

				cryptoWithPinRequestDto.setUserPin(encryptionPin);
				cryptoWithPinRequestDto.setData(data.getString(str.toString()));
				/*
				 * response = (ResponseWrapper)
				 * restClientService.postApi(ApiName.DECRYPTPINBASSED, "", "", request,
				 * ResponseWrapper.class);
				 * 
				 * decryptResponseDto =
				 * JsonUtil.readValue(JsonUtil.writeValueAsString(response.getResponse()),
				 * DecryptResponseDto.class);
				 */
				try {
					cryptoWithPinResponseDto = cryptoUtil.decryptWithPin(cryptoWithPinRequestDto);
				} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
					printLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
							null, "Error while decrypting the data" + ExceptionUtils.getStackTrace(e));
					throw new CryptoManagerException(PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getCode(),
							PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getMessage(), e);
				}
				data.put((String) str, cryptoWithPinResponseDto.getData());
			
			}

		}
		return data;
	}
}
	
	
	
	


	

