/*
 * @test /nodynamiccopyright/
 * @bug 4916607
 * @summary Test casts (warning)
 * @author gafter
 *
 * @compile/ref=CastWarn.out -XDrawDiagnostics -Xlint:unchecked CastWarn.java
 */

import java.util.*;

class CastWarn {

    // --- Disjoint ---

    private interface DA<T> { }
    private interface DB<T> extends DA<T> { }
    private interface DC<T> extends DA<Integer> { }

    private <N extends Number, I extends Integer, R extends Runnable, S extends String> void disjointness() {
        Object o;

        // Classes
        o = (DA<? extends Runnable>) (DA<? extends Number>) null; // <<warn 2>>
        o = (DA<? super Integer>) (DA<? extends Number>) null; // <<warn 3>>
        o = (DA<? super String>) (DA<? super Number>) null; // <<warn 4>>

        // Typevars
        o = (DA<? extends Integer>) (DA<N>) null; // <<warn 5>>
        o = (DA<I>) (DA<? extends Number>) null; // <<warn 6>>
        o = (DA<N>) (DA<? extends Integer>) null; // <<warn 7>>
        o = (DA<N>) (DA<? extends Runnable>) null; // <<warn 8>>

        o = (DA<N>) (DA<I>) null; // <<warn 9>>
        o = (DA<N>) (DA<R>) null; // <<warn 10>>

        // Raw (asymmetrical!)
        o = (DA<Number>) (DB) null; // <<warn 11>>
        o = (DA<? extends Number>) (DB) null; // <<warn 12>>
        o = (DB<Number>) (DA) null; // <<warn 13>>
        o = (DB<? extends Number>) (DA) null; // <<warn 14>>
    }
}
