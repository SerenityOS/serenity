/*
 * @test /nodynamiccopyright/
 * @bug 4959929
 * @summary unclear diagnostic for "new T()"
 * @author never
 *
 * @compile/fail/ref=NewGeneric.out -XDrawDiagnostics  NewGeneric.java
 */


public class NewGeneric {
    private static class Type<T> {
        Type() { T t = new T(); }
    }
}
