/*
 * @test /nodynamiccopyright/
 * @bug 8052070
 * @summary javac crashes when there are duplicated type parameters
 * @compile/fail/ref=DuplicateTypeParameter.out -XDrawDiagnostics DuplicateTypeParameter.java
 */

public class DuplicateTypeParameter<T, T, A> {
    class Inner <P, P, Q> {}
    public void foo() {
        class Local <M, M, N> {};
    }
}

class Secondary<D, D, E> {}
