/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is for identifying a non-SAM type 6: An interface that has a single abstract method, which is also public method in Object
 * @compile/fail/ref=NonSAM1.out -XDrawDiagnostics NonSAM1.java Helper.java
 */

public class NonSAM1 {
    void method() {
        Planet n = (Object o) -> true;
        System.out.println("never reach here " + n);
    }
}
