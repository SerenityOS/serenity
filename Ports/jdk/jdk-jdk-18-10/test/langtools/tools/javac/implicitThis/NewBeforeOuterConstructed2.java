/*
 * @test /nodynamiccopyright/
 * @bug 4689058
 * @summary unverifiable code for implicit outer in super constructor call
 *
 * @compile/fail/ref=NewBeforeOuterConstructed2.out -XDrawDiagnostics  NewBeforeOuterConstructed2.java
 */

public class NewBeforeOuterConstructed2 {
    NewBeforeOuterConstructed2(Object o) {}
    class Middle extends NewBeforeOuterConstructed2 {
        Middle(int i) {
            super(null);
        }
        Middle() {
            // The 'new' below is illegal, as the outer
            // constructor has not been called when the
            // implicit reference to 'this' is evaluated
            // during the new instance expression.
            super(/*Middle.this.*/new Middle(1));
        }
        class Inner {}
        void f() {
            System.out.println("ok");
        }
    }
    public static void main(String[] args) {
        NewBeforeOuterConstructed2 c = new NewBeforeOuterConstructed2(new Object());
        Middle m = c.new Middle();
        m.f();
    }
}
