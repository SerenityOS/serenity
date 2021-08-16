/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check lambda can access only effectively-final locals
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadAccess02.out -XDrawDiagnostics BadAccess02.java
 */

public class BadAccess02 {

    interface SAM {
        int m(int h);
    }

    static void test1() {
        int l = 0; //effectively final
        int j = 0; //non-effectively final
        j = 2;
        final int L = 0;
        SAM s = (int h) -> { int k = 0; return h + j + l + L; };
    }

    void test2() {
        int l = 0; //effectively final
        int j = 0; //non-effectively final
        j = 2;
        final int L = 0;
        SAM s = (int h) -> { int k = 0; return h + k + j + l + L; };
    }
}
