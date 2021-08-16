/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008723
 * @summary Graph Inference: bad graph calculation leads to assertion error
 * @compile TargetType65.java
 */
class TargetType65 {
    interface Predicate<X> {
        boolean accepts(X x);
    }

    static class Optional<T> {
        public boolean isPresent() { return false; }
        public static<E> Optional<E> empty() { return null; }
    }

    interface Supplier<X> {
        X make();
    }

    static class Sink<O, T> { }

    static class SubSink<R> extends Sink<R, Optional<R>> {  }

    static class Tester<T, O> {
        public static <F> Tester<F, Optional<F>> makeRef() {
            return new Tester<>(Optional.empty(), Optional::isPresent, SubSink::new);
        }

        private Tester(O emptyValue,
                       Predicate<O> presentPredicate,
                       Supplier<Sink<T, O>> sinkSupplier) {
        }
    }
}
