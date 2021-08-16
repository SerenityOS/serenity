/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check correctness of structural most specific test routine
 * @compile/fail/ref=MostSpecific03.out -XDrawDiagnostics MostSpecific03.java
 */

class Test {

    interface IntMapper {
        int map();
    }

    interface LongMapper {
        long map();
    }

    void m(IntMapper... im) { }
    void m(LongMapper... lm) { }

    void m2(IntMapper im1, IntMapper... im) { }
    void m2(LongMapper... lm) { }

    void test1() {
        m(); //ambiguous
        m(()->1); //ok
        m(()->1, ()->1); //ok
        m(()->1, ()->1, ()->1); //ok
    }

    void test2() {
        m(null, null); //ambiguous
        m(()->1, null); //ambiguous
        m(null, ()->1); //ambiguous
        m(()->1L, null); //ok
        m(null, ()->1L); //ok
    }

    void test3() {
        m2(); //ok
        m2(()->1); //ambiguous
        m2(()->1, ()->1); //ok
        m2(()->1, ()->1, ()->1); //ok
    }

    void test4() {
        m2(null, null, null); //ambiguous
        m2(()->1, null, null); //ambiguous
        m2(null, ()->1, null); //ambiguous
        m2(null, null, ()->1); //ambiguous
        m2(()->1, ()->1, null); //ambiguous
        m2(null, ()->1, ()->1); //ambiguous
        m2(()->1, null, ()->1); //ambiguous

        m2(()->1L, null, null); //ok
        m2(null, ()->1L, null); //ok
        m2(null, null, ()->1L); //ok
        m2(()->1L, ()->1L, null); //ok
        m2(null, ()->1L, ()->1L); //ok
        m2(()->1L, null, ()->1L); //ok
    }
}
