/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that ill-formed MI hierarchies do not compile
 * @compile/fail/ref=Neg02.out -XDrawDiagnostics Neg02.java
 */

class Neg02 {
     interface A {
         default void m() { Neg02.impl(this); }
     }

     interface B {
         default void m() { Neg02.impl(this); }
     }

     static class X implements A, B { } //error

     void test(X x) {
         x.m();
         ((A)x).m();
         ((B)x).m();
     }

     static void impl(A a) { }
     static void impl(B b) { }
}
