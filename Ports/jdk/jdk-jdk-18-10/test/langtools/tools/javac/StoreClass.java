/*
 * @test /nodynamiccopyright/
 * @bug 4350352
 * @summary InternalError: store unsupported: com.sun.tools.javac.v8.comp.Items
 * @author gafter
 *
 * @compile/fail/ref=StoreClass.out -XDrawDiagnostics StoreClass.java
 */

class StoreClass {
    void f() {
        StoreClass.class = null;
        int.class = null;
    }
}
