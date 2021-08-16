/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  NPE while checking if subinterface is a SAM type
 * @compile/fail/ref=BadConv03.out -XDrawDiagnostics BadConv03.java
 */

class BadConv03 {

    interface A {
        void a();
    }

    interface B extends A { //not a SAM (2 non-override equivalent abstracts!)
        void a(int i);
    }

    B b = ()-> { };
}
