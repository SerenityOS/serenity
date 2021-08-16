/*
 * @test  /nodynamiccopyright/
 * @bug     5024091
 * @summary AssertionError shouldn't be thrown
 * @author  Wei Tao
 * @compile/fail/ref=T5024091.out -XDfailcomplete=java.lang.StringBuilder -XDdev -XDrawDiagnostics -XDstringConcat=inline T5024091.java
 */

public class T5024091 {
    private final String[] stringArray = {"s", "t", "r"};
    public void foo() {
        String str = "S = " + stringArray[0];
    }
}
