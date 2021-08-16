/*
 * @test /nodynamiccopyright/
 * @bug 8203679
 * @summary This is a negative regression test for an AssertionError in DeferredAttr.
 * @compile/fail/ref=MethodRefStuckParenthesized.out -XDrawDiagnostics MethodRefStuckParenthesized.java
 */

public abstract class MethodRefStuckParenthesized {

  interface I {
    String v();
  }

  interface J {
    String v();
  }

  abstract String v();

  abstract void f(I v);

  abstract <X extends J> J g(X x);

  void test() {
    f(g((this::v)));
  }
}
