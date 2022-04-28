package io.mosip.print.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * The persistent class Processed RegPrc print List database table.
 *
 * @author Thamaraikannan
 * @since 1.0.0
 */

@Component
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "msp_card", schema = "print")
public class MspCardEntity implements Serializable {
    /**
     * The Id.
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * The Json Data.
     */
    @Column(name = "json_data")
    private String jsonData;

    /**
     * The Province.
     */
    @Column(name = "province")
    private String province;

    @Column(name = "city")
    private String city;

    @Column(name = "zone")
    private String zone;

    @Column(name = "agegroup")
    private Integer ageGroup;

    @Column(name = "introducer")
    private String introducer;

    @Column(name = "resident")
    private String resident;

    @Column(name = "registration_center_id")
    private String registrationCenterId;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Column(name = "download_date")
    private LocalDateTime downloadDate;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "request_id1")
    private String requestId1;

    @Column(name = "birthdate")
    private Date birthDate;
}
