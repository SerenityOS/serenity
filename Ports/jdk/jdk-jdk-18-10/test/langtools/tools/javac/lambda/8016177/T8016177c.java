/*
 * @test /nodynamiccopyright/
 * @bug 8016081 8016178
 * @summary structural most specific and stuckness
 * @compile/fail/ref=T8016177c.out -XDrawDiagnostics T8016177c.java
 */

class T8016177c {

    interface Function<X, Y> {
        Y m(X x);
    }

    interface ExtFunction<X, Y> extends Function<X, Y> { }

    <U, V> U m1(Function<U, V> f) { return null; }
    <U, V> U m1(ExtFunction<U, V> f) { return null; }

    void m2(Function<Integer, Integer> f) { }
    void m2(ExtFunction<Integer, Integer> f) { }

    void m3(Function<Integer, Integer> f) { }
    void m3(ExtFunction<Object, Integer> f) { }

    int g1(Object s) { return 1; }

    int g2(Number s) { return 1; }
    int g2(Object s) { return 1; }

    void test() {
        m1((Integer x)->x); //ok - explicit lambda - subtyping picks most specific
        m2((Integer x)->x); //ok - explicit lambda - subtyping picks most specific
        m3((Integer x)->x); //ok - explicit lambda (only one applicable)

        m1(x->1); //ok - stuck lambda but nominal most specific wins
        m2(x->1); //ok - stuck lambda but nominal most specific wins
        m3(x->1); //ambiguous - implicit lambda & different params

        m1(this::g1); //ok - unambiguous ref - subtyping picks most specific
        m2(this::g1); //ok - unambiguous ref - subtyping picks most specific
        m3(this::g1); //ambiguous - both applicable, neither most specific

        m1(this::g2); //ok - stuck mref but nominal most specific wins
        m2(this::g2); //ok - stuck mref but nominal most specific wins
        m3(this::g2); //ambiguous - different params
    }
}
