package io.mosip.print.service;

import io.mosip.print.model.EventModel;

public interface PrintService {
    
	/**
	 * Get the card
	 * 
	 * 
	 * @param eventModel
	 * @return
	 * @throws Exception
	 */
	public boolean generateCard(EventModel eventModel);

	// Map<String, byte[]> getDocuments(String credentialSubject, String sign,
	// String cardType,
	// boolean isPasswordProtected);
	
}