/**
 * @test /nodynamiccopyright/
 * @bug 8230105
 * @summary Ensuring speculative analysis on behalf of Analyzers works reasonably.
 * @compile/ref=AnalyzerNotQuiteSpeculative.out -XDfind=diamond -XDrawDiagnostics AnalyzerNotQuiteSpeculative.java
 */
public class AnalyzerNotQuiteSpeculative {
    private void test() {
        Subclass1 c1 = null;
        Subclass2 c2 = null;
        Base b = null;

        t(new C<Base>(c1).set(c2));
        t(new C<Base>(b).set(c2));
    }

    public static class Base {}
    public static class Subclass1 extends Base {}
    public static class Subclass2 extends Base {}
    public class C<T extends Base> {
        public C(T t) {}
        public C<T> set(T t) { return this; }
    }
    <T extends Base> void t(C<? extends Base> l) {}
}
