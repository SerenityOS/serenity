/*
 * @test /nodynamiccopyright/
 * @bug 8004102
 * @summary Add support for array constructor references
 * @compile/fail/ref=MethodReference60.out -XDrawDiagnostics MethodReference60.java
 */
public class MethodReference60 {

    interface ArrayFactory<X> {
        X make(int size);
    }

    interface BadArrayFactory1<X> {
        X make();
    }

    interface BadArrayFactory2<X> {
        X make(int i1, int i2);
    }

    interface BadArrayFactory3<X> {
        X make(String s);
    }

    public static void main(String[] args) {
        BadArrayFactory1<int[]> factory1 = int[]::new; //param mismatch
        BadArrayFactory2<int[]> factory2 = int[]::new; //param mismatch
        BadArrayFactory3<int[]> factory3 = int[]::new; //param mismatch
        ArrayFactory<Integer> factory4 = int[]::new; //return type mismatch
        ArrayFactory<Integer[]> factory5 = int[]::new; //return type mismatch
    }
}
