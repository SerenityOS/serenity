/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that SAM conversion handles Object members correctly
 * @author  Alex Buckley
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=LambdaConv09.out -XDrawDiagnostics LambdaConv09.java
 */

class LambdaConv09 {

    // Not a SAM type; not enough abstract methods
    interface Foo1 {}

    // SAM type; Foo has no abstract methods
    interface Foo2 { boolean equals(Object object); }


    // Not a SAM type; Foo still has no abstract methods
    interface Foo3 extends Foo2 { public abstract String toString(); }

    // SAM type; Bar has one abstract non-Object method
    interface Foo4<T> extends Foo2 { int compare(T o1, T o2); }

    // Not a SAM type; still no valid abstract methods
    interface Foo5 {
        boolean equals(Object object);
        String toString();
    }

    // SAM type; Foo6 has one abstract non-Object method
    interface Foo6<T> {
        boolean equals(Object obj);
        int compare(T o1, T o2);
    }

    // SAM type; Foo6 has one abstract non-Object method
    interface Foo7<T> extends Foo2, Foo6<T> { }

    void test() {
        Foo1 f1 = ()-> { };
        Foo2 f2 = ()-> { };
        Foo3 f3 = x -> true;
        Foo4 f4 = (x, y) -> 1;
        Foo5 f5 = x -> true;
        Foo6 f6 = (x, y) -> 1;
        Foo7 f7 = (x, y) -> 1;
    }
}
