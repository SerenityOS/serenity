/* /nodynamiccopyright/ */

public class TestCaseIf {

    @AliveRange(varName="o", bytecodeStart=9, bytecodeLength=5)
    @AliveRange(varName="o", bytecodeStart=17, bytecodeLength=1)
    void m0(String[] args) {
        Object o;
        if (args[0] != null) {
            o = "";
            o.hashCode();
        }
        o = "";
    }

    @AliveRange(varName="o", bytecodeStart=10, bytecodeLength=5)
    @AliveRange(varName="o", bytecodeStart=18, bytecodeLength=1)
    void m1() {
        Object o;
        int i = 5;
        if (i == 5) {
            o = "";
            o.hashCode();
        }
        o = "";
    }

    @AliveRange(varName="o", bytecodeStart=10, bytecodeLength=5)
    @AliveRange(varName="o", bytecodeStart=18, bytecodeLength=1)
    void m2() {
        Object o;
        int i = 5;
        if (!(i == 5)) {
            o = "";
            o.hashCode();
        }
        o = "";
    }

    @AliveRange(varName="o", bytecodeStart=15, bytecodeLength=5)
    @AliveRange(varName="o", bytecodeStart=23, bytecodeLength=1)
    void m3(String[] args) {
        Object o;
        if (args[0] != null && args[1] != null) {
            o = "";
            o.hashCode();
        }
        o = "";
    }

    @AliveRange(varName="o", bytecodeStart=15, bytecodeLength=5)
    @AliveRange(varName="o", bytecodeStart=23, bytecodeLength=1)
    void m4(String[] args) {
        Object o;
        if (args[0] != null || args[1] != null) {
            o = "";
            o.hashCode();
        }
        o = "";
    }

    @AliveRange(varName="finalLocal", bytecodeStart=11, bytecodeLength=6)
    @AliveRange(varName="used", bytecodeStart=13, bytecodeLength=4)
    void m5(Object o) {
        if (o != null) {
            Object notUsed;
            Object used;
            if (o != null) {
                final Object finalLocal = null;
                used = null;
                if (o == null) {}
            }
        }
    }
}
