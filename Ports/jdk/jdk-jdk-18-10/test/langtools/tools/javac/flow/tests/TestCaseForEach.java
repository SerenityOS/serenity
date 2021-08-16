/* /nodynamiccopyright/ */

public class TestCaseForEach {

    @AliveRange(varName="o", bytecodeStart=25, bytecodeLength=11)
    @AliveRange(varName="o", bytecodeStart=39, bytecodeLength=1)
    void m(String[] args) {
        Object o;
        for (String s : args) {
            o = "";
            o.hashCode();
        }
        o = "";
    }
}
