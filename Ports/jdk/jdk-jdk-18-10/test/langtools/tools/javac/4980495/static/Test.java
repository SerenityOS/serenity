/*
 * @test  /nodynamiccopyright/
 * @bug 4980495 6260444
 * @compile/fail/ref=Test.out -XDrawDiagnostics Test.java p1/A1.java p2/A2.java
 */

package p;

import static p1.A1.f;
import static p2.A2.f;

public class Test {

    public static void main(String argv[]) {
        f = 1;
    }
}
