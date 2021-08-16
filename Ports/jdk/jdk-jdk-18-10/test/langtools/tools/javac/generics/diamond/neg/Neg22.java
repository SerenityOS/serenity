/*
 * @test /nodynamiccopyright/
 * @bug 8132535
 * @summary Compiler fails with diamond anonymous class creation with intersection bound of enclosing class.
 * @compile/fail/ref=Neg22.out Neg22.java -XDrawDiagnostics
 */

public class Neg22  {

    class Outer<X extends Runnable & java.io.Serializable> {
        class Inner<Y> { }
    }

    class Box<Z> {
        Box(Z z) { }
    }

    {
        new Box<>(new Outer<>().new Inner<>()) { };
    }
}
