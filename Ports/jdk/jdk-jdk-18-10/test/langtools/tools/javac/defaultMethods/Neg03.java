/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that re-abstraction works properly
 * @compile/fail/ref=Neg03.out -XDrawDiagnostics Neg03.java
 */

class Neg03 {
    interface A {
        default void m() { Neg03.one(this); }
    }

    interface B {
        default void m() { Neg03.two(this); }
    }

    interface C extends A, B {
        default void m() { Neg03.one(this); }
    }

    static class X implements C, A { } //ok - ignore extraneous remix of A

    interface D extends A, B {
      void m();  // ok - m() is not reabstracted!
    }

    static class Y implements D, A { } // invalid - abstract D.m()

    interface E extends A {
        void m();  // reabstraction of m()
    }

    static class W implements D, E { } // invalid - abstracts D.m()/E.m()

    static class Z implements D, A, B { } // invalid - abstract D.m()

    static void one(Object a) {  }
    static void two(Object a) {  }
}
