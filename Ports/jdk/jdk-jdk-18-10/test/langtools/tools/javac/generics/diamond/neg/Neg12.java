/*
 * @test /nodynamiccopyright/
 * @bug 8062373
 *
 * @summary  Test diamond + anonymous classes with non-denotable types
 * @author mcimadamore
 * @compile/fail/ref=Neg12.out Neg12.java -XDrawDiagnostics
 *
 */

 class Neg12 {
    static class Foo<X> {
        Foo(X x) {  }
    }

    static class DoubleFoo<X,Y> {
        DoubleFoo(X x,Y y) {  }
    }

    static class TripleFoo<X,Y,Z> {
        TripleFoo(X x,Y y,Z z) {  }
    }

    Foo<? extends Integer> fi = new Foo<>(1);
    Foo<?> fw = new Foo<>(fi) {}; // Error.
    Foo<?> fw1 = new Foo<>(fi); // OK.
    Foo<? extends Double> fd = new Foo<>(3.0);
    DoubleFoo<?,?> dw = new DoubleFoo<>(fi,fd) {}; // Error.
    DoubleFoo<?,?> dw1 = new DoubleFoo<>(fi,fd); // OK.
    Foo<String> fs = new Foo<>("one");
    TripleFoo<?,?,?> tw = new TripleFoo<>(fi,fd,fs) {}; // Error.
    TripleFoo<?,?,?> tw1 = new TripleFoo<>(fi,fd,fs); // OK.
 }
