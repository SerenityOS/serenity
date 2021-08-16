/**
 * @test /nodynamiccopyright/
 * @bug     6863465
 * @summary javac doesn't detect circular subclass dependencies via qualified names
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6863465c.out -XDrawDiagnostics T6863465c.java
 */

class T6863465c {
    static class x { static interface y {} }
    static class z extends x implements c.b {}
    static class a { static interface b { static interface d {} } }
    static class c extends a implements z.y, z.d {}
}
