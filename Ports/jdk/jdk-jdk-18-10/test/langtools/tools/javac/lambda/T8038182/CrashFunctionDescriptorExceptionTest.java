/*
 * @test /nodynamiccopyright/
 * @bug 8038182
 * @summary javac crash with FunctionDescriptorLookupError for invalid functional interface
 * @compile/fail/ref=CrashFunctionDescriptorExceptionTest.out -XDrawDiagnostics CrashFunctionDescriptorExceptionTest.java
 */

class CrashFunctionDescriptorExceptionTest {

    @SuppressWarnings("unchecked")
    void m () {
        bar((B b) -> {});
    }

    <E extends A<E>> void bar(I<E> i) {}

    class A<E> {}

    class B<E> extends A<E> {}

    interface I<E extends A<E>> {
        void foo(E e);
    }
}
