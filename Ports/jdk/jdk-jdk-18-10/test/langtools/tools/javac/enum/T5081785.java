/*
 * @test /nodynamiccopyright/
 * @bug 5081785
 * @summary enums should be allowed in non-static contexts
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=T5081785.out -XDrawDiagnostics -source 15 T5081785.java
 * @compile T5081785.java
 */

class A1 {
    public void check() {
        class Foo {
            enum STRENGTH{};
        };
    }
}

class A2 {
    public A2 check() {
        return new A2() { enum STRENGTH{}; };
    }
}

class A3 {
    Object o = new Object() { enum STRENGTH{}; };
}

class A4 {
    class B {
        enum C { X, Y, Z }
    }
}
