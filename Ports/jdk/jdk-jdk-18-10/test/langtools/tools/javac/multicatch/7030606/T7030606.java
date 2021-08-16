/*
 * @test /nodynamiccopyright/
 * @bug 7030606
 *
 * @summary Project-coin: multi-catch types should be pairwise disjoint
 * @compile/fail/ref=T7030606.out -XDrawDiagnostics T7030606.java
 */

class T7030606 {
    class E1 extends Exception { }
    class E2 extends E1 { }

    void e1() throws E1 { }
    void e2() throws E2 { }

    void m1() {
        try {
            e1();
            e2();
        } catch (NonExistentType | E2 | E1 e) { }
    }

    void m2() {
        try {
            e1();
            e2();
        } catch (NonExistentType | E1 | E2 e) { }
    }

    void m3() {
        try {
            e1();
            e2();
        } catch (E2 | NonExistentType | E1 e) { }
    }

    void m4() {
        try {
            e1();
            e2();
        } catch (E1 | NonExistentType | E2 e) { }
    }

    void m5() {
        try {
            e1();
            e2();
        } catch (E2 | E1 | NonExistentType e) { }
    }

    void m6() {
        try {
            e1();
            e2();
        } catch (E1 | E2 | NonExistentType  e) { }
    }
}
