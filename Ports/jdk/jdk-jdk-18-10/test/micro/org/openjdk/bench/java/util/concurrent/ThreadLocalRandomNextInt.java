/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util.concurrent;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ThreadLocalRandomNextInt {

    @State(Scope.Benchmark)
    public static class Global {
        public ThreadLocal<Random> tlr;
        private List<ThreadLocal<Integer>> contaminators; // reachable, non-garbage-collectable

        @Setup(Level.Trial)
        public void setup() {
            tlr = new ThreadLocal<Random>() {
                @Override
                protected Random initialValue() {
                    return java.util.concurrent.ThreadLocalRandom.current();
                }
            };

            // contaminate ThreadLocals
            int contaminatorCount = Integer.getInteger("contaminators", 0);
            contaminators = new ArrayList<>(contaminatorCount);
            for (int i = 0; i < contaminatorCount; i++) {
                final int finalI = i;
                ThreadLocal<Integer> tl = new ThreadLocal<Integer>() {
                    @Override
                    protected Integer initialValue() {
                        return finalI;
                    }
                };
                contaminators.add(tl);
                tl.get();
            }
        }
    }

    @State(Scope.Thread)
    public static class Local {
        public java.util.concurrent.ThreadLocalRandom tlr;

        @Setup(Level.Trial)
        public void setup() {
            tlr = java.util.concurrent.ThreadLocalRandom.current();
        }
    }

    @Benchmark
    public int baseline(Local l) {
        return l.tlr.nextInt();
    }

    @Benchmark
    public int testJUC() {
        return java.util.concurrent.ThreadLocalRandom.current().nextInt();
    }

    @Benchmark
    public int testLang(Global g) {
        return g.tlr.get().nextInt();
    }

}
