/*
 * @test /nodynamiccopyright/
 * @bug 8138840 8139243 8139249
 * @summary Compiler crashes when compiling bitwise operations with illegal operand types
 * @compile/fail/ref=T8138840.out -XDrawDiagnostics T8138840.java
 */

class T8138840 {
    void test(int x, double d) {
        Object o = x & d;
    }
}
