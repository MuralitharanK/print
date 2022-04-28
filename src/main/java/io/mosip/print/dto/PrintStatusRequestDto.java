package io.mosip.print.dto;

import io.mosip.print.constant.PrintTransactionStatus;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@Getter
public class PrintStatusRequestDto {
    private String id;
    private PrintTransactionStatus printStatus;
    private String statusComments;
    private String processedTime;
}
