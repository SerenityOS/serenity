/*
 * @test /nodynamiccopyright/
 * @bug 8152411
 * @summary Regression: nested unchecked call does not trigger erasure of return type
 *
 * @compile/fail/ref=T8152411.out -XDrawDiagnostics T8152411.java
 */
import java.util.List;

class T8152411 {
        <A2 extends A, A> A m(List<? super A2> a2) { return null; }
        <B> B g(B b) { return null; }

        void test() {
                List<Integer> I = null;
                String s = g(m(I));
        }
}
