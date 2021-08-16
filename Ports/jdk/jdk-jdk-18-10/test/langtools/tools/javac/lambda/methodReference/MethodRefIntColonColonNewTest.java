/**
 * @test    /nodynamiccopyright/
 * @bug     8139245
 * @summary compiler crashes with exception on int:new method reference and generic method inference
 * @compile/fail/ref=MethodRefIntColonColonNewTest.out -XDrawDiagnostics MethodRefIntColonColonNewTest.java
 */

public class MethodRefIntColonColonNewTest {

    interface SAM<T> {
        T m(T s);
    }

    static <T> SAM<T> infmethod(SAM<T> t) { return t; }

    public static void main(String argv[]) {
        SAM<Object> s = infmethod(int::new);
        s.m();
    }
}
