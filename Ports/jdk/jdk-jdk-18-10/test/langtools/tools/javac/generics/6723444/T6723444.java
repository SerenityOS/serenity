/*
 * @test /nodynamiccopyright/
 * @bug 6723444
 *
 * @summary javac fails to substitute type variables into a constructor's throws clause
 * @author Mark Mahieu
 * @compile/fail/ref=T6723444_1.out -Xlint:-options -source 7 -XDrawDiagnostics T6723444.java
 * @compile/fail/ref=T6723444_2.out -XDrawDiagnostics T6723444.java
 *
 */
public class T6723444 {

    static class Foo<X extends Throwable> {
        Foo() throws X {}
    }

    <X extends Throwable> T6723444()
        throws X {}

    <X extends Throwable> T6723444(Foo<X> foo)
        throws X {}

    <X1 extends Throwable, X2 extends Throwable> T6723444(Foo<X1> foo, int i)
        throws X1, X2 {}

    public static void main(String[] args) throws Exception {

        // the following 8 statements should compile without error

        Foo<Exception> exFoo = new Foo<Exception>();
        exFoo = new Foo<Exception>() {};

        new<Exception> T6723444();
        new<Exception> T6723444() {};
        new T6723444(exFoo);
        new T6723444(exFoo) {};
        new<Exception, Exception> T6723444(exFoo, 1);
        new<Exception, Exception> T6723444(exFoo, 1) {};

        // the remaining statements should all raise an
        // unreported exception error

        new T6723444(exFoo, 1);
        new T6723444(exFoo, 1) {};

        Foo<Throwable> thFoo = new Foo<Throwable>();
        thFoo = new Foo<Throwable>() {};

        new<Throwable> T6723444();
        new<Throwable> T6723444() {};
        new T6723444(thFoo);
        new T6723444(thFoo) {};
        new T6723444(thFoo, 1);
        new T6723444(thFoo, 1) {};
        new<Throwable, Throwable> T6723444(thFoo, 1);
        new<Throwable, Throwable> T6723444(thFoo, 1) {};
    }
}
