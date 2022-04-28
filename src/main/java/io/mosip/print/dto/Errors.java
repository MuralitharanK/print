package io.mosip.print.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Errors {
	String errorCode;
	String message;
}