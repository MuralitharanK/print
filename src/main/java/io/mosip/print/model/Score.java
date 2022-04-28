package io.mosip.print.model;

import lombok.Data;

/** Added for backward compatibility 0.7 */
@Data
public class Score {
	
	private float scaleScore;	  
	private long internalScore;
	private KeyValuePair[] analyticsInfo;

}
