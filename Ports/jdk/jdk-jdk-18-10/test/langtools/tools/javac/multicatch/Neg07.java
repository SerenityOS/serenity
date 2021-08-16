/*
 * @test  /nodynamiccopyright/
 * @bug 7039822
 * @summary Verify typing of lub of exception parameter w.r.t getClass
 * @author Joseph D. Darcy
 * @compile/fail/ref=Neg07.out -XDrawDiagnostics Neg07.java
 */

public class Neg07 {
    private static void test(int i) {
        try {
            thrower(i);
        } catch (SonException | DaughterException e) {
            Class<? extends HasFoo> clazz2 = e.getClass(); // Rejected!
            HasFoo m = e;
            e.foo();
        }
    }

    private static interface HasFoo {
        void foo();
    }

    static void thrower(int i) throws SonException, DaughterException {
        if (i == 0)
            throw new SonException();
        else
            throw new DaughterException();
    }

    private static class ParentException extends RuntimeException {}

    private static class SonException
        extends ParentException
        implements HasFoo {

        public void foo() {
            System.out.println("SonException.foo");
        }
    }

    private static class DaughterException
        extends ParentException
        implements HasFoo {

        public void foo() {
            System.out.println("DaughterException.foo");
        }
    }
}
