/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740 6965277 7013420
 * @author Maurizio Cimadamore
 * @summary Test that resource variables are implicitly final
 * @compile/fail/ref=ImplicitFinal.out -XDrawDiagnostics ImplicitFinal.java
 */

import java.io.IOException;

class ImplicitFinal implements AutoCloseable {
    public static void main(String... args) {
        try(ImplicitFinal r = new ImplicitFinal()) {
            r = null; //disallowed
        } catch (IOException ioe) { // Not reachable
            throw new AssertionError("Shouldn't reach here", ioe);
        }

        try(@SuppressWarnings("unchecked") ImplicitFinal r1 = new ImplicitFinal()) {
            r1 = null; //disallowed
        } catch (IOException ioe) { // Not reachable
            throw new AssertionError("Shouldn't reach here", ioe);
        }

        try(final ImplicitFinal r2 = new ImplicitFinal()) {
            r2 = null; //disallowed
        } catch (IOException ioe) { // Not reachable
            throw new AssertionError("Shouldn't reach here", ioe);
        }

        try(final @SuppressWarnings("unchecked") ImplicitFinal r3 = new ImplicitFinal()) {
            r3 = null; //disallowed
        } catch (IOException ioe) { // Not reachable
            throw new AssertionError("Shouldn't reach here", ioe);
        }
    }
    public void close() throws IOException {
        throw new IOException();
    }
}
