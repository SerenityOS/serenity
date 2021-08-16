/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver05.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver05.java
 */

class Test extends Test {
   { for ( Integer x : null) {} }
}
