/*
 * @test /nodynamiccopyright/
 * @bug 8138840 8139243 8139249
 * @summary Compiler crashes when compiling bitwise operations with illegal operand types
 *          'void' is erroneously accepted as a possible operand for string concatenation
 * @compile/fail/ref=T8139243.out -XDrawDiagnostics T8139243.java
 */

class T8139243 {

    void test(String s) {
        s += m(); // compile time error
    }

    void m() { }
}
