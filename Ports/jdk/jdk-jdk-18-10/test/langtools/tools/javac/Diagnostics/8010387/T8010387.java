/**
 * @test /nodynamiccopyright/
 * @bug     8010387
 * @summary rich diagnostic sometimes contain wrong type variable numbering
 * @compile/fail/ref=T8010387.out -XDrawDiagnostics --diags=formatterOptions=disambiguateTvars,where T8010387.java
 */
abstract class T8010387<X> {

    interface F<X> { }

    <P> void test() {
        m(new F<P>() { });
    }


    abstract <T> T8010387<?> m(F<? extends X> fx);
}
