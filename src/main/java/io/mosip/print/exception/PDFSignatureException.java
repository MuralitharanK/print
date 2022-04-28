package io.mosip.print.exception;


public class PDFSignatureException extends BaseUncheckedException{


	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public PDFSignatureException() {
		super();

	}

	/**
	 * Instantiates a new file not found in destination exception.
	 *
	 * @param errorMessage the error message
	 */
	public PDFSignatureException(String errorMessage) {
		super(PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.getCode(), errorMessage);
	}

	/**
	 * Instantiates a new file not found in destination exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public PDFSignatureException(String message, Throwable cause) {
		super(PlatformErrorMessages.PRT_PRT_PDF_SIGNATURE_EXCEPTION.getCode() + EMPTY_SPACE, message, cause);

	}
}