/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This benchmark naively explores String::compareToIgnoreCase performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class StringCompareToIgnoreCase {

    public String upper = new String("\u0100\u0102\u0104\u0106\u0108");
    public String upperLower = new String("\u0100\u0102\u0104\u0106\u0109");
    public String lower = new String("\u0101\u0103\u0105\u0107\u0109");
    public String supUpper = new String("\ud801\udc00\ud801\udc01\ud801\udc02\ud801\udc03\ud801\udc04");
    public String supUpperLower = new String("\ud801\udc00\ud801\udc01\ud801\udc02\ud801\udc03\ud801\udc2c");
    public String supLower = new String("\ud801\udc28\ud801\udc29\ud801\udc2a\ud801\udc2b\ud801\udc2c");

    @Benchmark
    public int upperLower() {
        return upper.compareToIgnoreCase(upperLower);
    }

    @Benchmark
    public int lower() {
        return upper.compareToIgnoreCase(lower);
    }

    @Benchmark
    public int supUpperLower() {
        return supUpper.compareToIgnoreCase(supUpperLower);
    }

    @Benchmark
    public int supLower() {
        return supUpper.compareToIgnoreCase(supLower);
    }
}
