/*
 * @test    /nodynamiccopyright/
 * @bug     4209652 4363318
 * @summary Basic test for chained exceptions & Exception.getStackTrace().
 * @author  Josh Bloch
 */

public class ChainedExceptions {
    public static void main(String args[]) {
        try {
            a();
        } catch(HighLevelException e) {
            StackTraceElement[] highTrace = e.getStackTrace();
            int depthTrim = highTrace.length - 2;

            check(e, highTrace[0], "a",    48);
            check(e, highTrace[1], "main", 11);

            Throwable mid = e.getCause();
            StackTraceElement[] midTrace = mid.getStackTrace();
            if (midTrace.length - depthTrim != 4)
                throw new RuntimeException("Mid depth");
            check(mid, midTrace[0], "c",    58);
            check(mid, midTrace[1], "b",    52);
            check(mid, midTrace[2], "a",    46);
            check(mid, midTrace[3], "main", 11);

            Throwable low = mid.getCause();
            StackTraceElement[] lowTrace = low.getStackTrace();
            if (lowTrace.length - depthTrim != 6)
                throw new RuntimeException("Low depth");
            check(low, lowTrace[0], "e",    65);
            check(low, lowTrace[1], "d",    62);
            check(low, lowTrace[2], "c",    56);
            check(low, lowTrace[3], "b",    52);
            check(low, lowTrace[4], "a",    46);
            check(low, lowTrace[5], "main", 11);

            if (low.getCause() != null)
                throw new RuntimeException("Low cause != null");
        }
    }

    static void a() throws HighLevelException {
        try {
            b();
        } catch(MidLevelException e) {
            throw new HighLevelException(e);
        }
    }
    static void b() throws MidLevelException {
        c();
    }
    static void c() throws MidLevelException {
        try {
            d();
        } catch(LowLevelException e) {
            throw new MidLevelException(e);
        }
    }
    static void d() throws LowLevelException {
       e();
    }
    static void e() throws LowLevelException {
        throw new LowLevelException();
    }

    private static final String OUR_CLASS  = ChainedExceptions.class.getName();
    private static final String OUR_FILE_NAME = "ChainedExceptions.java";

    private static void check(Throwable t, StackTraceElement e, String methodName, int n) {
        if (!e.getClassName().equals(OUR_CLASS))
            throw new RuntimeException("Class: " + e, t);
        if (!e.getMethodName().equals(methodName))
            throw new RuntimeException("Method name: " + e, t);
        if (!e.getFileName().equals(OUR_FILE_NAME))
            throw new RuntimeException("File name: " + e, t);
        if (e.getLineNumber() != n)
            throw new RuntimeException("Line number: " + e, t);
    }
}

class HighLevelException extends Exception {
    HighLevelException(Throwable cause) { super(cause); }
}

class MidLevelException extends Exception {
    MidLevelException(Throwable cause)  { super(cause); }
}

class LowLevelException extends Exception {
}
