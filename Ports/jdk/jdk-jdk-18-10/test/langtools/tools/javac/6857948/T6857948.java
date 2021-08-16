/*
 * @test /nodynamiccopyright/
 * @bug 6857948
 * @summary 6857948: Calling a constructor with a doubly bogus argument causes an internal error
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6857948.out -XDrawDiagnostics T6857948.java
 */

class Foo {
   Foo(String v) {}
};

class Test {
   public static void main() {
      Foo f = new Foo("Hello!",nosuchfunction()) {};
   }
}
