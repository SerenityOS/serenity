/*
 * @test /nodynamiccopyright/
 * @bug 6956758
 *
 * @summary  NPE in com.sun.tools.javac.code.Symbol - isSubClass
 * @author Maurizio Cimadamore
 * @compile/fail/ref=T6956758neg.out -XDrawDiagnostics T6956758neg.java
 *
 */

class T6956758neg {

    interface I {}

    static class C {
        <T extends Object & I> T cloneObject(T object) throws Exception {
            return (T)object.clone();
        }
    }
}
