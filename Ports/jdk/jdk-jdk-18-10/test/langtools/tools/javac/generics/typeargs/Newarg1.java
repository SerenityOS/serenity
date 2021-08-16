/*
 * @test /nodynamiccopyright/
 * @bug 4851039
 * @summary explicit type arguments
 * @author gafter
 *
 * @compile/fail/ref=Newarg1.out -XDrawDiagnostics Newarg1.java
 */

// Test type mismatch on type argument for constructor

class T<X> {

    <K> T(K x) {
    }

    public static void main(String[] args) {
        new <Integer>T<Float>("");
    }
}
