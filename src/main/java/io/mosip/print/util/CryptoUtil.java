package io.mosip.print.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.print.dto.CryptoWithPinRequestDto;
import io.mosip.print.dto.CryptoWithPinResponseDto;
import io.mosip.print.exception.CryptoManagerException;
import io.mosip.print.exception.PlatformErrorMessages;

@Component
public class CryptoUtil {

	private static final int GCM_NONCE_LENGTH = 12;

	private static final int PBE_SALT_LENGTH = 32;

	private static final String AES_KEY_TYPE = "AES";


	@Value("${mosip.kernel.crypto.hash-symmetric-key-length:256}")
	private int symmetricKeyLength;

	@Value("${mosip.kernel.crypto.hash-iteration:100000}")
	private int iterations;

	@Value("${mosip.kernel.crypto.hash-algorithm-name:PBKDF2WithHmacSHA512}")
	private String passwordAlgorithm;


	/*
	 * public static void main(String[] args) throws NoSuchAlgorithmException,
	 * InvalidKeySpecException, InvalidKeyException,
	 * InvalidAlgorithmParameterException, IllegalBlockSizeException,
	 * BadPaddingException { CryptoUtil cu = new CryptoUtil();
	 * CryptoWithPinRequestDto cp = new CryptoWithPinRequestDto(); cp.setData(
	 * "sH0LIHnUee6wGni1stdDrO2GS_MzjwchlO6-0rmWbrduPwO4BE95xEyCDnRzy3joKz_l4UNRa64t15jWcT3dW2vI3w"
	 * ); cp.setUserPin("abc123"); CryptoWithPinResponseDto result =
	 * cu.decryptWithPin(cp); System.out.println("result: " + result); }
	 */
	public CryptoWithPinResponseDto decryptWithPin(CryptoWithPinRequestDto requestDto)
			throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

		String dataToDec = requestDto.getData();
		String userPin = requestDto.getUserPin();

		byte[] decodedEncryptedData = Base64.decodeBase64(dataToDec);
		byte[] pbeSalt = Arrays.copyOfRange(decodedEncryptedData, 0, PBE_SALT_LENGTH);
		byte[] gcmNonce = Arrays.copyOfRange(decodedEncryptedData, PBE_SALT_LENGTH, PBE_SALT_LENGTH + GCM_NONCE_LENGTH);
		byte[] encryptedData = Arrays.copyOfRange(decodedEncryptedData, PBE_SALT_LENGTH + GCM_NONCE_LENGTH,
				decodedEncryptedData.length);

