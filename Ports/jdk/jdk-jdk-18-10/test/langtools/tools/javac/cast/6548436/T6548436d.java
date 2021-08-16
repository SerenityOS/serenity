/*
 * @test /nodynamiccopyright/
 * @bug  6548436
 * @summary Incorrect inconvertible types error
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6548436d.out -XDrawDiagnostics  T6548436d.java
 */

public class T6548436d {

    static class Base<E extends Comparable<E>> {}

    static void test(Base<? extends Double> je) {
        Object o = (Base<Integer>)je;
    }
}
