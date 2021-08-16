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
package org.openjdk.bench.vm.lang;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Threads(Threads.MAX)
public class MonitorBench {

    @Param({"100", "250"})
    int consumeUnlocked;

    @Param({"100", "250"})
    int consumeLocked;

    @Param({"0", "1"})
    int throwThreshold;

    @Param({"10000"})
    int range;

    @Param({"1"})
    static int locksSize;

    Object[] sharedLocks;

    @Setup
    public void setup() {
        sharedLocks = new Object[locksSize];
        for (int i = 0; i < locksSize; i++) {
            sharedLocks[i] = new Object();
        }
    }

    int update2(int sharedIndex) throws Exception {
        synchronized (sharedLocks[sharedIndex]) {
            Blackhole.consumeCPU(consumeLocked);
            if (ThreadLocalRandom.current().nextInt(range) < throwThreshold) {
                throw new Exception("Update failed");
            } else {
                Blackhole.consumeCPU(consumeLocked);
                return 0;
            }
        }
    }

    int update1(int sharedIndex) throws Exception {
        synchronized (sharedLocks[sharedIndex]) {
            Blackhole.consumeCPU(consumeLocked);
            return update2(sharedIndex);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int action() throws InterruptedException {
        Blackhole.consumeCPU(consumeUnlocked);
        int sharedLockIndex = ThreadLocalRandom.current().nextInt(locksSize);
        Object sharedLock = sharedLocks[sharedLockIndex];
        synchronized (sharedLock) {
            while (true) {
                try {
                    return update1(sharedLockIndex);
                } catch (Exception e) {
                    sharedLock.wait(100);
                }
            }
        }
    }
}
