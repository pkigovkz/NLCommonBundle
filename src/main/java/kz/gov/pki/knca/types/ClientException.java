package kz.gov.pki.knca.types;

/**
 * Created by Yerlan.Yesmukhanov on 21.06.2016.
 */
public class ClientException extends Exception {
    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }
}
