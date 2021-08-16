/*
 * @test /nodynamiccopyright/
 * @bug 4821353
 * @summary new warning "finally cannot complete normally" should not be enabled by default
 * @author gafter
 *
 * @compile/fail/ref=FinallyWarn.out -XDrawDiagnostics -Xlint:finally -Werror FinallyWarn.java
 * @compile -Werror FinallyWarn.java
 */

class FinallyWarn {
    void f() {
        try {
        } finally {
            return;
        }
    }
}
