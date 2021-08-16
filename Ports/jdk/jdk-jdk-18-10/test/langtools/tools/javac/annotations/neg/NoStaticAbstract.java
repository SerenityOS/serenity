/*
 * @test /nodynamiccopyright/
 * @bug 8022322
 * @summary Static methods are not allowed in an annotation.
 * @compile/fail/ref=NoStaticAbstract.out -XDrawDiagnostics NoStaticAbstract.java
 */

@interface NoStaticAbstract {
    static int m();
}
