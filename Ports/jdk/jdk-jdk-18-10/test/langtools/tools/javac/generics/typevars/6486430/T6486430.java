/*
 * @test    /nodynamiccopyright/
 * @bug     6486430
 * @summary Compiler fails to reject access to static member in parameterized type
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T6486430.out -XDrawDiagnostics  T6486430.java
 */

class T6486430<T extends Number> {
    T6486430<?>.Inner.InnerMost x = null;
    static class Inner<S> {
        static class InnerMost {
        }
    }
}
