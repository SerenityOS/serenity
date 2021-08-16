/*
 * @test /nodynamiccopyright/
 * @bug 4739399
 * @summary generics: crash after error regarding bounds on type variable
 * @author gafter
 *
 * @compile/fail/ref=T4739399.out -XDrawDiagnostics   T4739399.java
 */

class T4739399 {
    class Crash1 {}

    interface C1GenericInterface {}

    interface C1B1<X> {}

    class Crash1Test<X> {
        public <M extends C1GenericInterface,N extends M & C1B1<X>> void meth( ) {}
    }
}
