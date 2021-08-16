/*
 * @test /nodynamiccopyright/
 * @bug 8022322
 * @summary Default methods are not allowed in an annotation.
 * @compile/fail/ref=NoDefaultAbstract.out -XDrawDiagnostics NoDefaultAbstract.java
 */
@interface NoDefaultAbstract {
    default int m();
}
