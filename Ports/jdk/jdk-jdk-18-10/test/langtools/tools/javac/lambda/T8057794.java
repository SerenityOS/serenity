/**
 * @test    /nodynamiccopyright/
 * @bug     8057794
 * @summary The tree for TypeVar.class does not have a type set, which leads to an NPE when
 *          checking if deferred attribution is needed
 * @compile/fail/ref=T8057794.out -XDrawDiagnostics T8057794.java
 */
class T8057794<T> {
    void t() {
        System.out.println(T.class.getSimpleName());
    }
}
