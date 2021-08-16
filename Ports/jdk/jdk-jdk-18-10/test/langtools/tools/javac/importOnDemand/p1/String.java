/* /nodynamiccopyright/ */

package p1;

import p2.*;

public class String {
    public static void test() { }
}

class Test1 {
    private void test() {
        String.test();
        Object.test();
        Boolean.valueOf(true);
    }
}
