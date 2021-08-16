/* @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Test various JLS changes made for supporting private interface methods.
 * @compile/fail/ref=Private09.out -XDrawDiagnostics Private09.java
 */
class Private09 {
    interface I {
        private private void poo() {}
    }
}
