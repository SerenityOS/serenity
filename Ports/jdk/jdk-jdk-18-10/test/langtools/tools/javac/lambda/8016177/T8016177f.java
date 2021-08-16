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
 * @bug 8016081 8016178
 * @summary structural most specific and stuckness
 * @compile T8016177f.java
 */
import java.util.*;

class T8016177f {

    interface Function<S, T> {
       T apply(S s);
    }

    interface IntFunction<T> {
       T apply(int s);
    }


    interface BiConsumer<X,Y> {
       void m(X x, Y y);
    }

    interface Consumer<X> {
       void m(X x);
    }

    interface Supplier<X> {
       X make();
    }

    interface TestData<T, S extends BaseStream<T, S>> {
       interface OfRef<T> extends TestData<T, Stream<T>> { }
       interface OfDouble extends TestData<Double, DoubleStream> { }
    }

    interface BaseStream<T, S extends BaseStream<T, S>> { }

    interface Stream<T> extends BaseStream<T, Stream<T>> {
       <M> Stream<M> map(Function<T, M> s);
       <R> R collect(Supplier<R> resultFactory, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);
       <Z> Z[] toArray(IntFunction<Z[]> s);
    }

    interface DoubleStream extends BaseStream<Double, DoubleStream> {
       DoubleStream filter(DoublePredicate dp);
       double[] toArray();
    }

    interface DoublePredicate {
       boolean p(double d);
    }

    <T, U, R, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>>
           R exerciseTerminalOps(TestData<T, S_IN> data,
                                 Function<S_IN, S_OUT> streamF,
                                 Function<S_OUT, R> terminalF) { return null; }

    <O> TestData.OfRef<O> ofCollection(Collection<O> collection) { return null; }

    void test1(TestData.OfDouble data, DoublePredicate dp) {
        exerciseTerminalOps(data, s -> s.filter(dp), s -> s.toArray());
    }

    void test2(Function<Double, Integer> fdi, TestData.OfRef<Double> td, Stream<Integer> si) {
        exerciseTerminalOps(
                        ofCollection((List<Double>)null),
                        s -> s.map(fdi),
                        s -> s.toArray(Integer[]::new));
    }
}
