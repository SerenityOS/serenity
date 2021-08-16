/*
 * @test    /nodynamiccopyright/
 * @bug     6318240
 * @summary Creation of array of inner class of an enclosing wildcard type doesn't work
 * @compile/fail/ref=BarNeg1a.out -XDrawDiagnostics  BarNeg1a.java
 */

class BarNeg1a<T> {
    Object object = new BarNeg1a<?>.Inner.InnerMost();
    static class Inner<S> {
        static class InnerMost {
        }
    }
}
