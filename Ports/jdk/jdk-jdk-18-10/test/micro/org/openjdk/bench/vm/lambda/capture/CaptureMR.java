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
package org.openjdk.bench.vm.lambda.capture;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

/**
 * evaluates method reference capture
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CaptureMR {

    public static class Mock0 {
        public Mock0() {
        }
    }

    public static Object method_static() {
        return "42";
    }

    public Object method_instance() {
        return "42";
    }

    @Benchmark()
    public FunctionalInterface0 mref_static0(){
        return CaptureMR::method_static;
    }

    @Benchmark()
    public FunctionalInterface0 mref_bound0(){
        return this::method_instance;
    }

    @Benchmark()
    public FunctionalInterface0 mref_constructor0(){
        return Mock0::new;
    }


//---------------------------

    public static class Mock1 {
        private Object oo;
        public Mock1(Object o) {
            oo = o;
        }
    }

    public static Object method_static(Object bar) {
        return "42" + bar;
    }

    public Object method_instance(Object bar) {
        return "42" + bar;
    }

    @Benchmark()
    public FunctionalInterface1 mref_static1(){
        return CaptureMR::method_static;
    }

    @Benchmark()
    public FunctionalInterface1 mref_bound1(){
        return this::method_instance;
    }

    @Benchmark()
    public FunctionalInterface1 mref_constructor1(){
        return Mock1::new;
    }

}
