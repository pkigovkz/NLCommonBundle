package kz.gov.pki.knca.types;

import lombok.Data;

/**
 * Created by 860707350375 on 13.02.2018.
 */
@Data
public class ResponseMessage<T> {
    private String code;
    private String message;
    private T responseObject;
}
