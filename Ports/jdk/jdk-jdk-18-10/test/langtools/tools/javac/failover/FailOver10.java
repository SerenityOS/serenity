/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver10.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver10.java
 */

class Test extends Test {

    boolean cond;

    { Object o = null; }
}
