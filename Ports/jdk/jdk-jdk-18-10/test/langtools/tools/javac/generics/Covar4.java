/*
 * @test /nodynamiccopyright/
 * @bug 4965756
 * @summary no covariant returns involving primitives
 * @author gafter
 *
 * @compile/fail/ref=Covar4.out -XDrawDiagnostics  Covar4.java
 */

public class Covar4 {
    static class A1 {
        public long f() { return 12L; }
    }
    static class B1 extends A1 {
        public int f() { return 12; }
    }
}
