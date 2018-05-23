/**
 * created by Stanley Tian
 */
public final class ProxyException extends Exception {

    public enum ErrorCode{
        FAILED_READ_REQUEST,
        FAILED_READ_HEADER
    }

    // the error code of this instance of ProxyException
    private final ErrorCode errorCode;

    /**
     * the constructor
     * @param errorCode is the error code of this instance
     */
    ProxyException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * error code getter
     * @return error code
     */
    private ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * string representation of this exception
     * @return the string representation of this exception
     */
    @Override
    public String toString() {
        return "ProxyException { " +
                "errorCode=" + getErrorCode() +
                '}';
    }
}
