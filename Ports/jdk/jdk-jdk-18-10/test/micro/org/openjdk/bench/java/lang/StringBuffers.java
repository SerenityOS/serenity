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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StringBuffers {

    private String name;
    private String blaha;
    private Sigurd sig;

    @Setup
    public void setup() {
        name = "joe";
        blaha = "sniglogigloienlitenapasomarengrodasjukadjavelhej";
        sig = new Sigurd();
    }

    @Benchmark
    public String appendAndToString() {
        return "MyStringBuffer named:" + ((name == null) ? "unknown" : name) + ".";
    }

    @Benchmark
    public String toStringComplex() {
        return sig.toString();
    }

    static class Sigurd {
        int x;
        byte y;
        String z = "yahoo";

        @Override
        public String toString() {
            return Integer.toString(x) + "_" + Integer.toString((int) y) + "_" + z + "_";
        }
    }

    @Benchmark
    public String substring() {
        return blaha.substring(30, 35);
    }

}
