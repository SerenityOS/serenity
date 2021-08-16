/*
 * @test  /nodynamiccopyright/
 * @bug 7024096
 * @summary Stack trace has invalid line numbers
 * @author Bruce Chapman
 * @compile T7024096.java
 * @run main T7024096
 */

public class T7024096 {
    private static final int START = 14; // starting line number for the test
    public static void main(String[] args) {
        T7024096 m = new T7024096();
        m.nest(START);
        m.nest(START + 1, m.nest(START + 1), m.nest(START + 1),
            m.nest(START + 2),
            m.nest(START + 3, m.nest(START + 3)));
    }

    public T7024096 nest(int expectedline, T7024096... args) {
        Exception e = new Exception("expected line#: " + expectedline);
        int myline = e.getStackTrace()[1].getLineNumber();
        if( myline != expectedline) {
            throw new RuntimeException("Incorrect line number " +
                    "expected: " + expectedline +
                    ", got: " + myline, e);
        }
        System.out.format("Got expected line number %d correct %n", myline);
        return null;
    }
}
