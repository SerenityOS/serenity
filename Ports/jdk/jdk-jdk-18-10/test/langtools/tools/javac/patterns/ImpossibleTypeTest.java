/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Ensure that in type test patterns, the predicate is not trivially provable false.
 * @compile/fail/ref=ImpossibleTypeTest.out -XDrawDiagnostics ImpossibleTypeTest.java
 */
public class ImpossibleTypeTest {

    public static void main(String[] args) {

        int in = 42;
        Integer i = 42;

        if (i instanceof String s ) {
            System.out.println("Broken");
        }
        if (i instanceof Undefined u ) {
            System.out.println("Broken");
        }
    }
}
