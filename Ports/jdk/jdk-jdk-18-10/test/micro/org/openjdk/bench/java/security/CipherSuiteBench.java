/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
 *
 */
package org.openjdk.bench.java.security;

import org.openjdk.jmh.annotations.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


@Fork(jvmArgsAppend = {"--add-exports", "java.base/sun.security.ssl=ALL-UNNAMED", "--add-opens", "java.base/sun.security.ssl=ALL-UNNAMED"})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
public class CipherSuiteBench {

    Method nameOf;

    @Param({"TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA" })
    String cipherSuite;

    @Setup
    public void initilizeClass() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> cs = Class.forName("sun.security.ssl.CipherSuite");
        nameOf = cs.getDeclaredMethod("nameOf", String.class);
        nameOf.setAccessible(true);
    }

    @Benchmark
    public Object benchmarkCipherSuite() throws InvocationTargetException, IllegalAccessException {
        return nameOf.invoke(null,cipherSuite);
    }
}
