/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that syntax for selecting generic receiver works
 * @author  Maurizio Cimadamore
 *
 * @compile MethodReference08.java
 * @compile/fail/ref=MethodReference08.out -Werror -Xlint:rawtypes -XDrawDiagnostics MethodReference08.java
 */

class MethodReference08 {
    interface SAM {
       String m(Foo f);
    }

    static class Foo<X> {
       String getX() { return null; }

       static void test() {
          SAM s = Foo::getX;
       }
    }
}
