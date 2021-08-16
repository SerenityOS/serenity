/*
 * @test    /nodynamiccopyright/
 * @bug     6318240
 * @summary Creation of array of inner class of an enclosing wildcard type doesn't work
 * @compile/fail/ref=BarNeg1.out -XDrawDiagnostics  BarNeg1.java
 */

class BarNeg1<T> {
    BarNeg1<?>.Inner<?>.InnerMost[] array = new BarNeg1<?>.Inner<Object>.InnerMost[10];
    class Inner<S> {
        class InnerMost {
        }
    }
}
