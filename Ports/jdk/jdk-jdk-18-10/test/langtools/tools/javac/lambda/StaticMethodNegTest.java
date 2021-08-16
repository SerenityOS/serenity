/**
 * @test    /nodynamiccopyright/
 * @bug     8008337
 * @author  sogoel
 * @summary static method is called via super
 * @compile/fail/ref=StaticMethodNegTest.out -XDrawDiagnostics StaticMethodNegTest.java
 */

interface A {
  static String m() {
    return "A";
  }
}

interface B {
  static String m() {
    return "B";
  }
}

interface AB extends A, B {
 static String m() {
   return A.super.m();
 }
}

