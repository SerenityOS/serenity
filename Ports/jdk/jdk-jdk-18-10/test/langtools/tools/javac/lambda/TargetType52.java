/*
 * @test /nodynamiccopyright/
 * @bug 8005244
 * @summary Implement overload resolution as per latest spec EDR
 *          uncatched sam conversion failure exception lead to javac crash
 * @compile/fail/ref=TargetType52.out -XDrawDiagnostics TargetType52.java
 */
class TargetType52 {

    interface FI<T extends CharSequence, V extends java.util.AbstractList<T>> {
        T m(V p);
    }

    void m(FI<? extends CharSequence, ? extends java.util.ArrayList<? extends CharSequence>> fip) { }

    void test() {
        m(p -> p.get(0));
    }
}
