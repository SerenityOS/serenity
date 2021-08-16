/*
 * @test /nodynamiccopyright/
 * @bug 5029773
 * @summary soundness problem with failure to substitute wildcard as type formal argument
 * @author gafter
 *
 * @compile/fail/ref=Capture2.out -XDrawDiagnostics  Capture2.java
 */

package capture2;

class R<T extends R<T>> {
    T f() { return null; }
    T t;

    void x(R<?> r) {
        this.t = r.f().t; // this should be an error!
    }
}
