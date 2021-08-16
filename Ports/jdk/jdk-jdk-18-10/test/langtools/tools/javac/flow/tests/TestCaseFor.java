/* /nodynamiccopyright/ */

public class TestCaseFor {

    @AliveRange(varName="o", bytecodeStart=10, bytecodeLength=11)
    @AliveRange(varName="o", bytecodeStart=24, bytecodeLength=1)
    void m1(String[] args) {
        Object o;
        for (int i = 0; i < 5; i++) {
            o = "";
            o.hashCode();
        }
        o = "";
    }

    @AliveRange(varName="o", bytecodeStart=10, bytecodeLength=11)
    @AliveRange(varName="o", bytecodeStart=24, bytecodeLength=1)
    void m2(String[] args) {
        Object o;
        for (int i = 0; i < 5; i++) {
            o = "";
            o.hashCode();
            continue;
        }
        o = "";
    }
}
