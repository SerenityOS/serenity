/*
 * Copyright (c) 2021, Huawei Technologies Co., Ltd. All rights reserved.
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
package org.openjdk.bench.java.security;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import sun.security.util.DisabledAlgorithmConstraints;

import java.security.AlgorithmConstraints;
import java.security.CryptoPrimitive;
import java.util.concurrent.TimeUnit;
import java.util.EnumSet;
import java.util.Set;

import static sun.security.util.DisabledAlgorithmConstraints.PROPERTY_TLS_DISABLED_ALGS;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(jvmArgsAppend = {"--add-exports", "java.base/sun.security.util=ALL-UNNAMED"})
@State(Scope.Thread)
public class AlgorithmConstraintsPermits {

    AlgorithmConstraints tlsDisabledAlgConstraints;
    Set<CryptoPrimitive> primitives = EnumSet.of(CryptoPrimitive.KEY_AGREEMENT);

    @Param({"SSLv3", "DES", "NULL", "TLS1.3"})
    String algorithm;

    @Setup
    public void setup() {
        tlsDisabledAlgConstraints = new DisabledAlgorithmConstraints(PROPERTY_TLS_DISABLED_ALGS);
    }

    @Benchmark
    public boolean permits() {
        return tlsDisabledAlgConstraints.permits(primitives, algorithm, null);
    }
}

