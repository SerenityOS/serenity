/*
 * @test /nodynamiccopyright/
 * @bug 4907941
 * @summary missing ambiguity error
 * @author gafter
 *
 * @compile/fail/ref=Covar3.out -XDrawDiagnostics   Covar3.java
 */

package covar3;

interface Test3<T> {
    void f(     T f);
    void f(String f);
}

class T {
    void f(Test3<String> x) {
        x.f("");
    }
}
