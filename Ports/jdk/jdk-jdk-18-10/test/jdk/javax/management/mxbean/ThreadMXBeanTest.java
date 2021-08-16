/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6305746
 * @key randomness
 * @summary Test that the null values returned by the ThreadMXBean work.
 * @author Eamonn McManus
 *
 * @run clean ThreadMXBeanTest
 * @run build ThreadMXBeanTest
 * @run main ThreadMXBeanTest
 */

import java.lang.management.*;
import java.util.*;
import javax.management.*;

public class ThreadMXBeanTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
        StandardMBean smb = new StandardMBean(tmb, ThreadMXBean.class, true);
        ObjectName on = new ObjectName("a:type=ThreadMXBean");
        mbs.registerMBean(smb, on);
        ThreadMXBean proxy = JMX.newMXBeanProxy(mbs, on, ThreadMXBean.class);
        long[] ids1 = proxy.getAllThreadIds();

        // Add some random ids to the list so we'll get back null ThreadInfo
        long[] ids2 = new long[ids1.length + 10];
        System.arraycopy(ids1, 0, ids2, 0, ids1.length);
        Random r = new Random();
        for (int i = ids1.length; i < ids2.length; i++)
            ids2[i] = Math.abs(r.nextLong());
        // Following line produces an exception if null values not handled
        ThreadInfo[] info = proxy.getThreadInfo(ids2);
        boolean sawNull = false;
        for (ThreadInfo ti : info) {
            if (ti == null)
                sawNull = true;
        }
        if (!sawNull)
            throw new Exception("No null value in returned array");
        System.out.println("TEST PASSED");
    }
}
