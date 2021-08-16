/*
 * @test /nodynamiccopyright/
 * @bug 4347611 4440249 4508227
 * @summary Check that qualified reference to type via an expression does not crash compiler.
 * @author maddox
 *
 * @compile/fail/ref=ExprQualifiedType.out -XDrawDiagnostics ExprQualifiedType.java
 */

public class ExprQualifiedType {

    static class Nested {
        static int i;
        static void m() { i = 1; }
    }

    static void test() {
        new ExprQualifiedType().Nested.i = 1;
        new ExprQualifiedType().Nested.m();
    }
}
