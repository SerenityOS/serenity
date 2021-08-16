/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test accessing non-static variable from lambda expressions in static context
 * @compile/fail/ref=AccessNonStatic_neg.out -XDrawDiagnostics AccessNonStatic_neg.java
 */

public class AccessNonStatic_neg {

    private int n = 0;

    static {
        ((Runnable) ()-> {
            System.out.println(this);
            System.out.println(n);
        }).run();
    }

    public static void test() {
        ((Runnable) ()-> {
            Object o = this;
            n++;
        }).run();
    }
}
