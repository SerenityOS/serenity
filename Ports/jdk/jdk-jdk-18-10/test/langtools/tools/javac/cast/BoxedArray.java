/*
 * @test /nodynamiccopyright/
 * @bug 5014309
 * @summary REGRESSION: compiler allows cast from Integer[] to int[]
 * @author gafter
 *
 * @compile/fail/ref=BoxedArray.out -XDrawDiagnostics  BoxedArray.java
 */

public class BoxedArray {
    int[] a2;
    void f(Integer[] a1) {
        a2 = (int[]) a1;
    }
}
