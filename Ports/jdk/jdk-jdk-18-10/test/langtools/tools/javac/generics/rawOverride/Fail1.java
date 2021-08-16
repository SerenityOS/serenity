/*
 * @test /nodynamiccopyright/
 * @bug 5073079
 * @summary Allow unchecked override of generified methods in
 * parameterless classes
 * @author Peter von der Ah\u00e9
 *
 * @compile/fail/ref=Fail1.out -XDrawDiagnostics  Fail1.java
 */

interface MyList<T> {}

interface A {
    void f(MyList l);
}

class B {
    public void f(MyList<String> l) { }
}

class C extends B implements A { }
