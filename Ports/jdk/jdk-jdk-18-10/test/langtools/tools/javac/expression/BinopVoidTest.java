/*
 * @test  /nodynamiccopyright/
 * @bug 8074148
 * @summary Attr.visitBinary flags error at wrong position
 *
 * @compile/fail/ref=BinopVoidTest.out -XDrawDiagnostics  BinopVoidTest.java
 */

class BinopVoidTest {
    void foo() {}
    int x = 10 + foo();
    int y = foo() + 10;
    int z = foo() + foo();
}
