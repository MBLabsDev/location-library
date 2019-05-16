package br.com.mblabs.location;

/**
 * Exception used to describe errors occurred when try to use
 * in location layer.
 */
@SuppressWarnings("serial")
public class LocationException extends Exception {

    private LocationErrorCode errorCode;
    private Exception innerException;

    /**
     * Define all possible error code
     */
    public enum LocationErrorCode {
        GENERIC_ERROR, CONTEXT_NULL,
    }

    public LocationException(String errorMessage, LocationErrorCode errorCode) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public LocationException(LocationErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public LocationErrorCode getErrorCode() {
        return errorCode;
    }
}
