/*
 * @test /nodynamiccopyright/
 * @bug 8064464
 * @summary regression with type inference of conditional expression
 * @compile/fail/ref=T8064464.out -XDrawDiagnostics T8064464.java
 */

import java.util.List;

class T8064464 {

  String f(Object o) { return null; }
  Integer f(int i) { return null; }

  <X extends Integer> X id() { return null; }

  void m(List<Integer> lx) {
    Integer i1 = f(!lx.isEmpty() ? 0 : lx.get(0)); //ok --> f(int)
    Integer i2 = f(!lx.isEmpty() ? lx.get(0) : 0); //ok --> f(int)
    f(!lx.isEmpty() ? id() : 0); // ambiguous
    f(!lx.isEmpty() ? 0 : id()); // ambiguous
  }
}
