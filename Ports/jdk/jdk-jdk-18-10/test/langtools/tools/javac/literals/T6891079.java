/* @test /nodynamiccopyright/
 * @bug 6891079 8056897
 * @summary Compiler allows invalid binary literals 0b and oBL
 * @compile/fail/ref=T6891079.out -XDrawDiagnostics T6891079.java
 */

class Test {
    int bi = 0B;
    long bl = 0BL;
    int xi = 0X;
    long xl = 0XL;
}
