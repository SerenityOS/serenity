/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver03.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver03.java
 */

class Test extends Test {
   Test i;
}
