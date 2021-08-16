/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary verify specialized enum classes can't be abstract
 * @author Joseph D. Darcy
 *
 * @compile/fail/ref=FauxSpecialEnum2.out -XDrawDiagnostics  FauxSpecialEnum2.java
 */

public enum FauxSpecialEnum2 {
    XRAY,
    GAMMA {
        abstract void test();
    };
}
