/*
 * @test /nodynamiccopyright/
 * @bug 5025346
 * @summary Crash on cast
 * @author gafter
 *
 * @compile/fail/ref=CastCrash.out -XDrawDiagnostics  CastCrash.java
 */

package cast.crash;

import java.util.*;

interface SN extends Set<Number>{}
interface LI extends List<Integer>{}

class CastCrash {
    void f(LI l) {
        SN sn = (SN) l;
    }
}
