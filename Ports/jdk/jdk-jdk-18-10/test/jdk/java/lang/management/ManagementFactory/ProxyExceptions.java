/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     5024531
 * @summary Test type mapping of the platform MXBean proxy
 *          returned from Management.newPlatformMXBeanProxy().
 * @author  Mandy Chung
 */
import java.lang.management.*;
import javax.management.*;
import static java.lang.management.ManagementFactory.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import com.sun.management.GcInfo;

public class ProxyExceptions {
    private static MBeanServer server =
        ManagementFactory.getPlatformMBeanServer();
    private static MemoryPoolMXBean heapPool = null;
    private static MemoryPoolMXBean nonHeapPool = null;
    public static void main(String[] argv) throws Exception {
        List<MemoryPoolMXBean> pools = getMemoryPoolMXBeans();
        for (MemoryPoolMXBean p : pools) {
            MemoryPoolMXBean proxy = newPlatformMXBeanProxy(server,
                MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",name=" + p.getName(),
                MemoryPoolMXBean.class);
            boolean uoeCaught;
            if (!p.isUsageThresholdSupported()) {
                try {
                    proxy.getUsageThreshold();
                    uoeCaught = false;
                } catch (UnsupportedOperationException e) {
                    uoeCaught = true;
                }
                if (!uoeCaught) {
                    throw new RuntimeException("TEST FAILED: " +
                        "UnsupportedOperationException not thrown " +
                        "when calling getUsageThreshold on " + p.getName());
                }
                try {
                    proxy.setUsageThreshold(100);
                    uoeCaught = false;
                } catch (UnsupportedOperationException e) {
                    uoeCaught = true;
                }
                if (!uoeCaught) {
                    throw new RuntimeException("TEST FAILED: " +
                        "UnsupportedOperationException not thrown " +
                        "when calling setUsageThreshold on " + p.getName());
                }
            }
            if (!p.isCollectionUsageThresholdSupported()) {
                try {
                    proxy.getCollectionUsageThreshold();
                    uoeCaught = false;
                } catch (UnsupportedOperationException e) {
                    uoeCaught = true;
                }
                if (!uoeCaught) {
                    throw new RuntimeException("TEST FAILED: " +
                        "UnsupportedOperationException not thrown " +
                        "when calling getCollectionUsageThreshold on " +
                        p.getName());
                }
                try {
                    proxy.setCollectionUsageThreshold(100);
                    uoeCaught = false;
                } catch (UnsupportedOperationException e) {
                    uoeCaught = true;
                }
                if (!uoeCaught) {
                    throw new RuntimeException("TEST FAILED: " +
                        "UnsupportedOperationException not thrown " +
                        "when calling getCollectionUsageThreshold on " +
                        p.getName());
                }
            }
        }

        ThreadMXBean thread = newPlatformMXBeanProxy(server,
                                                  THREAD_MXBEAN_NAME,
                                                  ThreadMXBean.class);
        boolean iaeCaught = false;
        try {
            thread.getThreadInfo(-1);
        } catch (IllegalArgumentException e) {
            iaeCaught = true;
        }
        if (!iaeCaught) {
            throw new RuntimeException("TEST FAILED: " +
                "IllegalArgumentException not thrown " +
                "when calling getThreadInfo(-1)");
        }

        System.out.println("Test passed.");
    }
}
