/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  javac accepts ill-formed lambda/method reference targets
 * @compile/fail/ref=TargetType49.out -XDrawDiagnostics TargetType49.java
 */
class TargetType49 {

    interface F {
        default Object clone() { return null; }
        void m();
    }

    F f1 = ()->{};
    F f2 = this::g;

    void g() { }
}
