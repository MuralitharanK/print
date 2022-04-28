package io.mosip.print.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Data
@Getter
@Setter
public class BaseResponseDTO<T> {
	
    String id;
    String version;
    String responsetime;
    String metadata;
    T response;
    Errors[] errors;
    public Errors[] getErrors() {
                   if(errors != null) {
                   return Arrays.copyOf(errors, errors.length);
                   }else {
                                  return null;
                   }
    }

    public void setErrors(List<Errors> errorsList) {
        if (errorsList != null) {
            errors = new Errors[errorsList.size()];
            for (int i = 0; i < errorsList.size(); i++)
                errors[i] = errorsList.get(i);
        }
    }
}
