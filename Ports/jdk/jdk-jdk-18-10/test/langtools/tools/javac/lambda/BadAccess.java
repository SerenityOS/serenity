/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that non-static variables are not accessible from static lambdas
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadAccess.out -XDrawDiagnostics BadAccess.java
 */

public class BadAccess {

    int i;
    static int I;

    interface SAM {
        int m();
    }

    static void test1() {
        int l = 0; //effectively final
        final int L = 0;
        SAM s = ()-> i + I + l + L;
    }

    void test2() {
        int l = 0; //effectively final
        final int L = 0;
        SAM s = ()-> i + I + l + L;
    }
}
