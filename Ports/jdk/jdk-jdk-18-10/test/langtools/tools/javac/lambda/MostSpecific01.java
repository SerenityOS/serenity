/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check correctness of structural most specific test routine
 * @compile/fail/ref=MostSpecific01.out -XDrawDiagnostics MostSpecific01.java
 */

class Test {

    interface IntMapper {
        int map();
    }

    interface LongMapper {
        long map();
    }

    void m(IntMapper im, String s) { }
    void m(LongMapper lm, Integer s) { }

    void test() {
        m(()->1, null);
    }
}
