/*
 * @test /nodynamiccopyright/
 * @bug     6569633
 * @author mcimadamore
 * @summary  Varargs: parser error when varargs element type is an array
 * @compile/fail/ref=T6569633.out -XDrawDiagnostics T6569633.java
 */

class T6569633 {
    void  m1 (Integer... i[]) { }
    void  m2 (Integer[]... i) { }
    void  m3 (Integer[]... i[]) { }
}
