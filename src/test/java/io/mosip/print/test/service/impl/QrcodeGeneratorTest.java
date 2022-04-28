package io.mosip.print.test.service.impl;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.print.constant.QrVersion;
import io.mosip.print.exception.QrcodeGenerationException;
import io.mosip.print.service.impl.QrcodeGeneratorImpl;
import io.mosip.print.spi.QrCodeGenerator;
import io.mosip.print.test.TestBootApplication;



@SpringBootTest(classes = TestBootApplication.class)
@RunWith(SpringRunner.class)
public class QrcodeGeneratorTest {

	
	private QrCodeGenerator<QrVersion> qrCodeGeneratorImpl = new QrcodeGeneratorImpl();

	private static final String binaryData="01001000011000010111000001110000011110010010000001000101011000010111001101110100011001010111001000100001";

	@Test
	public void testGenerateQrCode() throws Exception {
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V25), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V26), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V27), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V28), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V29), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V30), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V31), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V32), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V33), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V34), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V35), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V36), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V37), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V38), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V39), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCode("data", QrVersion.V40), isA(byte[].class));
	}
	
	@Test
	public void testGenerateQrCodeWithBinaryData() throws Exception {
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V25), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V26), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V27), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V28), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V29), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V30), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V31), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V32), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V33), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V34), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V35), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V36), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V37), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V38), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V39), isA(byte[].class));
		assertThat(qrCodeGeneratorImpl.generateQrCodeFromBinaryData(binaryData, QrVersion.V40), isA(byte[].class));
	}


	@Test(expected = QrcodeGenerationException.class)
	public void testGenerateQrcodeGenerationException() throws Exception {
		qrCodeGeneratorImpl.generateQrCode(
				"sSsaVRzwQRH6Eoh5hhcacmnvkYL2KDtwYOgnRNclaEfccdrtMXfg563x3zMNdRcWwd7TLWM2NZlzWeWc6aG8HfKB5Us2PijzMvjwxnkOfUPTasPOHWFyEbILRIPXUoK4b9GUIPChJKPeZNGeBXQAyj5BmHmw6GfKQqLYHMuDd6n5McrOuoT8I3YJvVfqcnntOfm7AegL8UOEdRxzQjq7JKvNoc9wknRxWhDwEBgFNsiRpMntJGiyGxZxg8CGL5B0NnvzUUJZdemF2au76Fj9A37kWYJLwfolsFxtglUIzO5KqW4gfSMvdsG0V9b4NsOZDvSarYOllUAAwcnouItYvFy1gb0UapCIagqkLmh26M5v6E0PYzPwa3SbcxJ1gHe6H8MJXmMBwJVbT0N0sP6yQIjG4GqlkNsGcaRDOWv6C35V3JSbfxHF7TIFbkENzWQUSH7U3doQlWfR08uwzEstk1Vc9XirRkGUZ05hMNvfzm82JjiTCSovBld10dxM8rMa6Mz5nw3YrbjjWlmgBOVwAtGA8UOfTsrWQ2pLlt7xyeotn4TNtjTGIw5nClO67SxxDRLNtqReY7VbljHzmUm9GUFiqIr8l7B4J4v807elPn6VPYJpcBJ98VEMwwLkPLIlywA2578NvZj5UsGURjMeTdtR6FnMYSso5DD2dI9tLgRXYekAod19sqvXe7m5GS85bsBqaIyappDgAoMIvxJWEHcscxflRV6uRYhsTQNNT2aegXDQh6xeX0aWZysuaHgqxxGo4rE0ya8t5xhZsUlcplhqcSe8zZ8cCBLTnVdgmeTxQhtBAUeJiRdYyzFXkQgDxRzy90RLMrno3siUrRcR9PvppBvfrKXLYpwfzKsOjzhHL8uJXV4n7NixhbWk3ZjEobsiiyYDE52DLL2hrfLBi33VY9kPsEiOgMr1TvBejTeU5VwgbXup6s4J49FmksmZPgSViJ8EzKw1sHG57qB1pcnedWWlljBPkrALPlPVvwh6tcgHtesxh7JUsllJAIEKZTBc5Lc7lAjrFHotiMqlRrjpTIYEqp89kItvGjiKXBAjaFvfYquEW0HhvqhZW5v0CP3SLpduwJ3cn0Xu0qHxpo0zCi5x7itpsJ5jNFKjoqA34FMSIxXD0EC5GcsQ0A7dUjxGYBC1TazUfzbpIVGX4k9AZUFquaXe8ptNVg7Vpc3I7ANNtvYlWp046btHBb64VU9FaxPdVA1Uo3jK9PxcZk7adsF5voS8wt4BgEzogKPNIj6J5ehB0n5SQC6PZ",
				QrVersion.V25);
	}

}
