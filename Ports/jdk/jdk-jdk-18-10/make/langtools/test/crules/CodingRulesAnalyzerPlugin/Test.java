/**@test /nodynamiccopyright/
 * @compile/fail/ref=Test.out -Xplugin:coding_rules -XDrawDiagnostics Test.java
 */

import com.sun.tools.javac.util.Assert;

public class Test {

    String v;

    public void check1(String value) {
        Assert.check(value.trim().length() > 0, "value=" + value); //fail
    }
    public void check2(String value) {
        Assert.check(value.trim().length() > 0, "value=" + "value"); //ok
    }
    public void check3(String value) {
        Assert.check(value.trim().length() > 0, () -> "value=" + value); //ok
    }
    public void check4(String value) {
        Assert.check(value.trim().length() > 0, value); //ok
    }
    public void check5(String value) {
        Assert.check(value.trim().length() > 0, v); //ok
    }
    public void check6(String value) {
        Assert.check(value.trim().length() > 0, () -> "value=" + "value"); //fail
    }
    public void check7(String value) {
        Assert.check(value.trim().length() > 0, () -> value); //fail
    }
    public void check8(String value) {
        Assert.check(value.trim().length() > 0, () -> v); //fail
    }
}
