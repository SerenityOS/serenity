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

package org.openjdk.bench.java.net;

import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodType.methodType;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, jvmArgsAppend = "--add-exports=java.base/sun.net.www=ALL-UNNAMED")
public class ThreadLocalParseUtil {

    private static final MethodHandle MH_DECODE;
    private static final MethodHandle MH_TO_URI;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Class<?> c = Class.forName("sun.net.www.ParseUtil");
            MH_DECODE = lookup.findStatic(c, "decode", methodType(String.class, String.class));
            MH_TO_URI = lookup.findStatic(c, "toURI", methodType(URI.class, URL.class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Benchmark
    public String decodeTest() throws Throwable {
        return (String) MH_DECODE.invokeExact("/xyz/\u00A0\u00A0");
    }

    @Benchmark
    public URI appendEncodedTest() throws Throwable {
        URL url = new URL("https://example.com/xyz/abc/def?query=#30");
        return (URI) MH_TO_URI.invokeExact(url);
    }
}
