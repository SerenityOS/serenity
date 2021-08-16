/*
 * @test /nodynamiccopyright/
 * @bug 8166108
 * @summary Verify that a program cannot access instance state before construction
 * @compile/fail/ref=AnonymousInSuperCallNegTest.out -XDrawDiagnostics AnonymousInSuperCallNegTest.java
 */

public class AnonymousInSuperCallNegTest {

    static class Base {
        Base(Object o) {}
    }

    static class Outer {
        class Inner {}
    }

    public static class JavacBug extends Base {
        int x;
        JavacBug() {
            super(new Outer().new Inner() {
                void foo() {
                    System.out.println("x = " + x);
                }
            }); }
    }

    public static void main(String[] args) {
        new JavacBug();
    }
}