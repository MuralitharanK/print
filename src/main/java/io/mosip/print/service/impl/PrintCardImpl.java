package io.mosip.print.service.impl;

import io.mosip.print.service.PrintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class PrintCardImpl implements PrintImpl {
    @Autowired
    PrintRepository printRepository;
    @Override
    public Integer sno() {
        return printRepository.sno();
    }
    @Override
    public Integer countById(String printId) {
        return printRepository.countById(printId);
    }
    @Override
    public Integer countByRequestId1(String printId) {
        return printRepository.countByRequestId1(printId);
    }
    @Override
    public Integer findIdByRequestId1(String id) {
        return printRepository.findIdByRequestId1(id);
    }

    @Override
    public Integer updateExistingId(String id,int status,int newstatus) {
        return printRepository.updateExistingId(id,status,newstatus);
    }
}
	
	
	
	


	

