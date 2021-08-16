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
 * @bug     5058319
 * @summary Check if a RuntimeException is wrapped by RuntimeMBeanException
 *          only once.
 *
 * @requires vm.gc != "Z"
 * @author  Mandy Chung
 *
 * @build MXBeanException
 * @run main MXBeanException
 */

import java.lang.management.*;
import javax.management.*;
import java.util.*;
import static java.lang.management.ManagementFactory.*;

public class MXBeanException {
    private static MemoryPoolMXBean pool;

    public static void main(String[] argv) throws Exception {
        List<MemoryPoolMXBean> pools =
            ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean p : pools) {
            if (!p.isUsageThresholdSupported()) {
                pool = p;
                break;
            }
        }

        // check if UnsupportedOperationException is thrown
        try {
            pool.setUsageThreshold(1000);
            throw new RuntimeException("TEST FAILED: " +
                "UnsupportedOperationException not thrown");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // check if UnsupportedOperationException is thrown
        // when calling through MBeanServer
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE +
                                       ",name=" + pool.getName());
        Attribute att = new Attribute("UsageThreshold", 1000);
        try {
            mbs.setAttribute(on, att);
        } catch (RuntimeMBeanException e) {
            checkMBeanException(e);
        } catch (RuntimeOperationsException e) {
            checkMBeanException(e);
        }

        // check if UnsupportedOperationException is thrown
        // when calling through proxy

        MemoryPoolMXBean proxy = newPlatformMXBeanProxy(mbs,
                                     on.toString(),
                                     MemoryPoolMXBean.class);
        try {
            proxy.setUsageThreshold(1000);
            throw new RuntimeException("TEST FAILED: " +
                "UnsupportedOperationException not thrown via proxy");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        System.out.println("Test passed");
    }

    private static void checkMBeanException(JMRuntimeException e) {
        Throwable cause = e.getCause();
        if (!(cause instanceof UnsupportedOperationException)) {
            throw new RuntimeException("TEST FAILED: " + cause +
                "is not UnsupportedOperationException");
        }
    }
}
