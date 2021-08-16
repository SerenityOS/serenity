/*
 * @test    /nodynamiccopyright/
 * @bug     6318240
 * @summary Creation of array of inner class of an enclosing wildcard type doesn't work
 * @compile/fail/ref=BarNeg2a.out -XDrawDiagnostics  BarNeg2a.java
 */

class BarNeg2a<T> {
    BarNeg2a.Inner.InnerMost object = new BarNeg2a.Inner<?>.InnerMost();
    static class Inner<S> {
        static class InnerMost {
        }
    }
}
