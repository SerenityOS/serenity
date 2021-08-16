/*
 * @test  /nodynamiccopyright/
 * @bug 8185451
 * @summary Misleading 'cannot be accessed from outside package' diagnostic for inconsistent varargs override
 * @compile/fail/ref=MisleadingVarArgsErrorMsgTest.out -XDrawDiagnostics MisleadingVarArgsErrorMsgTest.java
 */

class MisleadingVarArgsErrorMsgTest {
    class A {
        void f(int... x) {}
    }

    class B extends A {
        @Override
        void f(int[] x) {}
    }

    {
        new B().f(1);
    }
}
