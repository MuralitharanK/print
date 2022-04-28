package io.mosip.print.spi;

import io.mosip.kernel.core.bioapi.model.CompositeScore;
import io.mosip.kernel.core.bioapi.model.KeyValuePair;
import io.mosip.kernel.core.bioapi.model.QualityScore;
import io.mosip.kernel.core.bioapi.model.Score;
import io.mosip.kernel.core.cbeffutil.entity.BIR;

/**
 * The Interface IBioApi.
 * 
 * @author Sanjay Murali
 * @author Manoj SP
 * 
 */
public interface IBioApi {

	/**
	 * It checks the quality of the provided biometric image and render the
	 * respective quality score.
	 *
	 * @param sample the sample
	 * @param flags  the flags
	 * @return the response
	 */
	QualityScore checkQuality(BIR sample, KeyValuePair[] flags);

	/**
	 * It compares the biometrics and provide the respective matching scores.
	 *
	 * @param sample  the sample
	 * @param gallery the gallery
	 * @param flags   the flags
	 * @return the response
	 */
	Score[] match(BIR sample, BIR[] gallery, KeyValuePair[] flags);

	/**
	 * Extract template.
	 *
	 * @param sample the sample
	 * @param flags  the flags
	 * @return the response
	 */
	BIR extractTemplate(BIR sample, KeyValuePair[] flags);

	/**
	 * It segment the single biometric image into multiple biometric images. Eg:
	 * Split the thumb slab into multiple fingers
	 *
	 * @param sample the sample
	 * @param flags  the flags
	 * @return the response
	 */
	BIR[] segment(BIR sample, KeyValuePair[] flags);

	CompositeScore compositeMatch(BIR[] sampleList, BIR[] recordList, KeyValuePair[] flags);
}
