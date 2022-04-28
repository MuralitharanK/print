package io.mosip.print.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "msp_card")
@Getter
@Setter
public class PrintcardMCE {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "json_data ")
    private String name;
    @Column(name = "province")
    private String province;
    @Column(name = "city")
    private String city;
    @Column(name = "zone")
    private String zone;
    @Column(name = "zip")
    private String zip;
    @Column(name = "download_date")
    private Date downloadDate;
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "registration_date")
    private Timestamp registrationDate;
    @Column(name = "registration_center_id")
    private String registrationCenterId;
    @Column(name = "status")
    private int status;
    @Column(name = "request_id1")
    private String requestId1;
    @Column(name = "resident")
    private String resident;
    @Column(name = "introducer")
    private String introducer;
    @Column(name = "birthdate")
    private Date birthdate;

    public PrintcardMCE() {

    }


}
