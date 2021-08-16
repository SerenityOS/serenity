/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify that not allowed types of statements cannot be used in rule case.
 * @compile/fail/ref=SwitchStatementBroken2.out -XDrawDiagnostics SwitchStatementBroken2.java
 */

public class SwitchStatementBroken2 {

    private void statementArrowNotArbitraryStatements(int i, int j) {
        String res;

        switch (i) {
            case 0 -> res = "NULL-A";
            case 1 -> { res = "NULL-A"; }
            case 2 -> throw new IllegalStateException();
            case 3 -> if  (j < 0) res = "A"; else res = "B";
            case 4 -> while (j-- > 0) res += "I";
            case 5 -> switch (j) {
                case 0: res = "0"; break;
            }
            case 7 -> int i;
            default -> if  (j < 0) res = "A"; else res = "B";
        }
    }

}
