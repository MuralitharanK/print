package io.mosip.print.service.impl;

import org.springframework.stereotype.Service;

@Service
public interface PrintImpl {

    public Integer sno();

    public Integer countById(String printId) ;
    public Integer countByRequestId1(String printId);
    public Integer findIdByRequestId1(String rid) ;
    public Integer updateExistingId(String rid,int status,int newstatus) ;
}
	
	
	
	


	

