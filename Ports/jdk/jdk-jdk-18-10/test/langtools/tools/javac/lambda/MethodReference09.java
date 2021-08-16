/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that non static members cannot be referenced from a method reference qualifier
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=MethodReference09.out -XDrawDiagnostics MethodReference09.java
 */

class MethodReference09 {
    interface SAM {
       String m(Foo f);
    }

    static class Foo<X> {
       String getX() { return null; }

       Foo<X> getThis() { return this; }

       static void test() {
          SAM s1 = Foo.getThis()::getX;
          SAM s2 = this::getX;
       }
    }
}
