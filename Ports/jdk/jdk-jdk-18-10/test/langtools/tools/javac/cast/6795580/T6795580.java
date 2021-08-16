/*
 * @test /nodynamiccopyright/
 * @author Maurizio Cimadamore
 * @bug     6795580
 * @summary parser confused by square brackets in qualified generic cast
 * @compile/fail/ref=T6795580.out -XDrawDiagnostics T6795580.java
 */

class T6795580 {
    class Outer<S> {
        class Inner<T> {}
    }

    void cast1(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<Integer>.Inner<Long>[])p;
    }

    void cast2(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<? extends Number>.Inner<Long>[])p;
    }

    void cast3(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<Integer>.Inner<? extends Number>[])p;
    }

    void cast4(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<? extends Number>.Inner<? extends Number>[])p;
    }

    void cast5(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<? super Number>.Inner<Long>[])p;
    }

    void cast6(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<Integer>.Inner<? super Number>[])p;
    }

    void cast7(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<? super Number>.Inner<? super Number>[])p;
    }

    void cast8(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<? extends String>.Inner<Long>[])p;
    }

    void cast9(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<Integer>.Inner<? extends String>[])p;
    }

    void cast10(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<? super String>.Inner<Long>[])p;
    }

    void cast11(Outer<Integer>.Inner<Long>[] p) {
        Object o = (Outer<Integer>.Inner<? super String>[])p;
    }
}
