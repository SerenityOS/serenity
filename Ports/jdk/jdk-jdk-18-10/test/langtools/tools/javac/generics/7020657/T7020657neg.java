/*
 * @test /nodynamiccopyright/
 * @bug 7020657 6985719
 *
 * @summary  Javac rejects a fairly common idiom with raw override and interfaces
 * @author Maurizio Cimadamore
 * @compile/fail/ref=T7020657neg.out -XDrawDiagnostics T7020657neg.java
 *
 */

import java.util.*;

class T7020657neg {
    interface A {
        int get(List<String> l);
    }

    interface B  {
        int get(List<Integer> l);
    }

    interface C extends A, B { }
}
