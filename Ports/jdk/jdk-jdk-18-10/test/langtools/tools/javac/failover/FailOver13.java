/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver13.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver13.java
 */

class Test extends Test {

    Integer x = 1;

    { x = (Object)o; }
}
