/*
 * @test /nodynamiccopyright/
 * @bug 8206986 8254286
 * @summary Check types inferred for switch expressions.
 * @compile/fail/ref=ExpressionSwitchInfer.out -XDrawDiagnostics ExpressionSwitchInfer.java
 */

import java.util.ArrayList;
import java.util.List;

public class ExpressionSwitchInfer {

    private static final String NULL = "null";

    private <T> T test(List<T> l, Class<T> c, String param) {
        test(param == NULL ? new ArrayList<>() : new ArrayList<>(), CharSequence.class, param).charAt(0);
        test(param == NULL ? new ArrayList<>() : new ArrayList<>(), CharSequence.class, param).substring(0);

        test(switch (param) {
            case NULL -> new ArrayList<>();
            default -> new ArrayList<>();
        }, CharSequence.class, param).charAt(0);
        test(switch (param) {
            case NULL -> new ArrayList<>();
            default -> new ArrayList<>();
        }, CharSequence.class, param).substring(0);

        String str = switch (param) {
            case "" -> {
                yield 0;
            } default ->"default";
        };

        return null;
    }

    void bug8254286(I1 i1, I2 i2, int s) {
        var t1 = switch (s) {
            case 1 -> i1;
            case 2 -> null;
            default -> i2;
        };
        t1.m();
        var t2 = switch (s) {
            case 2 -> null;
            case 1 -> i1;
            default -> i2;
        };
        t2.m();
        var t3 = switch (s) {
            case 1 -> i1;
            default -> i2;
            case 2 -> null;
        };
        t3.m();
        var t4 = switch (s) {
            case 1 -> i1;
            default -> null;
        };
        t4.m();
        var t5 = switch (s) {
            default -> null;
            case 1 -> i1;
        };
        t5.m();
        var t6 = switch (s) {
            default -> null;
        };
        var t7 = switch (s) {
            case 1 -> null;
            default -> null;
        };
        var t8 = switch (s) {
            case 1 -> null;
            case 2 -> null;
            default -> null;
        };
    }

    interface I {
        void m();
    }
    interface I1 extends I {}
    interface I2 extends I {}

}
