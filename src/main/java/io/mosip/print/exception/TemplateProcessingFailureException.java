package io.mosip.print.exception;


/**
 * The Class TemplateProcessingFailureException.
 */
public class TemplateProcessingFailureException extends BaseUncheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new template processing failure exception.
	 */
	public TemplateProcessingFailureException() {
		super();
	}

	/**
	 * Instantiates a new template processing failure exception.
	 *
	 * @param message
	 *            the message
	 */
	public TemplateProcessingFailureException(String message) {
		super(PlatformErrorMessages.PRT_TEM_PROCESSING_FAILURE.getCode(), message);
	}

	/**
	 * Instantiates a new template processing failure exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public TemplateProcessingFailureException(String message, Throwable cause) {
		super(PlatformErrorMessages.PRT_TEM_PROCESSING_FAILURE.getCode() + EMPTY_SPACE, message, cause);
	}

}
