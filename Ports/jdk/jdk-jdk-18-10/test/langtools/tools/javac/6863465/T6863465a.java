/**
 * @test /nodynamiccopyright/
 * @bug     6863465
 * @summary javac doesn't detect circular subclass dependencies via qualified names
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6863465a.out -XDrawDiagnostics T6863465a.java
 */

class T6863465a {
    static class a { static interface b {} }
    static class c extends a implements z.y {}
    static class x { static interface y {} }
    static class z extends x implements c.b {}
}
