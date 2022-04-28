package io.mosip.print.exception;
	

/**
 * The Class ApisResourceAccessException.
 * 
 * @author M1049387
 */
public class ApisResourceAccessException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new apis resource access exception.
	 */
	public ApisResourceAccessException() {
		super();
	}

	/**
	 * Instantiates a new apis resource access exception.
	 *
	 * @param message the message
	 */
	public ApisResourceAccessException(String message) {
		super(PlatformErrorMessages.PRT_RCT_UNKNOWN_RESOURCE_EXCEPTION.getCode(), message);
	}

	/**
	 * Instantiates a new apis resource access exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public ApisResourceAccessException(String message, Throwable cause) {
		super(PlatformErrorMessages.PRT_RCT_UNKNOWN_RESOURCE_EXCEPTION.getCode(), message, cause);
	}
}