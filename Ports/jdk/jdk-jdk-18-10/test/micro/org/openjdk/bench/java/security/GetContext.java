/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring AccessController.getContext
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public abstract class GetContext {

    public static class Top extends GetContext {

        @SuppressWarnings("removal")
        @Benchmark
        public AccessControlContext testNonPriv() {
            return AccessController.getContext();
        }

        @SuppressWarnings("removal")
        @Benchmark
        public AccessControlContext testPriv() {
            PrivilegedAction<AccessControlContext> pa = () -> AccessController.getContext();
            return AccessController.doPrivileged(pa);
        }
    }

    public static class Deep extends GetContext {

        @Param({"2", "50"})
        int depth;

        @SuppressWarnings("removal")
        private AccessControlContext recurse(int depth) {
            if (depth > 0) {
                return recurse(depth - 1);
            } else {
                return AccessController.getContext();
            }
        }

        @SuppressWarnings("removal")
        @Benchmark
        public AccessControlContext testNonPrivRecurse() {
            return recurse(depth);
        }

        @SuppressWarnings("removal")
        @Benchmark
        public AccessControlContext testPrivInline() {
            PrivilegedAction<AccessControlContext> pa = () -> recurse(depth);
            return AccessController.doPrivileged(pa);
        }
    }
}
