/*
 * @test /nodynamiccopyright/
 * @bug 4880220
 * @summary Add a warning when accessing a static method via an reference
 *
 * @compile/ref=T4880220.empty.out                                                   T4880220.java
 * @compile/ref=T4880220.warn.out       -XDrawDiagnostics         -Xlint:static      T4880220.java
 * @compile/ref=T4880220.warn.out       -XDrawDiagnostics         -Xlint:all         T4880220.java
 * @compile/ref=T4880220.empty.out      -XDrawDiagnostics         -Xlint:all,-static T4880220.java
 * @compile/ref=T4880220.error.out/fail -XDrawDiagnostics -Werror -Xlint:all         T4880220.java
 */

public class T4880220 {
    void m1() {
        int good_1 = C.m();
        int good_2 = C.f;
        int good_3 = C.x;

        C c = new C();
        int bad_inst_1 = c.m();
        int bad_inst_2 = c.f;
        int bad_inst_3 = c.x;

        int bad_expr_1 = c().m();
        int bad_expr_2 = c().f;
        int bad_expr_3 = c().x;
    }

    void m2() {
        Class<?> good_1 = C.class;
        Class<?> good_2 = C[].class;
    }

    C c() {
        return new C();
    }

    static class C {
        static int m() { return 0; }
        static int f;
        static final int x = 3;
    }
}
