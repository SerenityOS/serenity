/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  case of cyclic type inference (lambda passed where inference var expected)
 * @compile/fail/ref=TargetType26.out -XDrawDiagnostics TargetType26.java
 */

class TargetType26 {
    interface SAM {
       void m();
    }

    <Z> void call(Z z) { }

    { call(() -> { }); }
}
