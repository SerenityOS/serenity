/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check correctness of structural most specific test routine
 * @compile/fail/ref=MostSpecific02.out -XDrawDiagnostics MostSpecific02.java
 */

class Test {

    interface IntMapper {
        int map();
    }

    interface LongMapper {
        long map();
    }

    void m(IntMapper im, LongMapper s) { }
    void m(LongMapper lm, IntMapper s) { }

    void test() {
        m(()->1, ()->1);
    }
}
