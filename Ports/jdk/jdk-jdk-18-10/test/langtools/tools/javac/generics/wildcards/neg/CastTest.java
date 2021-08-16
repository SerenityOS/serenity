/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4916607
 * @summary Test casts (legal)
 * @author gafter
 *
 * @compile CastTest.java
 */

import java.util.*;

class CastTest {

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

        o = (AB<String>) (AA<String>) null; // <<pass>>
        o = (AC<String>) (AA<Vector<String>>) null; // <<pass>>
        o = (AD<Number>) (AA<Vector<? extends Number>>) null; // <<pass>>
        o = (AD<?>) (AA<Vector<? extends Object>>) null; // <<pass>>
        o = (AD<Object>) (AA<Vector<?>>) null; // <<pass>>
        o = (AE<String>) (AA<Vector<? super String>>) null; // <<pass>>
        o = (AF<String>) (AA<String[]>) null; // <<pass>>
        o = (AG<?>) (AA<String>) null; // <<pass>>
    }

    // --- Inconsistent matches ---

    private class BA<T> { }
    private class BB<T, S> { }

    private class BC<T> extends BA<Integer> { }
    private class BD<T> extends BB<T, T> { }

    private void inconsistentMatches() {
        Object o;

        o = (BC<?>) (BA<Integer>) null; // <<pass>>
        o = (BD<String>) (BB<String, String>) null; // <<pass>>
    }

    private void whyMustEverythingBeSo_______Complicated() {
        // This has to work...
        BD<Number> bd = new BD<Number>();
        BB<? extends Number, ? super Integer> bb = bd;
        // 4916620: wildcards: legal cast is rejected
        // bd = (BD<Number>) bb; // <<warn>> <<todo: cast-infer>>
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
        o = (CE<String>) (CD<String>) null; // <<pass>>

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
        o = (DA<? extends Number>) (DA<Integer>) null; // <<pass>>
        o = (DA<? super Integer>) (DA<Number>) null; // <<pass>>
        o = (DA<?>) (DA<Integer>) null; // <<pass>>
        o = (DA<?>) (DA<? extends Integer>) null; // <<pass>>
        o = (DA<?>) (DA<? super String>) null; // <<pass>>
        o = (DA<?>) (DA<?>) null; // <<pass>>

        // Typevars
        o = (DA<? extends Number>) (DA<I>) null; // <<pass>>

        // Raw (asymmetrical!)
        o = (DA) (DB<Number>) null; // <<pass>>
        o = (DA<?>) (DB) null; // <<pass>>
        o = (DA<? extends Object>) (DB) null; // <<pass>>
        o = (DB) (DA<Number>) null; // <<pass>>
        o = (DB<?>) (DA) null; // <<pass>>
        o = (DB<? extends Object>) (DA) null; // <<pass>>
        o = (DC<?>) (DA<?>) null; // <<pass>>
    }

}
