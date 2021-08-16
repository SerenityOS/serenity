/**
 * @test  /nodynamiccopyright/
 * @bug     6359951
 * @summary Crash when using class field
 *
 *
 * @compile/fail/ref=T6359951.out -XDrawDiagnostics T6359951.java
 */

public class T6359951 {
    public static String x;
    public static class C {}
    class D<T> { }
    class E<T> extends D<T.classOfT> {}
    class F<T extends T6359951> extends D<T.x> {}
    class G<T extends T6359951> extends D<T.C> {}
}
