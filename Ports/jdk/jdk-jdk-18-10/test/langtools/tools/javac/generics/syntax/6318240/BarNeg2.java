/*
 * @test    /nodynamiccopyright/
 * @bug     6318240
 * @summary Creation of array of inner class of an enclosing wildcard type doesn't work
 * @compile/fail/ref=BarNeg2.out -XDrawDiagnostics  BarNeg2.java
 */

class BarNeg2<T> {
    BarNeg2<?>.Inner<?>.InnerMost[] array = new BarNeg2<Object>.Inner<?>.InnerMost[10];
    class Inner<S> {
        class InnerMost {
        }
    }
}
