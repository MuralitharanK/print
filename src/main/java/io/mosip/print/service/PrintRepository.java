package io.mosip.print.service;

import io.mosip.print.model.PrintcardMCE;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PrintRepository extends CrudRepository<PrintcardMCE, String> {
    @Query("SELECT max(p.id) FROM PrintcardMCE p")
    public Integer sno();
    @Query("SELECT count(p.id) FROM PrintcardMCE p where p.requestId= ?1 ")
    public Integer countById(@Param("requestId") String printid) ;

    @Query("SELECT count(p.id) FROM PrintcardMCE p where p.requestId1= ?1 ")
    public Integer countByRequestId1(@Param("requestId1") String rid) ;

    @Query("SELECT p.id FROM PrintcardMCE p where p.requestId1= ?1 ")
    public Integer findIdByRequestId1(@Param("requestId1") String rid);
    @Modifying
    @Query("update PrintcardMCE p set p.status=?3 where p.requestId1= ?1 and p.status=?2 ")
    public Integer updateExistingId(@Param("requestId1") String rid,@Param("status") int status,@Param("status") int newstatus);
}
