/*
 * @test /nodynamiccopyright/
 * @bug 8006775
 * @summary the receiver parameter and static methods/classes
 * @author Werner Dietl
 * @compile/fail/ref=StaticThings.out -XDrawDiagnostics StaticThings.java
 */
class Test {
  // bad
  static void test1(Test this) {}

  // bad
  static Object test2(Test this) { return null; }

  class Nested1 {
    // good
    void test3a(Nested1 this) {}
    // good
    void test3b(Test.Nested1 this) {}
    // No static methods
    // static void test3c(Nested1 this) {}
  }
  static class Nested2 {
    // good
    void test4a(Nested2 this) {}
    // good
    void test4b(Test.Nested2 this) {}
    // bad
    static void test4c(Nested2 this) {}
    // bad
    static void test4d(Test.Nested2 this) {}
  }
}
