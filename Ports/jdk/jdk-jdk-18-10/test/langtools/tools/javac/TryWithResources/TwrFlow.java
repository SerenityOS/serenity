/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740 7013420
 * @author Joseph D. Darcy
 * @summary Test exception analysis of try-with-resources blocks
 * @compile/fail/ref=TwrFlow.out -XDrawDiagnostics TwrFlow.java
 */

import java.io.IOException;
public class TwrFlow implements AutoCloseable {
    public static void main(String... args) {
        try(TwrFlow twrFlow = new TwrFlow()) {
            System.out.println(twrFlow.toString());
        } catch (IOException ioe) { // Not reachable
            throw new AssertionError("Shouldn't reach here", ioe);
        }
        // CustomCloseException should be caught or added to throws clause
    }

    /*
     * A close method, but the class is <em>not</em> Closeable or
     * AutoCloseable.
     */
    public void close() throws CustomCloseException {
        throw new CustomCloseException();
    }
}

class CustomCloseException extends Exception {}
