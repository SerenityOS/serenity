/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Check behavior for invalid breaks.
 * @compile/fail/ref=ExpressionSwitchBreaks2.out -XDrawDiagnostics ExpressionSwitchBreaks2.java
 */

public class ExpressionSwitchBreaks2 {
    private String print(int i, int j) {
        LOOP: while (true) {
        OUTER: switch (i) {
            case 0:
                return switch (j) {
                    case 0:
                        yield "0-0";
                    case 1:
                        break ; //error: missing value
                    case 2:
                        break OUTER; //error: jumping outside of the switch expression
                    case 3: {
                        int x = -1;
                        x: switch (i + j) {
                            case 0: break x;
                        }
                        yield "X";
                    }
                    case 4: return "X"; //error: no returns from inside of the switch expression
                    case 5: continue;   //error: no continue out of the switch expression
                    case 6: continue LOOP; //error: dtto, but with a label
                    case 7: continue UNKNOWN; //error: unknown label
                    default: {
                        String x = "X";
                        x: switch (i + j) {
                            case 0: yield ""; //error: cannot yield from switch expression that is not immediatelly enclosing
                        }
                        yield "X";
                    }
                };
            case 1:
                yield "1" + undef; //error: complex value and no switch expression
        }
        }
        j: print(switch (i) {
            case 0: yield 0;
            default: break j;
        }, 0);
        j2: print(switch (i) {
            case 0: yield 0;
            default: break j2;
        }, 0);
        return null;
    }

}
