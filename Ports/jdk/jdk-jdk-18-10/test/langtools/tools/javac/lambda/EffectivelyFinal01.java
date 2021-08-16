/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  effectively final check fails on method parameter
 * @compile/fail/ref=EffectivelyFinal01.out -XDrawDiagnostics EffectivelyFinal01.java
 */
class EffectivelyFinal01 {

    interface SAM {
        Integer m(Integer i);
    }

    void test(Integer nefPar) {
        SAM s = (Integer h) ->  { Integer k = 0; return k + h + nefPar; };
        nefPar++;  //non-effectively final
    }
}
