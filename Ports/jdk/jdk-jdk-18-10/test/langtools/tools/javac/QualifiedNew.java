/*
 * @test  /nodynamiccopyright/
 * @bug 4406966 6969184
 * @summary null qualifying inner instance creation should be error.
 * @author gafter
 *
 * @compile/fail/ref=QualifiedNew.out -XDrawDiagnostics QualifiedNew.java
 */

class QualifiedNew {
    class Y {}
    class Z {
        Y[] a;
        Object tmp1 = null.new Y();
        Object tmp2 = a.new Y();
    }
}
