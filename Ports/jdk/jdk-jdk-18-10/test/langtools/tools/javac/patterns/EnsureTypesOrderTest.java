/*
 * @test /nodynamiccopyright/
 * @bug 8187420 8231827
 * @summary Error message mentions relevant types transposed
 * @compile/fail/ref=EnsureTypesOrderTest.out -XDrawDiagnostics EnsureTypesOrderTest.java
 */
public class EnsureTypesOrderTest {
    public static void main(String [] args) {
        if (args instanceof String s) {
            System.out.println("Broken");
        }
    }
}
