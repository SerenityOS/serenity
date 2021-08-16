/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver11.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver11.java
 */

class Test extends Test {

    boolean cond;

    void m(Object o) {}

    { m(cond ? null : null); }
}
