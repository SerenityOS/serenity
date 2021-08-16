/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that default overrides are properly type-checked
 * @compile/fail/ref=Neg09.out -Werror -Xlint:unchecked -XDrawDiagnostics Neg09.java
 */
import java.util.List;

class Neg09 {
    interface I {
        default List<String> m() { return null; }
    }

    static class C1 {
        public List m() { return null; } //unchecked (return) override
    }

    static class C2 extends C1 implements I { }

    static class C3 implements I {
        public List m() { return null; } //unchecked (return) override
    }
}
