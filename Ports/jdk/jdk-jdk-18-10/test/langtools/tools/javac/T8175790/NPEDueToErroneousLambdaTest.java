/*
 * @test /nodynamiccopyright/
 * @bug 8175790
 * @summary field JCVariableDecl.vartype can't be null after post attribution analysis
 * @compile/fail/ref=NPEDueToErroneousLambdaTest.out -XDrawDiagnostics NPEDueToErroneousLambdaTest.java
 */

import java.util.List;
import java.util.function.Function;

public abstract class NPEDueToErroneousLambdaTest {

    interface R {}
    interface A {}
    interface S {}

    abstract <I, O> Function<I, O> p(final Function<I, O> function);
    abstract <I, O> List<O> t(Function<? super I, ? extends O> function);

    public void f() {
        t(p(new Function<A, Object>() {
            public List<Object> apply(A a) throws Exception {
                return t((Function<R, S>)input -> {
                        return t(p((Function<Boolean, S>) i -> null), null);
                });
            }
        }));
    }
}
