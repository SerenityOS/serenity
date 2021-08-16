/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver04.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver04.java
 */

class Test {
   { new Unknown() {}; }
}
