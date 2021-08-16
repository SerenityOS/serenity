/*
 * @test /nodynamiccopyright/
 * @author Maurizio Cimadamore
 * @bug     6557182
 * @summary  Unchecked warning *and* inconvertible types
 * @compile/fail/ref=T6557182.out -XDrawDiagnostics -Xlint:unchecked T6557182.java
 */

class T6557182 {

    <T extends Number & Comparable<String>> void test1(T t) {
        Comparable<Integer> ci = (Comparable<Integer>) t;
    }

    <T extends Number & Comparable<? extends Number>> void test2(T t) {
        Comparable<Integer> ci = (Comparable<Integer>) t;
    }
}
