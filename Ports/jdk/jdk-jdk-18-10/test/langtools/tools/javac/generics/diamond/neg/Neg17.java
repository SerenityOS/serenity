/*
 * @test /nodynamiccopyright/
 * @bug 8062373
 * @summary Test that the anonymous class constructor appears to returns a Foo<T>, when it actually returns a Anon$1. (status as of now - may change in future)
 * @compile/fail/ref=Neg17.out Neg17.java -XDrawDiagnostics
 */

import java.util.Collections;

abstract class Neg17<T> {

   abstract void m();

   public static void main(String[] args) {
       Collections.singletonList(new Neg17<>() { void m() {} }).get(0).m(); // good.
       Collections.singletonList(new Neg17<>() {
                 void m() {}
                 private void n() {}
           }).get(0).n(); // bad unknown method n()
   }
}
