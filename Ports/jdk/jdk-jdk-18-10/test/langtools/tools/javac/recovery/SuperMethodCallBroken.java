/**
 * @test /nodynamiccopyright/
 * @bug 8259235
 * @summary Invocation of a method from a superinterface in a class that has an erroneous supertype
 *          should not crash javac.
 * @compile/fail/ref=SuperMethodCallBroken.out -XDdev -XDrawDiagnostics SuperMethodCallBroken.java
 */
public abstract class SuperMethodCallBroken extends Undef implements I, java.util.List<String> {
    public void test() {
        I.super.test();
    }
}
interface I {
    public default void test() {}
}
