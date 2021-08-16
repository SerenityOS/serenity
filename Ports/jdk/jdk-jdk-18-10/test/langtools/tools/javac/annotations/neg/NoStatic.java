/*
 * @test /nodynamiccopyright/
 * @bug 8022322
 * @summary Static methods are not allowed in an annotation.
 * @compile/fail/ref=NoStatic.out -XDrawDiagnostics NoStatic.java
 */

@interface NoStatic {
    static int m() {return 0;}
}
