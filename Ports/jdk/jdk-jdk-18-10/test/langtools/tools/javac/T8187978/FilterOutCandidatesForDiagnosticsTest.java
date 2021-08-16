/*
 * @test  /nodynamiccopyright/
 * @bug 8187978
 * @summary javac can show overload error messages that include non-valid candidates
 * @compile/fail/ref=FilterOutCandidatesForDiagnosticsTest.out -XDrawDiagnostics FilterOutCandidatesForDiagnosticsTest.java
 */

class FilterOutCandidatesForDiagnosticsTest {

    interface C<E> {
        boolean add(E e);
    }

    interface L<E> extends C<E> {
        @Override
        boolean add(E e);
        void add(int index, E element);
    }

    static abstract class AC<E> implements C<E> {
        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    static abstract class AL<E> extends AC<E> implements L<E> {
        @Override
        public boolean add(E e) {
            return true;
        }
        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }
    }

    static class ARL<E> extends AL<E> implements L<E> {
        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void add(int index, E element) {
        }
        private void add(E e, Object[] elementData, int s) {
        }
    }

    void test() {
        make(new ARL<String>(), new ARL<Integer>()).add("");
    }

    <Z> Z make(Z z1, Z z2) {
        return null;
    }
}
