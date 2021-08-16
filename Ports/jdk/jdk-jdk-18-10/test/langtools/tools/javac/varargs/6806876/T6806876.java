/*
 * @test /nodynamiccopyright/
 * @bug     6806876
 * @author mcimadamore
 * @summary  ClassCastException occurs in assignment expressions without any heap pollutions
 * @compile/fail/ref=T6806876.out -Xlint:unchecked -Werror -XDrawDiagnostics T6806876.java
 */

class T6806876 {
    void test(Integer i, Long l) {
        Comparable<?>[] res = m(i, l);
    }

    <T> T[] m(T...a) {
        return null;
    }
}
