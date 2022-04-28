package io.mosip.print.test.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.print.controller.Print;
import io.mosip.print.model.EventModel;
import io.mosip.print.service.PrintService;
import io.mosip.print.test.TestBootApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestBootApplication.class)
@AutoConfigureMockMvc
public class PrintControllerTest {

	@InjectMocks
	private Print printController;

	@Mock
	PrintService printService;

	private MockMvc mockMvc;

	Gson gson = new GsonBuilder().serializeNulls().create();

	String reqJson;

	String reqCredentialEventJson;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(printController).build();
		EventModel credentialEvent = new EventModel();
		reqCredentialEventJson = gson.toJson(credentialEvent);
	}

	@Test
	public void testHandleSubscribeEventSuccess() throws Exception {
		byte[] pdfbytes = "pdf".getBytes();
//		Mockito.when(printService.generateCard(Mockito.any())).thenReturn(pdfbytes);
		mockMvc.perform(MockMvcRequestBuilders.post("/print/callback/notifyPrint")
				.contentType(MediaType.APPLICATION_JSON_VALUE).content(reqCredentialEventJson.getBytes()))
				.andExpect(status().isOk());
	}

}
