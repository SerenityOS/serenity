/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring java.lang.Class.newInstance speed.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class NewInstance {

    public Class<?>[] samePublicClasses;
    public Class<?>[] differentPublicClasses;
    public Class<?>[] differentPublicClassesConstant;
    public Class<?>[] sameProtectedClasses;
    public Class<?>[] differentProtectedClasses;

    @Setup
    public void setup() {
        samePublicClasses = new Class<?>[]{Apub.class, Apub.class, Apub.class};
        differentPublicClasses = new Class<?>[]{Apub.class, Bpub.class, Cpub.class};
        differentPublicClassesConstant = new Class<?>[]{Apub.class, Bpub.class, Cpub.class};
        sameProtectedClasses = new Class<?>[]{Apro.class, Apro.class, Apro.class};
        differentProtectedClasses = new Class<?>[]{Apro.class, Bpro.class, Cpro.class};
    }

    /**
     * Performs Class.newInstance on the same class over and over again. That it is the same class is not provable at
     * compile time. The class is protected.
     */
    @SuppressWarnings("deprecation")
    @Benchmark
    public void threeSameProtected(Blackhole bh) throws IllegalAccessException, InstantiationException {
        for (Class<?> cl : sameProtectedClasses) {
            bh.consume(cl.newInstance());
        }
    }

    /**
     * Performs Class.newInstance on three different classes, just allocating one instance of one class at a time. The
     * classes are all protected.
     */
    @SuppressWarnings("deprecation")
    @Benchmark
    public void threeDifferentProtected(Blackhole bh) throws IllegalAccessException, InstantiationException {
        for (Class<?> cl : differentProtectedClasses) {
            bh.consume(cl.newInstance());
        }
    }

    /**
     * Performs Class.newInstance on the same class over and over again. That it is the same class is not provable at
     * compile time. The class is public.
     */
    @SuppressWarnings("deprecation")
    @Benchmark
    public void threeSamePublic(Blackhole bh) throws IllegalAccessException, InstantiationException {
        for (Class<?> cl : samePublicClasses) {
            bh.consume(cl.newInstance());
        }
    }

    /**
     * Performs Class.newInstance on three different classes, just allocating one instance of one class at a time. The
     * classes are all public.
     */
    @SuppressWarnings("deprecation")
    @Benchmark
    public void threeDifferentPublic(Blackhole bh) throws IllegalAccessException, InstantiationException {
        for (Class<?> cl : differentPublicClasses) {
            bh.consume(cl.newInstance());
        }
    }

    /**
     * Performs Class.newInstance on three different classes, just allocating one instance of one class at a time. The
     * classes are all public.
     */
    @SuppressWarnings("deprecation")
    @Benchmark
    public void threeDifferentPublicConstant(Blackhole bh) throws IllegalAccessException, InstantiationException {
        bh.consume(Apub.class.newInstance());
        bh.consume(Bpub.class.newInstance());
        bh.consume(Cpub.class.newInstance());
    }

    @SuppressWarnings("deprecation")
    @Benchmark
    public void threeDifferentPublicFinal(Blackhole bh) throws IllegalAccessException, InstantiationException {
        for (Class<?> cl : differentPublicClassesConstant) {
            bh.consume(cl.newInstance());
        }
    }

    /* Protected test classes */
    static class Apro {}
    static class Bpro {}
    static class Cpro {}

    /* Public test classes */
    public static class Apub {}
    public static class Bpub {}
    public static class Cpub {}

}
