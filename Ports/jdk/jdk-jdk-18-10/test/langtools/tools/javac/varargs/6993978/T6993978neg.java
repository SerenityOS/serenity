/*
 * @test /nodynamiccopyright/
 * @bug     6993978
 * @author mcimadamore
 * @summary  ClassCastException occurs in assignment expressions without any heap pollutions
 * @compile/fail/ref=T6993978neg.out -Xlint:unchecked -Werror -XDrawDiagnostics T6993978neg.java
 */

import java.util.List;

class T6993978neg {
   @SuppressWarnings({"varargs","unchecked"})
   static <X> void m(X... x) {  }
   static void test(List<String> ls) {
       m(ls); //compiler should still give unchecked here
   }
}
