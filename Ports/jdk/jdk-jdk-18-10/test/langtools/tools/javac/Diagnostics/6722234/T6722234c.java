/**
 * @test /nodynamiccopyright/
 * @bug     6722234
 * @summary javac diagnostics need better integration with the type-system
 * @author  mcimadamore
 * @compile/fail/ref=T6722234c.out -XDrawDiagnostics --diags=formatterOptions=simpleNames T6722234c.java
 */

class T6722234c {
    static class String {}
    <T> void m(String s2) {}

    void test() {
        m("");
    }
}
