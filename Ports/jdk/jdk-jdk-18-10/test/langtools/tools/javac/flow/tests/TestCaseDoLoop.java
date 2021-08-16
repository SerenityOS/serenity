/* /nodynamiccopyright/ */

public class TestCaseDoLoop {

    @AliveRange(varName="o", bytecodeStart=3, bytecodeLength=15)
    @AliveRange(varName="args", bytecodeStart=0, bytecodeLength=18)
    void m(String[] args) {
        Object o;
        do {
            o = "";
            o.hashCode();
        } while (args[0] != null);
        o = "";
    }
}
