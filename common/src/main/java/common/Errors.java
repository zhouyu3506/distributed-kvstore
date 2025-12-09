package common;


public final class Errors {

    private Errors() {
    }

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String METADATA_STALE = "METADATA_STALE";
    public static final String RETRY_EXHAUSTED = "RETRY_EXHAUSTED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public static class KvException extends RuntimeException {
        private final String code;

        public KvException(String code, String message) {
            super(message);
            this.code = code;
        }

        public KvException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
