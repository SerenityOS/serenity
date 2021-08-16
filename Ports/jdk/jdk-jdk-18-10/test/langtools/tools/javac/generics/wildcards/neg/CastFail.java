/*
 * @test /nodynamiccopyright/
 * @bug 4916607
 * @summary Test casts (errors)
 * @author gafter
 *
 * @compile/fail/ref=CastFail.out -XDrawDiagnostics CastFail.java
 */

import java.util.*;

class CastFail {

    // --- Directly transferring parameters ---

    private class AA<T> { }

    private class AB<T> extends AA<T> { }
    private class AC<T> extends AA<Vector<T>> { }
    private class AD<T> extends AA<Vector<? extends T>> { }
    private class AE<T> extends AA<Vector<? super T>> { }
    private class AF<T> extends AA<T[]> { }
    private class AG<T> extends AA<String> { }

    private void parameterTransfer() {
        Object o;

        o = (AB<String>) (AA<Number>) null; // <<fail 1>>
        o = (AC<String>) (AA<Vector<Number>>) null; // <<fail 2>>
        o = (AC<String>) (AA<Stack<String>>) null; // <<fail 3>>
        o = (AD<String>) (AA<Vector<? extends Number>>) null; // <<fail 4>>
        o = (AE<Number>) (AA<Vector<? super String>>) null; // <<fail 5>>
        o = (AF<String>) (AA<Number[]>) null; // <<fail 6>>
        o = (AG<?>) (AA<Number>) null; // <<fail 7>>
    }

    // --- Inconsistent matches ---

    private class BA<T> { }
    private class BB<T, S> { }

    private class BC<T> extends BA<Integer> { }
    private class BD<T> extends BB<T, T> { }

    private void inconsistentMatches() {
        Object o;

        o = (BC<?>) (BA<String>) null; // <<fail 8>>
        o = (BD<String>) (BB<String, Number>) null; // <<fail 9>>
        o = (BD<String>) (BB<Number, String>) null; // <<fail 10>>
    }

    // --- Transferring parameters via supertypes ---

    private interface CA<T> { }
    private interface CB<T> extends CA<T> { }
    private interface CC<T> extends CA<T> { }

    private class CD<T> implements CB<T> { }
    private interface CE<T> extends CC<T> { }

    private interface CF<S> { }
    private interface CG<T> { }
    private class CH<S, T> implements CF<S>, CG<T> { }
    private interface CI<S> extends CF<S> { }
    private interface CJ<T> extends CG<T> { }
    private interface CK<S, T> extends CI<S>, CJ<T> { }

    private void supertypeParameterTransfer() {
        Object o;
        CD<?> cd = (CE<?>) null; // <<fail 11>>
        CE<?> ce = (CD<?>) null; // <<fail 12>>
        o = (CE<Number>) (CD<String>) null; // <<fail 13>>

        // 4916622: unnecessary warning with cast
        // o = (CH<String, Integer>) (CK<String, Integer>) null; // <<pass>> <<todo: cast-infer>>
    }

    // --- Disjoint ---

    private interface DA<T> { }
    private interface DB<T> extends DA<T> { }
    private interface DC<T> extends DA<Integer> { }

    private <N extends Number, I extends Integer, R extends Runnable, S extends String> void disjointness() {
        Object o;

        // Classes
        o = (DA<Number>) (DA<Integer>) null; // <<fail 14>>
        o = (DA<? extends Integer>) (DA<Number>) null; // <<fail 15>>
        o = (DA<? super Number>) (DA<Integer>) null; // <<fail 16>>
        o = (DA<? extends Runnable>) (DA<? extends String>) null; // <<fail 17>>
        o = (DA<? super Number>) (DA<? extends Integer>) null; // <<fail 18>>

        // Typevars
        o = (DA<? extends String>) (DA<I>) null; // <<fail 19>>
        o = (DA<S>) (DA<R>) null; // <<fail 20>>

        // Raw (asymmetrical!)
        o = (DC<?>) (DA<? super String>) null; // <<fail 21>>
    }
}
