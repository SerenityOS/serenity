/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that target type of a method ref is a SAM type
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=MethodReference04.out -XDrawDiagnostics MethodReference04.java
 */

class MethodReference04 {
    void m(Integer i) {}

    Object o = this::m; //fail - not a valid target type
}
