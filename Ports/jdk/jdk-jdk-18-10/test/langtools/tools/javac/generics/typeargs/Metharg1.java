/*
 * @test /nodynamiccopyright/
 * @bug 4851039
 * @summary explicit type arguments
 * @author gafter
 *
 * @compile/fail/ref=Metharg1.out -XDrawDiagnostics Metharg1.java
 */

// Test type mismatch on type argument for method call

class T<X> {

    class U<Y> extends T<X> {
        <B> U() {
            <Object>super();
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

    <A> T() {
    }

    <K> void f(K k) {
        this.<Integer>f("");
    }

    public static void main(String[] args) {
        T<Integer> x = new <Object>T<Integer>();
        T<Integer>.U<Float> y = x.new <Object>U<Float>();
        x.<Object>f("");
    }
}
