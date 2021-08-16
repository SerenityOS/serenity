/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.metaspace;

import java.util.List;
import java.lang.management.*;
import jdk.test.lib.Platform;
import static jdk.test.lib.Asserts.*;

/* @test TestMetaspaceMemoryPool
 * @bug 8000754
 * @summary Tests that a MemoryPoolMXBeans is created for metaspace and that a
 *          MemoryManagerMXBean is created.
 * @requires vm.bits == 64 & vm.opt.final.UseCompressedOops == true
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseCompressedOops gc.metaspace.TestMetaspaceMemoryPool
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseCompressedOops -XX:MaxMetaspaceSize=60m gc.metaspace.TestMetaspaceMemoryPool
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCompressedOops -XX:+UseCompressedClassPointers gc.metaspace.TestMetaspaceMemoryPool
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:CompressedClassSpaceSize=60m gc.metaspace.TestMetaspaceMemoryPool
 */
public class TestMetaspaceMemoryPool {
    public static void main(String[] args) {
        verifyThatMetaspaceMemoryManagerExists();

        boolean isMetaspaceMaxDefined = InputArguments.containsPrefix("-XX:MaxMetaspaceSize");
        verifyMemoryPool(getMemoryPool("Metaspace"), isMetaspaceMaxDefined);

        if (Platform.is64bit()) {
            if (InputArguments.contains("-XX:+UseCompressedOops")) {
                MemoryPoolMXBean cksPool = getMemoryPool("Compressed Class Space");
                verifyMemoryPool(cksPool, true);
            }
        }
    }

    private static void verifyThatMetaspaceMemoryManagerExists() {
        List<MemoryManagerMXBean> managers = ManagementFactory.getMemoryManagerMXBeans();
        for (MemoryManagerMXBean manager : managers) {
            if (manager.getName().equals("Metaspace Manager")) {
                return;
            }
        }

        throw new RuntimeException("Expected to find a metaspace memory manager");
    }

    private static MemoryPoolMXBean getMemoryPool(String name) {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().equals(name)) {
                return pool;
            }
        }

        throw new RuntimeException("Expected to find a memory pool with name " + name);
    }

    private static void verifyMemoryPool(MemoryPoolMXBean pool, boolean isMaxDefined) {
        MemoryUsage mu = pool.getUsage();
        long init = mu.getInit();
        long used = mu.getUsed();
        long committed = mu.getCommitted();
        long max = mu.getMax();

        assertGTE(init, 0L);
        assertGTE(used, init);
        assertGTE(committed, used);

        if (isMaxDefined) {
            assertGTE(max, committed);
        } else {
            assertEQ(max, -1L);
        }
    }
}
