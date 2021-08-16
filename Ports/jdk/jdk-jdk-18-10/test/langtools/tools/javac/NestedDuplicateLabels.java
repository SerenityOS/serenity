/*
 * @test /nodynamiccopyright/
 * @bug 1241001
 * @summary The compiler failed to detect duplicate, nested labels.
 * @author turnidge
 *
 * @compile/fail/ref=NestedDuplicateLabels.out -XDrawDiagnostics  NestedDuplicateLabels.java
 */

class NestedDuplicateLabels {
    void method() {
    foo: { { foo: {} } }
    }
}
