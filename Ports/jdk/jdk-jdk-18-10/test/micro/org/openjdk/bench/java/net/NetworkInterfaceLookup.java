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
package org.openjdk.bench.java.net;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.TimeUnit;

/**
 * Assess time to perform native NetworkInterface lookups; uses
 * reflection to access both package-private isBoundInetAddress and
 * public getByInetAddress (to get comparable numbers)
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
public class NetworkInterfaceLookup {

    static final InetAddress address = InetAddress.getLoopbackAddress();

    static final Method isBoundInetAddress_method;

    static final Method getByInetAddress_method;

    static {
        Method isBound = null;
        Method getByInet = null;

        try {
            isBound = NetworkInterface.class.getDeclaredMethod("isBoundInetAddress", InetAddress.class);
            isBound.setAccessible(true);
        } catch (Exception e) {
            System.out.println("NetworkInterface.isBoundInetAddress not found");
        }

        try {
            getByInet = NetworkInterface.class.getDeclaredMethod("getByInetAddress", InetAddress.class);
        } catch (Exception e) {
            System.out.println("NetworkInterface.getByInetAddress not found");
        }
        isBoundInetAddress_method = isBound;
        getByInetAddress_method = getByInet;
    }

    @Benchmark
    public boolean bound() throws Exception {
        return (boolean)isBoundInetAddress_method.invoke(null, address);
    }

    @Benchmark
    public NetworkInterface getByInetAddress() throws Exception {
        return (NetworkInterface)getByInetAddress_method.invoke(null, address);
    }
}
