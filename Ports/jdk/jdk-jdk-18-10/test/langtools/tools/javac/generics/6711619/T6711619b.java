/*
 * @test /nodynamiccopyright/
 * @bug 6711619
 *
 * @summary javac doesn't allow access to protected members in intersection types
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6711619b.out -XDrawDiagnostics T6711619b.java
 */

class T6711619b {
    static class X1<E extends X1<E>> {
         private int i;
         E e;
         int f() {
             return e.i;
         }
    }

    static class X2<E extends X2<E>> {
         static private int i;
         int f() {
             return E.i;
         }
    }

    static class X3<E extends X3<E> & java.io.Serializable> {
         private int i;
         E e;
         int f() {
             return e.i;
         }
    }

    static class X4<E extends X4<E> & java.io.Serializable> {
         static private int i;
         int f() {
             return E.i;
         }
    }
}
