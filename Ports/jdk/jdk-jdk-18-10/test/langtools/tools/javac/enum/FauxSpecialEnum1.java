/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary verify specialized enum classes can't be abstract
 * @author Joseph D. Darcy
 *
 * @compile/fail/ref=FauxSpecialEnum1.out -XDrawDiagnostics  FauxSpecialEnum1.java
 */

public enum FauxSpecialEnum1 {
    INFRARED {
        void test() {System.out.println("Concrete test");}
    },
    ULTRAVIOLET {
        abstract void test();
    };
    abstract void test();
}