		SecretKey derivedKey = getDerivedKey(userPin, pbeSalt);
		byte[] decryptedData = symmetricDecrypt(derivedKey, encryptedData, gcmNonce, pbeSalt);
		CryptoWithPinResponseDto responseDto = new CryptoWithPinResponseDto();
		responseDto.setData(new String(decryptedData));
		return responseDto;
	}



	private byte[] symmetricDecrypt(SecretKey key, byte[] data, byte[] iv, byte[] aad) throws InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		if (iv == null) {
			return symmetricDecrypt(key, data, aad);
		}
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CryptoManagerException(PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getCode(),
					PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getMessage(), e);
		}


			SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
			if (aad != null) {
				cipher.updateAAD(aad);
			}
			return cipher.doFinal(data);
	}

	private static byte[] symmetricDecrypt(SecretKey key, byte[] data, byte[] aad) {
		byte[] output = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
			byte[] randomIV = Arrays.copyOfRange(data, data.length - cipher.getBlockSize(), data.length);
			SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, randomIV);

			cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
			if (aad != null && aad.length != 0) {
				cipher.updateAAD(aad);
			}
			output = cipher.doFinal(Arrays.copyOf(data, data.length - cipher.getBlockSize()));
		} catch (Exception e) {
			throw new CryptoManagerException(PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getCode(),
					PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getMessage(), e);
		}
		return output;
	}
	private SecretKey getDerivedKey(String userPin, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		String derivedKeyHex = hash(userPin.getBytes(), salt);
		byte[] derivedKey = hexDecode(derivedKeyHex);
		return new SecretKeySpec(derivedKey, AES_KEY_TYPE);
	}

	private String hash(byte[] data, byte[] salt) {
		SecretKeyFactory secretKeyFactory;
		char[] convertedData = new String(data).toCharArray();
		PBEKeySpec pbeKeySpec = new PBEKeySpec(convertedData, salt, iterations, symmetricKeyLength);
		SecretKey key = null;
		try {
			secretKeyFactory = SecretKeyFactory.getInstance(passwordAlgorithm);
			key = secretKeyFactory.generateSecret(pbeKeySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CryptoManagerException(PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getCode(),
					PlatformErrorMessages.PRT_INVALID_KEY_EXCEPTION.getMessage(), e);
		}

		return DatatypeConverter.printHexBinary(key.getEncoded());
	}

	private byte[] hexDecode(String hexData) {

		char[] hexDataCharArr = hexData.toCharArray();
		int dataLength = hexDataCharArr.length;

		if ((dataLength & 0x01) != 0) {
			throw new io.mosip.print.exception.ParseException(hexData, hexData);
		}

		byte[] decodedBytes = new byte[dataLength >> 1];

		for (int i = 0, j = 0; j < dataLength; i++) {
			int f = Character.digit(hexDataCharArr[j], 16) << 4;
			j++;
			f = f | Character.digit(hexDataCharArr[j], 16);
			j++;
			decodedBytes[i] = (byte) (f & 0xFF);
		}
		return decodedBytes;
	}
	/*
	 * public SignatureResponseDto signPDF(PDFSignatureRequestDto request) {
	 * SignatureCertificate signatureCertificate =
	 * keymanagerService.getSignatureCertificate( request.getApplicationId(),
	 * Optional.of(request.getReferenceId()), request.getTimeStamp()); Rectangle
	 * rectangle = new Rectangle(request.getLowerLeftX(), request.getLowerLeftY(),
	 * request.getUpperRightX(), request.getUpperRightY()); OutputStream
	 * outputStream; try { String providerName =
	 * signatureCertificate.getProviderName(); outputStream =
	 * pdfGenerator.signAndEncryptPDF(Base64.decodeBase64(request.getData()),
	 * rectangle, request.getReason(), request.getPageNumber(),
	 * Security.getProvider(providerName),
	 * signatureCertificate.getCertificateEntry(), request.getPassword()); } catch
	 * (IOException | GeneralSecurityException e) { throw new
	 * CryptoManagerException(PlatformErrorMessages.PRT_PDF_SIGN_EXCEPTION.getCode()
	 * , PlatformErrorMessages.PRT_PDF_SIGN_EXCEPTION.getMessage(), e); }
	 * SignatureResponseDto signatureResponseDto = new SignatureResponseDto();
	 * signatureResponseDto
	 * .setData(Base64.encodeBase64URLSafeString(((ByteArrayOutputStream)
	 * outputStream).toByteArray())); return signatureResponseDto; }
	 * 
	 * public SignatureCertificate getSignatureCertificate(String applicationId,
	 * Optional<String> referenceId, String timestamp) { return
	 * getSigningCertificate(applicationId, referenceId, timestamp, true); }
	 * 
	 * private SignatureCertificate getSigningCertificate(String applicationId,
	 * Optional<String> referenceId, String timestamp, boolean isPrivateRequired) {
	 * String alias = null; List<KeyAlias> currentKeyAlias = null; Map<String,
	 * List<KeyAlias>> keyAliasMap = null; LocalDateTime generationDateTime = null;
	 * LocalDateTime expiryDateTime = null; CertificateEntry<X509Certificate,
	 * PrivateKey> certificateEntry = null; LocalDateTime localDateTimeStamp =
	 * DateUtils.getUTCCurrentDateTime(); if (!referenceId.isPresent() ||
	 * referenceId.get().trim().isEmpty()) {
	 * 
	 * keyAliasMap = dbHelper.getKeyAliases(applicationId, KeymanagerConstant.EMPTY,
	 * localDateTimeStamp); } else { keyAliasMap =
	 * dbHelper.getKeyAliases(applicationId, referenceId.get(), localDateTimeStamp);
	 * } currentKeyAlias = keyAliasMap.get(KeymanagerConstant.CURRENTKEYALIAS); if
	 * (currentKeyAlias.size() > 1) {
	 * 
	 * throw new
	 * NoUniqueAliasException(KeymanagerErrorConstant.NO_UNIQUE_ALIAS.getErrorCode()
	 * , KeymanagerErrorConstant.NO_UNIQUE_ALIAS.getErrorMessage()); } else if
	 * (currentKeyAlias.size() == 1) {
	 * 
	 * KeyAlias fetchedKeyAlias = currentKeyAlias.get(0); alias =
	 * fetchedKeyAlias.getAlias(); certificateEntry = getCertificateEntry(alias,
	 * isPrivateRequired); generationDateTime =
	 * fetchedKeyAlias.getKeyGenerationTime(); expiryDateTime =
	 * fetchedKeyAlias.getKeyExpiryTime(); } else if (currentKeyAlias.isEmpty()) {
	 * throw new
	 * NoUniqueAliasException(KeymanagerErrorConstant.NO_UNIQUE_ALIAS.getErrorCode()
	 * , KeymanagerErrorConstant.NO_UNIQUE_ALIAS.getErrorMessage()); } String
	 * providerName = keyStore.getKeystoreProviderName(); return new
	 * SignatureCertificate(alias, certificateEntry, generationDateTime,
	 * expiryDateTime, providerName); }
	 */

}
