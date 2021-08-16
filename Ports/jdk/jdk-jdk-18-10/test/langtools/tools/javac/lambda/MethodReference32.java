/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that varargs warnings are generated during SAM conversion
 * @compile/fail/ref=MethodReference32.out -Xlint:unchecked -Werror -XDrawDiagnostics MethodReference32.java
 */

import java.util.*;

class MethodReference32 {

    interface SAM {
        MethodReference32 m(List<Integer> l1, List<Integer> l2);
    }

    MethodReference32 meth(List<Integer>... lli) { return null; }
    MethodReference32(List<Integer>... lli) { }

    SAM s1 = this::meth;
    SAM s2 = MethodReference32::new;
}
