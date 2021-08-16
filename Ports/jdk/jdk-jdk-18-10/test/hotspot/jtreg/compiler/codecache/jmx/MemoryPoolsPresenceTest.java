/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test MemoryPoolsPresenceTest
 * @summary verify that MemoryManagerMXBean exists for every code cache segment
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @library /test/lib
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.MemoryPoolsPresenceTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.MemoryPoolsPresenceTest
 */

package compiler.codecache.jmx;

import jdk.test.lib.Asserts;
import sun.hotspot.code.BlobType;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MemoryPoolsPresenceTest {

    private static final String CC_MANAGER = "CodeCacheManager";
    private final Map<String, Integer> counters = new HashMap<>();

    public static void main(String args[]) {
        new MemoryPoolsPresenceTest().runTest();
    }

    protected void runTest() {
        List<MemoryManagerMXBean> beans
                = ManagementFactory.getMemoryManagerMXBeans();
        Optional<MemoryManagerMXBean> any = beans
                .stream()
                .filter(bean -> CC_MANAGER.equals(bean.getName()))
                .findAny();
        Asserts.assertTrue(any.isPresent(), "Bean not found: " + CC_MANAGER);
        MemoryManagerMXBean ccManager = any.get();
        Asserts.assertNotNull(ccManager, "Found null for " + CC_MANAGER);
        String names[] = ccManager.getMemoryPoolNames();
        for (String name : names) {
            counters.put(name, counters.containsKey(name)
                    ? counters.get(name) + 1 : 1);
        }
        for (BlobType btype : BlobType.getAvailable()) {
            Asserts.assertEQ(counters.get(btype.getMemoryPool().getName()), 1,
                    "Found unexpected amount of beans for pool "
                    + btype.getMemoryPool().getName());
        }
        Asserts.assertEQ(BlobType.getAvailable().size(),
                counters.keySet().size(), "Unexpected amount of bean names");
    }
}
