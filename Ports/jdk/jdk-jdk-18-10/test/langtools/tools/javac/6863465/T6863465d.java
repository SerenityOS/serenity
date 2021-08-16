/**
 * @test /nodynamiccopyright/
 * @bug     6863465
 * @summary javac doesn't detect circular subclass dependencies via qualified names
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6863465d.out -XDrawDiagnostics T6863465d.java
 */

class T6863465d {
    static class a { static interface b { static interface d {} } }
    static class c extends a implements z.y, z.d {}
    static class x { static interface y { static interface w {} } }
    static class z extends x implements c.b, c.w {}
}
