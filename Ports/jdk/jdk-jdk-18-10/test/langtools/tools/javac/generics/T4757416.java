/*
 * @test /nodynamiccopyright/
 * @bug 4756416 7170058
 * @summary generics: erasure clash not detected
 * @author gafter
 *
 * @compile/fail/ref=T4757416.out -XDrawDiagnostics   T4757416.java
 */

class T4756416 {
    static class C<A> { A id ( A x) { return x; } }
    interface I<A> { A id(A x); }
    static class D extends C<String> implements I<Integer> {
        public String id(String x) { return x; }
        public Integer id(Integer x) { return x; }
    }
}
