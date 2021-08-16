/*
 * @test /nodynamiccopyright/
 * @bug 6537020
 * @summary JCK tests: a compile-time error should be given in case of ambiguously imported fields (types, methods)
 *
 * @compile/fail/ref=T6537020.out -XDrawDiagnostics T6537020.java
 */

package p;

import static p.T6537020.C.s;

class T6537020 {

    static class A {
       static String s;
    }

    interface B {
       String s = "";
    }

    static class C extends A implements B { }

    Object o = s;
}
