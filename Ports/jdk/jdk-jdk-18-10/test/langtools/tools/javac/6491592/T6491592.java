/*
 * @test /nodynamiccopyright/
 * @bug     6491592
 * @summary Compiler crashes on assignment operator
 * @author  alex.buckley@...
 * @compile/fail/ref=T6491592.out -XDrawDiagnostics T6491592.java
 */

public class T6491592 {
    public static void main(String... args) {
        Object o = null;
        o += null;
    }
}
