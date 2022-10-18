package io.mosip.print.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mosip.print.constant.*;
import io.mosip.print.dto.CryptoWithPinRequestDto;
import io.mosip.print.dto.CryptoWithPinResponseDto;
import io.mosip.print.dto.DataShare;
import io.mosip.print.dto.JsonValue;
import io.mosip.print.entity.MspCardEntity;
import io.mosip.print.exception.*;
import io.mosip.print.idrepo.dto.IdResponseDTO1;
import io.mosip.print.logger.LogDescription;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.model.CredentialStatusEvent;
import io.mosip.print.model.EventModel;
import io.mosip.print.model.StatusEvent;
import io.mosip.print.repository.MspCardRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PrintServiceImpl implements PrintService{

	private String topic="CREDENTIAL_STATUS_UPDATE";
	
	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	@Autowired
	private DataShareUtil dataShareUtil;

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

	/** The Constant UIN_CARD_TEMPLATE. */
	private static final String UIN_CARD_TEMPLATE = "RPR_UIN_CARD_TEMPLATE";

	/** The Constant FACE. */
	private static final String FACE = "Face";

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

	/** The qr code generator. */
	@Autowired
	private QrCodeGenerator<QrVersion> qrCodeGenerator;


	/** The Constant VID_CREATE_ID. */
	public static final String VID_CREATE_ID = "registration.processor.id.repo.generate";

	/** The Constant REG_PROC_APPLICATION_VERSION. */
	public static final String REG_PROC_APPLICATION_VERSION = "registration.processor.id.repo.vidVersion";

	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.print.datetime.pattern";

	public static final String VID_TYPE = "registration.processor.id.repo.vidType";

	/** The cbeffutil. */
	@Autowired
	private CbeffUtil cbeffutil;

	/** The env. */
	@Autowired
	private Environment env;

	@Value("${mosip.datashare.partner.id}")
	private String partnerId;

	@Value("${mosip.datashare.policy.id}")
	private String policyId;

	@Value("${mosip.template-language}")
	private String templateLang;

	@Value("#{'${mosip.mandatory-languages:}'.concat('${mosip.optional-languages:}')}")
	private String supportedLang;

	@Value("${mosip.print.verify.credentials.flag:true}")
	private boolean verifyCredentialsFlag;

	@Value("${token.request.clientId}")
	private String clientId;

	@Autowired
	@Qualifier("mspCardRepository")
	MspCardRepository mspCardRepository;


	public boolean generateCard(EventModel eventModel) {
		Map<String, byte[]> byteMap = new HashMap<>();
		String decodedCrdential = null;
		String credential = null;
		boolean isPrinted =false;
		try{
		if (eventModel.getEvent().getDataShareUri() == null || eventModel.getEvent().getDataShareUri().isEmpty()) {
			credential = eventModel.getEvent().getData().get("credential").toString();
		} else {
			String dataShareUrl = eventModel.getEvent().getDataShareUri();
			URI dataShareUri = URI.create(dataShareUrl);
			credential = restApiClient.getApi(dataShareUri, String.class);
		}

		String ecryptionPin = eventModel.getEvent().getData().get("protectionKey").toString();
		decodedCrdential = cryptoCoreUtil.decrypt(credential);

		Map proofMap = new HashMap<String, String>();
		proofMap = (Map) eventModel.getEvent().getData().get("proof");
		String sign = proofMap.get("signature").toString();
		Map<String, Object> attributes = getDocuments(decodedCrdential,
				eventModel.getEvent().getData().get("credentialType").toString(), ecryptionPin,
				eventModel.getEvent().getTransactionId(), getSignature(sign, credential), "UIN", false, eventModel.getEvent().getId(),
				eventModel.getEvent().getData().get("registrationId").toString(), eventModel.getEvent().getData().get("vid").toString());


		String printid = (String) eventModel.getEvent().getId();

		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		obj.put("photo",attributes.get(APPLICANT_PHOTO));
		obj.put("qrCode",attributes.get(QRCODE));
		String fullAddress = StringUtils.arrayToDelimitedString(new Object[]{attributes.get("addressLine1"), attributes.get("addressLine2"), attributes.get("addressLine3")}, ", " );
		obj.put("address", (fullAddress != null && !fullAddress.equals("")) ? fullAddress : "N/A");
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

		MspCardEntity mspCardEntity = new MspCardEntity();
		mspCardEntity.setJsonData(woenc);
		mspCardEntity.setRequestId(printid);
		mspCardEntity.setStatus(90);
		UUID uuid=UUID.randomUUID();
		mspCardEntity.setId(uuid.toString());
		mspCardRepository.create(mspCardEntity);
		isPrinted=true;
		}catch (Exception e){
			printLogger.error(e.getMessage() , e);
			isPrinted = false;
		}
		return isPrinted;
	}
	private String getSignature(String sign, String crdential) {
		String signHeader = sign.split("\\.")[0];
		String signData = sign.split("\\.")[2];
		String signature = signHeader + "." + crdential + "." + signData;
		return signature;
	}
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
		boolean isPhotoSet=false;
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
			if(decryptedJson.has("biometrics")){
				individualBio = decryptedJson.getString("biometrics");
				String individualBiometric = new String(individualBio);
				isPhotoSet = setApplicantPhoto(individualBiometric, attributes);
				attributes.put("isPhotoSet",isPhotoSet);
			}

			uin = decryptedJson.getString("UIN");
			if (isPasswordProtected) {
				password = getPassword(uin);
			}

			if (!isPhotoSet) {
				printLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), uin +
								PlatformErrorMessages.PRT_PRT_APPLICANT_PHOTO_NOT_SET.name());
			}
			setTemplateAttributes(decryptedJson.toString(), attributes);
			attributes.put(IdType.UIN.toString(), uin);
			if (vid != null)
				attributes.put(IdType.VID.toString(), vid);
			else
				attributes.put(IdType.VID.toString(), "");


			byte[] textFileByte = createTextFile(decryptedJson.toString());
			byteMap.put(UIN_TEXT_FILE, textFileByte);

			boolean isQRcodeSet = setQrCode(decryptedJson.toString(), attributes, isPhotoSet);
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
		String printTextFileJson = utilities.getPrintTextFileJson(utilities.getConfigServerFileStorageURL(),
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
						/*
						 * if (jsonValue.getLanguage().equals(primaryLang)) printTextFileMap.put(value +
						 * "_" + primaryLang, jsonValue.getValue()); if
						 * (jsonValue.getLanguage().equals(secondaryLang)) printTextFileMap.put(value +
						 * "_" + secondaryLang, jsonValue.getValue());
						 */
						if (supportedLang.contains(jsonValue.getLanguage()))
							printTextFileMap.put(value + "_" + jsonValue.getLanguage(), jsonValue.getValue());

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
	 * @param attributes   the attributes
	 * @return true, if successful
	 * @throws QrcodeGenerationException                          the qrcode
	 *                                                            generation
	 *                                                            exception
	 * @throws IOException                                        Signals that an
	 *                                                            I/O exception has
	 *                                                            occurred.
	 * @throws io.mosip.print.exception.QrcodeGenerationException
	 */
	private boolean setQrCode(String qrString, Map<String, Object> attributes,boolean isPhotoSet)
			throws QrcodeGenerationException, IOException, io.mosip.print.exception.QrcodeGenerationException {
		boolean isQRCodeSet = false;
		JSONObject qrJsonObj = JsonUtil.objectMapperReadValue(qrString, JSONObject.class);
		if(isPhotoSet) {
			qrJsonObj.remove("biometrics");
		}
		byte[] qrCodeBytes = qrCodeGenerator.generateQrCode(qrJsonObj.toString(), QrVersion.V30);
		if (qrCodeBytes != null) {
			String imageString = Base64.encodeBase64String(qrCodeBytes);
			attributes.put(QRCODE, "data:image/png;base64," + imageString);
			isQRCodeSet = true;
		}

		return isQRCodeSet;
	}

	/**
	 * Sets the applicant photo.
	 *
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

			String mapperJsonString = utilities.getIdentityMappingJson(utilities.getConfigServerFileStorageURL(),
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
							if (supportedLang.contains(jsonValue.getLanguage()))
								attribute.put(value + "_" + jsonValue.getLanguage(), jsonValue.getValue());
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
			printLogger.error("Error while parsing Json file" ,e);
			throw new ParsingException(PlatformErrorMessages.PRT_RGS_JSON_PARSING_EXCEPTION.getMessage(), e);
		}
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
				uinCardPd = uinCardPd.concat(getParameter(jsonValues, templateLang));

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

	private void printStatusUpdate(String requestId, byte[] data, String credentialType)
			throws DataShareException, ApiNotAccessibleException, IOException, Exception {
		DataShare dataShare = null;
		dataShare = dataShareUtil.getDataShare(data, policyId, partnerId);
		CredentialStatusEvent creEvent = new CredentialStatusEvent();
		LocalDateTime currentDtime = DateUtils.getUTCCurrentDateTime();
		StatusEvent sEvent = new StatusEvent();
		sEvent.setId(UUID.randomUUID().toString());
		sEvent.setRequestId(requestId);
		sEvent.setStatus("printing");
		sEvent.setUrl(dataShare.getUrl());
		sEvent.setTimestamp(Timestamp.valueOf(currentDtime).toString());
		creEvent.setPublishedOn(new DateTime().toString());
		creEvent.setPublisher("PRINT_SERVICE");
		creEvent.setTopic(topic);
		creEvent.setEvent(sEvent);
		webSubSubscriptionHelper.printStatusUpdateEvent(topic, creEvent);
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


	public org.json.JSONObject decryptAttribute(org.json.JSONObject data, String encryptionPin, String credential)
			throws ParseException {

		// org.json.JSONObject jsonObj = new org.json.JSONObject(credential);
		JSONParser parser = new JSONParser(); // this needs the "json-simple" library
		Object obj = parser.parse(credential);
		JSONObject jsonObj = (org.json.simple.JSONObject) obj;

		JSONArray jsonArray = (JSONArray) jsonObj.get("protectedAttributes");
		if (Objects.isNull(jsonArray)) {
			return data;
		}
		for (Object str : jsonArray) {

				CryptoWithPinRequestDto cryptoWithPinRequestDto = new CryptoWithPinRequestDto();
				CryptoWithPinResponseDto cryptoWithPinResponseDto = new CryptoWithPinResponseDto();

				cryptoWithPinRequestDto.setUserPin(encryptionPin);
				cryptoWithPinRequestDto.setData(data.getString(str.toString()));
				try {
					cryptoWithPinResponseDto = cryptoUtil.decryptWithPin(cryptoWithPinRequestDto);
				} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
					printLogger.error("Error while decrypting the data" ,e);
					throw new CryptoManagerException(PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getCode(),
							PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getMessage(), e);
				}
				data.put((String) str, cryptoWithPinResponseDto.getData());
			
			}

		return data;
	}
}
	
