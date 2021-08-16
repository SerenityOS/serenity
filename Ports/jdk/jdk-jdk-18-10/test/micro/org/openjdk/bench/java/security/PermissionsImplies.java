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
package org.openjdk.bench.java.security;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.security.Permissions;
import java.security.UnresolvedPermission;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring Permissions.implies
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Thread)
public class PermissionsImplies {

    private Permissions withPermission = new Permissions();
    private Permissions withoutPermission = new Permissions();
    private Permissions withUnresolvedPermission = new Permissions();

    private RuntimePermission permission = new RuntimePermission("exitVM");

    @Setup
    public void setup() {
        withPermission.add(permission);
        withUnresolvedPermission.add(permission);
        withUnresolvedPermission.add(new UnresolvedPermission("java.lang.FilePermission", "foo", "write", null));
    }

    @Benchmark
    public boolean withoutPermission() {
        return withoutPermission.implies(permission);
    }

    @Benchmark
    public boolean withPermission() {
        return withPermission.implies(permission);
    }

    @Benchmark
    public boolean withUnresolvedPermission() {
        return withUnresolvedPermission.implies(permission);
    }
}
