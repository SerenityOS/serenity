/**
 * @test /nodynamiccopyright/
 * @bug     6722234
 * @summary javac diagnostics need better integration with the type-system
 * @author  mcimadamore
 * @compile/fail/ref=T6722234a_1.out -XDrawDiagnostics --diags=formatterOptions=disambiguateTvars T6722234a.java
 * @compile/fail/ref=T6722234a_2.out -XDrawDiagnostics --diags=formatterOptions=disambiguateTvars,where T6722234a.java
 */

class T6722234a<T extends String> {
    <T extends Integer> void test(T t) {
        m(t);
    }
    void m(T t) {}
}
