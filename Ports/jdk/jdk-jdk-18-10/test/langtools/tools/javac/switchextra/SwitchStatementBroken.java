/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify that rule and ordinary cases cannot be mixed.
 * @compile/fail/ref=SwitchStatementBroken.out -XDrawDiagnostics SwitchStatementBroken.java
 */

public class SwitchStatementBroken {

    private void statementBroken(int i) {
        String res;

        switch (i) {
            case 0 -> { res = "NULL-A"; }
            case 1: { res = "NULL-A"; break; }
            case 2: { res = "NULL-A"; break; }
            default -> { res = "NULL-A"; break; }
        }
    }

}
