/**
 * @test /nodynamiccopyright/
 * @bug 6943278
 * @summary spurious error message for inference and type-variable with erroneous bound
 * @compile/fail/ref=T6943278.out -XDrawDiagnostics -Xlint:unchecked T6943278.java
 */
class T6943278<X extends Number & NonExistentInterface> {
    <X> T6943278<X> m() { return null;}
    <X extends Number & NonExistentInterface> T6943278<X> m(X x) { return null;}
    T6943278<?> f1 = m();
    T6943278<?> f2 = m("");
}
