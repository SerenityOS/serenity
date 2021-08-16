/*
 * @test /nodynamiccopyright/
 * @bug 4951260
 * @summary compiler disallows raw call to generic constructor
 * @author gafter
 *
 * @compile       -Werror                  UncheckedConstructor.java
 * @compile/fail/ref=UncheckedConstructor.out -XDrawDiagnostics  -Werror -Xlint:unchecked UncheckedConstructor.java
 */

import java.util.*;

class G3 {

    G3(Enumeration<Object> e) { }

    static void g() {
        new G3(new Enumeration() {
                public boolean hasMoreElements() { return false; }
                public Object nextElement() { return null; }
            });
    }

}
