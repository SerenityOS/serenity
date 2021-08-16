/*
 * @test /nodynamiccopyright/
 * @bug 8022322
 * @summary Default methods are not allowed in an annotation.
 * @compile/fail/ref=NoDefault.out -XDrawDiagnostics NoDefault.java
 */
@interface NoDefault {
    default int m() {return 0;}
}
