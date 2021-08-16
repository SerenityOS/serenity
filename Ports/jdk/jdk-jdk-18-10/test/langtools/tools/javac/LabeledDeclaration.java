/*
 * @test  /nodynamiccopyright/
 * @bug 4039843 8057652
 * @summary The compiler should not allow labeled declarations.
 * @author turnidge
 *
 * @compile/fail/ref=LabeledDeclaration.out -XDrawDiagnostics  LabeledDeclaration.java
 */

class LabeledDeclaration {
    void method() {
    foo: int i = 111;
    }
}
