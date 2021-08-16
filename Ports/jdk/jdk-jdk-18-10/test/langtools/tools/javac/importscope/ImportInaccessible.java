/**
 * @test /nodynamiccopyright/
 * @bug 8067886
 * @summary Verify that type import on demand won't put inaccessible types into the Scope
 * @compile/fail/ref=ImportInaccessible.out -XDrawDiagnostics ImportInaccessible.java
 */
package p;
import p.ImportInaccessible.Nested.*;

class ImportInaccessible {
    static class Nested<X extends Inner> {
        private static class Inner{}
     }
}
