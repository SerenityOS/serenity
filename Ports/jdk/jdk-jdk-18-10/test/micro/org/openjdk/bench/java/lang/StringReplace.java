/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class StringReplace {

    public String slat1 = new String("java.lang.String");
    public String sutf16 = new String("\u1D36\u1D43\u1D5B\u1D43 \uFF11\uFF13");

    @Benchmark
    public String replace0x1_1_Latin1() {
        return slat1.replace("Z", "z");
    }

    @Benchmark
    public String replace1x1_0_Latin1() {
        return slat1.replace("g", "");
    }

    @Benchmark
    public String replace1x1_1_Latin1() {
        return slat1.replace("g", "k");
    }

    @Benchmark
    public String replace1x1_2_Latin1() {
        return slat1.replace("g", "th");
    }

    @Benchmark
    public String replace2x1_0_Latin1() {
        return slat1.replace(".", "");
    }

    @Benchmark
    public String replace2x1_1_Latin1() {
        return slat1.replace(".", "/");
    }

    @Benchmark
    public String replace2x1_2_Latin1() {
        return slat1.replace(".", "::");
    }

    @Benchmark
    public String replace0x1_1_UTF16() {
        return sutf16.replace("\u1D36", "\u1D38\u1D3A");
    }

    @Benchmark
    public String replace1x1_0_UTF16() {
        return sutf16.replace("\uFF11", "");
    }

    @Benchmark
    public String replace1x1_1_UTF16() {
        return sutf16.replace("\u1D36", "\u24BF");
    }

    @Benchmark
    public String replace1x1_2_UTF16() {
        return sutf16.replace("\uFF11", "\uFF11\uFF12");
    }

    @Benchmark
    public String replace2x1_0_UTF16() {
        return sutf16.replace("\u1D43", "");
    }

    @Benchmark
    public String replace2x1_1_UTF16() {
        return sutf16.replace("\u1D43", "\u1D2C");
    }

    @Benchmark
    public String replace2x1_2_UTF16() {
        return sutf16.replace("\u1D43", "\u21DB\u21DB");
    }
}
