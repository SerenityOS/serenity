/*
 * @test /nodynamiccopyright/
 * @bug 4948144
 * @summary Generics: assignment of Class to type parm's default should elicit error
 * @author never
 *
 * @compile/fail/ref=ClassToTypeParm.out -XDrawDiagnostics  ClassToTypeParm.java
 */

class ClassToTypeParm<T> {
    void f(Class c) {
        T t = c;
    }
}
