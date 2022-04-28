package io.mosip.print.test.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.print.constant.QrVersion;
import io.mosip.print.dto.IdResponseDTO1;
import io.mosip.print.dto.ResponseDTO;
import io.mosip.print.model.EventModel;
import io.mosip.print.service.PrintRestClientService;
import io.mosip.print.service.UinCardGenerator;
import io.mosip.print.service.impl.PrintServiceImpl;
import io.mosip.print.spi.CbeffUtil;
import io.mosip.print.spi.QrCodeGenerator;
import io.mosip.print.test.TestBootApplication;
import io.mosip.print.util.CryptoCoreUtil;
import io.mosip.print.util.JsonUtil;
import io.mosip.print.util.TemplateGenerator;
import io.mosip.print.util.Utilities;


@SpringBootTest(classes = TestBootApplication.class)
@RunWith(SpringRunner.class)
public class PrintServiceTest {

	@InjectMocks
	private PrintServiceImpl printServiceImpl;

	@Mock
	private Environment env;

	@Mock
	private PrintRestClientService<Object> restClientService;

	/** The id response. */
	private IdResponseDTO1 idResponse = new IdResponseDTO1();

	/** The response. */
	private ResponseDTO response = new ResponseDTO();

	/** The template generator. */
	@Mock
	private TemplateGenerator templateGenerator;

	/** The uin card generator. */
	@Mock
	private UinCardGenerator<byte[]> uinCardGenerator;

	/** The utility. */
	@Mock
	private Utilities utility;

	@Mock
	private QrCodeGenerator<QrVersion> qrCodeGenerator;

	@Mock
	private CryptoCoreUtil cryptoCoreUtil;

	@Mock
	private CryptoUtil cryptoUtil;

	@Mock
	private CbeffUtil cbeffutil;

	private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	private EventModel eventModel;

	private String decryptedJson = "json";

	@Before
	public void setUp() throws Exception {

		when(env.getProperty("mosip.print.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		byte[] qrcode = "QRCODE GENERATED".getBytes();
		Mockito.when(qrCodeGenerator.generateQrCode(any(), any())).thenReturn(qrcode);
		ClassLoader classLoader = getClass().getClassLoader();
		File printTextFile = new File(classLoader.getResource("printTextFileJson.json").getFile());
		File mappingFile = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		File credentialFile = new File(classLoader.getResource("credential.json").getFile());

		String mappingFileJson = FileUtils.readFileToString(mappingFile, StandardCharsets.UTF_8);
		String printTextFileJson = FileUtils.readFileToString(printTextFile, StandardCharsets.UTF_8);
		String credentialFileJson = FileUtils.readFileToString(credentialFile, StandardCharsets.UTF_8);
		/*
		 * JSONObject crednetialJson = new JSONObject(credentialFileJson); String
		 * credential = crednetialJson.getString("credential"); String protectionKey =
		 * crednetialJson.getString("protectionKey"); Event event = new Event();
		 * event.setDataShareUri(null); event.setData(data); eventModel.setEvent(event);
		 */
		eventModel = JsonUtil.readValue(credentialFileJson, EventModel.class);
		// PowerMockito.when(Utilities.class, "getJson", "", any()).thenReturn(value);

		Mockito.when(utility.getConfigServerFileStorageURL()).thenReturn("configUrl");
		Mockito.when(utility.getGetRegProcessorIdentityJson()).thenReturn(mappingFileJson);
		Mockito.when(utility.getRegistrationProcessorPrintTextFile()).thenReturn(printTextFileJson);
		Mockito.when(cryptoCoreUtil.decrypt(any())).thenReturn(decryptedJson);

	}

	@Test
	@Ignore
	public void testQrcodegeneration() throws Exception {
		// JSONObject jsonObject = new JSONObject(decryptedJson1);
		// Mockito.when(printServiceImpl.decryptAttribute(any(), any(),
		// any())).thenReturn(jsonObject);
		printServiceImpl.generateCard(eventModel);
	}

}
