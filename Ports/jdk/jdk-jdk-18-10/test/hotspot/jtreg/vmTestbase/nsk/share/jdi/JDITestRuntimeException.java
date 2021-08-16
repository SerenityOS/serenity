package nsk.share.jdi;

public class JDITestRuntimeException extends RuntimeException {
    public JDITestRuntimeException(String str) {
        super("JDITestRuntimeException : " + str);
    }
}
