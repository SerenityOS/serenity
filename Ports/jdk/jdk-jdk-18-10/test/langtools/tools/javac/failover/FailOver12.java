/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver12.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver12.java
 */

class Test extends Test {

    Integer x = 1;

    { try {} catch (Exception e) {} }
}
