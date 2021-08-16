/**
 * @test  /nodynamiccopyright/
 * @bug 4094658 4277300 4785453
 * @summary Test enforcement of JLS 6.6.1 and 6.6.2 rules requiring that
 * the type to which a component member belongs be accessible in qualified
 * names.
 *
 * @compile pack1/P1.java
 * @compile pack1/P2.java
 * @compile/fail/ref=QualifiedAccess_2.out -XDrawDiagnostics QualifiedAccess_2.java
 */

import pack1.P1;

class A {
    private static class B {
        static class Inner {}
    }
}

class X extends pack1.P1 {
    X() { super("bar"); }
    void foo() {
        /*-----------------*
        // BOGUS: Reports matching constructor not found.
        // OK if 'Q' is made a public constructor.
        Object y = new Q("foo");// ERROR - protected constructor Q inaccessible
        *------------------*/
        // Reports 'P1.R.S' not found at all. (private)
        Object z = new R.S.T();         // ERROR - S is inaccessible
    }
}

class Y {

    class Foo {
        class Bar {}
    }

    class C extends A.B {}              // ERROR - B is inaccessible
    class D extends A.B.Inner {}        // ERROR - B is inaccessible

    static class Quux {
        private static class Quem {
            P1.Foo.Bar x;               // ERROR - Foo is inaccessible
            static class MyError extends Error {}
        }
    }
}

class Z {
    void foo() throws Y.Quux.Quem.MyError {
                                // ERROR - type of Quux not accesible (private)
        throw new Y.Quux.Quem.MyError();
                                // ERROR - type of Quux not accesible (private)
    }
}
