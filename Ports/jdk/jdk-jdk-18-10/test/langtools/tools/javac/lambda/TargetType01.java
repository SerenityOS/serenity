/*
 * @test /nodynamiccopyright/
 * @bug 8003280 8009131
 * @summary Add lambda tests
 *  check nested case of overload resolution and lambda parameter inference
 * @compile/fail/ref=TargetType01.out -XDrawDiagnostics TargetType01.java
 */

class TargetType01 {

    interface Func<A,B> {
        B call(A a);
    }

    interface F_I_I extends Func<Integer,Integer> {}
    interface F_S_S extends Func<String,String> {}

    static Integer M(F_I_I f){ return null; }
    static String M(F_S_S f){ return null; }

    static {
        M(x1 -> {
            return M( x2 -> {
                return x1 + x2;
            });
        }); //ambiguous
    }
}
