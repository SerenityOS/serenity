/*
 * @test /nodynamiccopyright/
 * @bug 4851039
 * @summary explicit type arguments
 * @author gafter
 *
 * @compile/fail/ref=Newarg2.out -XDrawDiagnostics Newarg2.java
 */

// Test type mismatch on type argument for inner constructor

class T {

    class U<Y> extends T {
        <B> U(B b) {}
    }

    public static void main(String[] args) {
        new T().new <Integer>U<Integer>("");
    }
}
