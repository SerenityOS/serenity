/*
 * @test /nodynamiccopyright/
 * @summary Check usages of underscore as identifier generate warnings
 * @compile/fail/ref=UnderscoreAsIdent8.out -source 8 -Xlint:-options -XDrawDiagnostics -Werror UnderscoreAsIdent.java
 * @compile/fail/ref=UnderscoreAsIdent9.out -XDrawDiagnostics -Werror UnderscoreAsIdent.java
 */
package _._;

import _._;

class _ {
    String _ = null;
    void _(String _) { }
    void testLocal() {
        String _ = null;
    }
    void testFor() {
        for (int _ = 0; _ < 10; _++);
    }
    void testTry() {
        try { } catch (Throwable _) { }
    }
    void testLabel() {
        _:
        for (;;) {
            break _;
        }
        _:
        for (;;) {
            continue _;
        }
    }
}
