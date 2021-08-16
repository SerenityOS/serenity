/*
 * @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Twr with different kinds of variables: local, instance, class, array component, parameter
 * @compile/fail/ref=TwrVarKinds.out -XDrawDiagnostics TwrVarKinds.java
 */

public class TwrVarKinds implements AutoCloseable {

    final static TwrVarKinds r1 = new TwrVarKinds();
    final TwrVarKinds r2 = new TwrVarKinds();
    static TwrVarKinds r3 = new TwrVarKinds();
    TwrVarKinds r4 = new TwrVarKinds();

    public static void main(String... args) {

        TwrVarKinds r5 = new TwrVarKinds();

        /* static final field - ok */
        try (r1) {
        }

        /* non-static final field - ok */
        try (r1.r2) {
        }

        /* static non-final field - wrong */
        try (r3) {
            fail("Static non-final field is not allowed");
        }

        /* non-static non-final field - wrong */
        try (r1.r4) {
            fail("Non-static non-final field is not allowed");
        }

        /* local variable - covered by TwrForVariable1 test */

        /* array components - covered by TwrForVariable2 test */

        /* method parameter - ok */
        method(r5);

        /* constructor parameter - ok */
        TwrVarKinds r6 = new TwrVarKinds(r5);

        /* lambda parameter - covered by TwrAndLambda */

        /* exception parameter - ok */
        try {
            throw new ResourceException();
        } catch (ResourceException e) {
            try (e) {
            }
        }
    }

    public TwrVarKinds() {
    }

    public TwrVarKinds(TwrVarKinds r) {
        try (r) {
        }
    }

    static void method(TwrVarKinds r) {
        /* parameter */
        try (r) {
        }
    }

    static void fail(String reason) {
        throw new RuntimeException(reason);
    }

    public void close() {}

    static class ResourceException extends Exception implements AutoCloseable {
        public void close() {}
    }
}
