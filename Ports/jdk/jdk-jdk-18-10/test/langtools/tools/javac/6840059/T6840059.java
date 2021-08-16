/*
 * @test /nodynamiccopyright/
 * @bug 6840059
 * @summary 6758789: Some method resolution diagnostic should be improved
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6840059.out -XDrawDiagnostics T6840059.java
 */

class T6840059 {

    T6840059(Integer x) {}

    void test() {
        new T6840059(""){};
    }
}
