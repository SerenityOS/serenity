/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver07.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver07.java
 */

class Test extends Test {
    Integer x = 1;
    { do {} while (x); }
}
