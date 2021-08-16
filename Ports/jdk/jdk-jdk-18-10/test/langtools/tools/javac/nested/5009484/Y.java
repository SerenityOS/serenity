/*
 * @test    /nodynamiccopyright/
 * @bug     5009484
 * @summary Compiler fails to resolve appropriate type for outer member
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=Y.out -XDrawDiagnostics  Y.java
 */

public class Y<T> {
    private T t;
    class Foo extends Y<Y<T>> {
        Y<T> y = t;
    }
}
