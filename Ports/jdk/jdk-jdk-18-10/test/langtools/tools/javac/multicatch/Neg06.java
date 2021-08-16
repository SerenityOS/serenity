/*
 * @test /nodynamiccopyright/
 * @bug 7002070
 *
 * @summary If catch clause has an incompatible type, error pointer points to first exception type in list
 * @author mcimadamore
 * @compile/fail/ref=Neg06.out -XDrawDiagnostics Neg06.java
 *
 */

class Neg06 {
    void test() {
        try { }
        catch (String | Integer s) {}
    }
}
