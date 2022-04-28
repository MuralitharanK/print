package io.mosip.print.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BaseRequestDTO<T> {
	public String id;
	public Metadata metadata;
	public T request;
	public String requesttime;
	public String version;
}
