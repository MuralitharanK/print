package io.mosip.print.service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.print.constant.LoggerFileConstant;
import io.mosip.print.logger.PrintLogger;
import io.mosip.print.model.PrintcardMCE;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Repository
@Transactional
public class PrintDBService {
    @PersistenceContext
    private EntityManager entityManager;

    public synchronized void savePrintdata(PrintcardMCE obj) {
        try {
            long taskStart = System.currentTimeMillis();
            PrintcardMCE card = new PrintcardMCE();
            card.setId(obj.getId());
            card.setName(obj.getName());
//            card.setProvince(obj.getProvince());
//            card.setCity(obj.getCity());
//            card.setZip(obj.getZip());
//            card.setZone(obj.getZone());
            card.setRequestId(obj.getRequestId());
//            card.setRequestId1(obj.getRequestId1());
//            card.setDownloadDate(new Date());
//            card.setRegistrationDate(obj.getRegistrationDate());
//            card.setRegistrationCenterId(obj.getRegistrationCenterId());
//            card.setResident(obj.getResident());
//            card.setIntroducer(obj.getIntroducer());
//            card.setRid(obj.getRid());

//        card.setId(obj.getId());
//            card.setBirthdate(obj.getBirthdate());
            entityManager.persist(card);

        } catch (Exception e) {

        }
    }


    ;

    // Map<String, byte[]> getDocuments(String credentialSubject, String sign,
    // String cardType,
    // boolean isPasswordProtected);

}
