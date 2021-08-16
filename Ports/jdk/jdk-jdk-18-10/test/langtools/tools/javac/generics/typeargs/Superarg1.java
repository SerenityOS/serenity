/*
 * @test /nodynamiccopyright/
 * @bug 4851039
 * @summary explicit type arguments
 * @author gafter
 *
 * @compile/fail/ref=Superarg1.out -XDrawDiagnostics Superarg1.java
 */

// Test type mismatch on type argument for super constructor

class T<X> {

    class U<Y> extends T<X> {
        <B> U() {
            <Integer>super("");
        }
        U(int i) {
            <Object>this();
        }
    }

    class V<Z> extends U<Z> {
        <C> V(T<X> t) {
            t.<Object>super();
        }
    }

    <A> T(A a) {
    }

    <K> void f() {
        this.<Object>f();
    }

    public static void main(String[] args) {
        T<Integer> x = new <Object>T<Integer>("");
        T<Integer>.U<Float> y = x.new <Object>U<Float>();
        x.<Object>f();
    }
}
