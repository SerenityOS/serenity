/* /nodynamiccopyright/ */

public class TestCaseConditional {

    @AliveRange(varName="o", bytecodeStart=5, bytecodeLength=33)
    @AliveRange(varName="oo", bytecodeStart=23, bytecodeLength=15)
    void m(String[] args) {
        Boolean o;
        Boolean oo = ((o = Boolean.TRUE).booleanValue()) ?
                o = Boolean.TRUE :
                Boolean.FALSE;
        oo.hashCode();
        o = Boolean.FALSE;
        o.hashCode();
    }
}
