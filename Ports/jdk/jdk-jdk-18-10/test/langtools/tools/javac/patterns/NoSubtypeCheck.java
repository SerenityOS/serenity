/*
 * @test /nodynamiccopyright/
 * @bug 8250625
 * @summary Verify pattern matching test which is always true produces an error
 * @compile/fail/ref=NoSubtypeCheck.out -XDrawDiagnostics NoSubtypeCheck.java
 */
public class NoSubtypeCheck {

    public static void main(Object o, String s, List<String> l) {
        boolean b1 = o instanceof Object v1;
        boolean b2 = o instanceof String v2;
        boolean b3 = s instanceof Object v3;
        boolean b4 = s instanceof String v4;
        boolean b5 = l instanceof List<String> v5;
        boolean b6 = l instanceof List2<String> v6;
        boolean b7 = undef instanceof String v7;
        boolean b8 = o instanceof Undef v7;
    }

    public interface List<T> {}
    public interface List2<T> extends List<T> {}
}
