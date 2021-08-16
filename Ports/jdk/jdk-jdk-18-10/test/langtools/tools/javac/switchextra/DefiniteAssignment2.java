/**
 * @test /nodynamiccopyright/
 * @summary Verify that definite assignment works (illegal code)
 * @compile/fail/ref=DefiniteAssignment2.out -XDrawDiagnostics DefiniteAssignment2.java
 */
public class DefiniteAssignment2 {

    public static void main(String[] args) {
        int a = 0;
        E e = E.A;

        {
        int x;

        switch(a) {
            case 0: break;
            default: x = 1; break;
        }

        System.err.println(x);
        }

        {
        int x;

        switch(a) {
            case 0 -> {}
            default -> x = 1;
        }

        System.err.println(x);
        }

        {
        int x;

        switch(a) {
            case 0: x = 0; break;
            default:
        }

        System.err.println(x);
        }

        {
        int x;

        switch(e) {
            case A, B, C -> x = 0;
        }

        System.err.println(x);
        }

        {
        int x;

        switch(e) {
            case A, B, C -> { x = 0; }
        }

        System.err.println(x);
        }

        {
        int x;

        switch(e) {
            case A, B -> { x = 0; }
            case C -> throw new IllegalStateException();
        }

        System.err.println(x);
        }
    }

    enum E {
        A, B, C;
    }
}
