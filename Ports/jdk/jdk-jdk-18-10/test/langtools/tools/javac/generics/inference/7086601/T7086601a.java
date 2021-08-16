/**
 * @test /nodynamiccopyright/
 * @bug 7086601
 * @summary Error message bug: cause for method mismatch is 'null'
 * @compile/fail/ref=T7086601a.out -XDrawDiagnostics T7086601a.java
 */

class T7086601 {
    static <S> void m1(Iterable<? super S> s1, Iterable<? super S> s2) { }
    static void m1(Object o) {}

    static <S> void m2(Iterable<? super S> s1, Iterable<? super S> s2, Iterable<? super S> s3) { }
    static void m2(Object o) {}

    @SafeVarargs
    static <S> void m3(Iterable<? super S>... ss) { }
    static void m3(Object o) {}

    static void test1(Iterable<String> is, Iterable<Integer> ii) {
        m1(is, ii);
    }

    static void test2(Iterable<String> is, Iterable<Integer> ii, Iterable<Double> id) {
        m2(is, ii, id);
    }

    static void test3(Iterable<String> is, Iterable<Integer> ii) {
        m3(is, ii);
    }

    static void test4(Iterable<String> is, Iterable<Integer> ii, Iterable<Double> id) {
        m3(is, ii, id);
    }
}
