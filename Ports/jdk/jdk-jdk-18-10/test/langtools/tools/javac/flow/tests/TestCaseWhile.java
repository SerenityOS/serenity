/* /nodynamiccopyright/ */

public class TestCaseWhile {

    @AliveRange(varName="o", bytecodeStart=9, bytecodeLength=8)
    @AliveRange(varName="o", bytecodeStart=20, bytecodeLength=1)
    void m(String[] args) {
        Object o;
        while (args[0] != null) {
            o = "";
            o.hashCode();
        }
        o = "";
    }
}
