/*
 * @test /nodynamiccopyright/
 * @bug 8138840 8139243 8139249
 * @summary Compiler crashes when compiling bitwise operations with illegal operand types
*           Unary operator erroneously applied to non-integral type operand
 * @compile/fail/ref=T8139249.out -XDrawDiagnostics T8139249.java
 */

class T8139249 {
    void test(float f2) {
        float f1 = ~f2;
    }
}
