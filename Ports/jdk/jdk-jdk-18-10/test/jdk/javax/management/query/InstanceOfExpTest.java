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
 * @bug 5072174 6335848
 * @summary test the new method javax.management.Query.isInstanceOf("className")
 * @author Shanliang JIANG
 *
 * @run clean InstanceOfExpTest
 * @run build InstanceOfExpTest
 * @run main InstanceOfExpTest
 */

import java.util.*;
import java.lang.management.ManagementFactory;

import javax.management.*;

public class InstanceOfExpTest {

    public static class Simple implements SimpleMBean {}
    public static interface SimpleMBean {}

    public static void main(String[] args) throws Exception {
        System.out.println(">>> Test the method javax.management.Query.isInstanceOf");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final String className = "javax.management.NotificationBroadcaster";

        final ObjectName name1 = new ObjectName("test:simple=1");
        mbs.createMBean(Simple.class.getName(), name1);

        final ObjectName name2 = new ObjectName("test:timer=1");
        mbs.createMBean("javax.management.timer.Timer", name2);

        QueryExp exp = Query.isInstanceOf(Query.value(className));
        Set<ObjectName> list = mbs.queryNames(new ObjectName("*:*"), exp);

        if (list.contains(name1) || !list.contains(name2)) {
            throw new RuntimeException("InstanceOfExp does not work.");
        }

        for (ObjectName on : list) {
            if (!mbs.isInstanceOf(on, className)) {
                throw new RuntimeException("InstanceOfQueryExp does not work.");
            }
        }

        Set<ObjectName> all = mbs.queryNames(null, null);
        for (ObjectName n : all) {
            if (mbs.isInstanceOf(n, className) != list.contains(n))
                throw new RuntimeException("InstanceOfExp does not work.");
        }

        try {
            QueryExp exp1 = Query.isInstanceOf(null);
            throw new RuntimeException("Not got an exception with a null class name.");
        } catch (IllegalArgumentException iae) {
            // OK. Good
        }
    }
}
