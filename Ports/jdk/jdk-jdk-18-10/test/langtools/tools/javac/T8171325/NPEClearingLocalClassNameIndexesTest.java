/*
 * @test  /nodynamiccopyright/
 * @bug 8171325
 * @summary NPE in Check.clearLocalClassNameIndexes
 * @compile/fail/ref=NPEClearingLocalClassNameIndexesTest.out -XDrawDiagnostics NPEClearingLocalClassNameIndexesTest.java
 */

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NPEClearingLocalClassNameIndexesTest {
    <A> void f(List<A> t) {}
    <B, C> C g(C u, Function<B, C> v) { return null; }
    <D> D g(Supplier<D> w) { return null; }

    public void test() {
        f(g((String) null, task -> g(new NoSuch() {})));
        f(g((String) null, task -> g(new NoSuch<int>() {})));
    }
}
