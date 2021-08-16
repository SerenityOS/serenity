/**
 * @test /nodynamiccopyright/
 * @bug 8266027
 * @summary Verify the diamond finder works on fields with modifiers.
 * @compile/ref=DiamondFields.out -XDfind=diamond -XDrawDiagnostics DiamondFields.java
 */

import java.util.LinkedList;
import java.util.List;

public class DiamondFields {
                List<String> f1 = new LinkedList<String>();
    private     List<String> f2 = new LinkedList<String>();
    static      List<String> f3 = new LinkedList<String>();
    @Deprecated List<String> f4 = new LinkedList<String>();
    final       List<String> f5 = new LinkedList<String>();

    DiamondFields() {
        List<String> l1 = new LinkedList<String>();
        final List<String> l2 = new LinkedList<String>();
        @Deprecated List<String> l3 = new LinkedList<String>();
    }

    void t() {
        List<String> l1 = new LinkedList<String>();
        final List<String> l2 = new LinkedList<String>();
        @Deprecated List<String> l3 = new LinkedList<String>();
    }
}
