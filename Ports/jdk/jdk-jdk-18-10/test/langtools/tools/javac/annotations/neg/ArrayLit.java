/*
 * @test /nodynamiccopyright/
 * @bug 4901266
 * @summary JSR175 (4): don't allow "new" in annotations
 * @author gafter
 *
 * @compile/fail/ref=ArrayLit.out -XDrawDiagnostics  ArrayLit.java
 */

@ArrayLit(new int[] {1, 2, 3})
@interface ArrayLit {
    int[] value();
}
