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
 * @bug 4716807
 * @summary Test if the repository supports correctly the use of
 *          wildcards in the ObjectName key properties value part.
 * @author Luis-Miguel Alventosa
 *
 * @run clean RepositoryWildcardTest
 * @run build RepositoryWildcardTest
 * @run main RepositoryWildcardTest
 */

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

public class RepositoryWildcardTest {

    private static final String classname =
            "javax.management.monitor.StringMonitor";

    private static int mbeanCreation(MBeanServer mbs, String name)
        throws Exception {
        int error = 0;
        try {
            System.out.println("Test: createMBean(" + name + ")");
            mbs.createMBean(classname, ObjectName.getInstance(name));
            error++;
            System.out.println("Didn't get expected exception!");
            System.out.println("Test failed!");
        } catch (RuntimeOperationsException e) {
            System.out.println("Got expected exception = " +
                               e.getCause().toString());
            System.out.println("Test passed!");
        }
        return error;
    }

    private static int mbeanDeletion(MBeanServer mbs, String name)
        throws Exception {
        int error = 0;
        try {
            System.out.println("Test: unregisterMBean(" + name + ")");
            mbs.unregisterMBean(ObjectName.getInstance(name));
            error++;
            System.out.println("Didn't get expected exception!");
            System.out.println("Test failed!");
        } catch (InstanceNotFoundException e) {
            System.out.println("Got expected exception = " + e.toString());
            System.out.println("Test passed!");
        }
        return error;
    }

    private static int mbeanQuery(MBeanServer mbs,
                                  String name,
                                  Set<ObjectName> expectedSet)
        throws Exception {
        int error = 0;
        System.out.println("Test: queryNames(" + name + ")");
        Set<ObjectName> returnedSet =
                mbs.queryNames(ObjectName.getInstance(name), null);
        System.out.println("ReturnedSet = " + new TreeSet(returnedSet));
        System.out.println("ExpectedSet = " + new TreeSet(expectedSet));
        if (returnedSet.equals(expectedSet)) {
            System.out.println("Test passed!");
        } else {
            error++;
            System.out.println("Test failed!");
        }
        return error;
    }

    public static void main(String[] args) throws Exception {

        int error = 0;

        MBeanServer mbs = MBeanServerFactory.newMBeanServer();

        ObjectName[] namesArray = {
            ObjectName.getInstance("d:k=abc"),
            ObjectName.getInstance("d:k=abcd"),
            ObjectName.getInstance("d:k=abcde"),
            ObjectName.getInstance("d:k=abc,k2=v2"),
            ObjectName.getInstance("d:k=abcd,k2=v2"),
            ObjectName.getInstance("d:k=abcde,k2=v2"),
            ObjectName.getInstance("d:k=\"abc\""),
            ObjectName.getInstance("d:k=\"abc\",k2=v2"),
            ObjectName.getInstance("d:p1=v1,p2=v2,p3=v3,p4=v4,p5=v5,p6=v6," +
                                   "p7=v7,p8=v8,p9=v9,p10=v10,p11=v11,p12=v12")
        };

        for (ObjectName name : namesArray)
            mbs.createMBean(classname, name);

        System.out.println("----------------------------------------------");
        System.out.println("TEST createMBean WITH PATTERNS");
        System.out.println("----------------------------------------------");

        error += mbeanCreation(mbs, "d:k=v,*");
        error += mbeanCreation(mbs, "d:k=*");
        error += mbeanCreation(mbs, "d:k=a?b");
        error += mbeanCreation(mbs, "d:k=\"?\"");
        error += mbeanCreation(mbs, "d:k=\"a*b\"");

        System.out.println("----------------------------------------------");
        System.out.println("TEST queryNames WITH PATTERNS");
        System.out.println("----------------------------------------------");

        Set<ObjectName> expectedSet = new HashSet<ObjectName>();
        for (ObjectName name : namesArray)
            expectedSet.add(name);
        expectedSet.add(MBeanServerDelegate.DELEGATE_NAME);
        Set<ObjectName> returnedSet =
            mbs.queryNames(ObjectName.getInstance("*:*"), null);
        error += mbeanQuery(mbs, "*:*", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        for (ObjectName name : namesArray)
            expectedSet.add(name);
        error += mbeanQuery(mbs, "d:*", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[0]);
        expectedSet.add(namesArray[1]);
        expectedSet.add(namesArray[2]);
        expectedSet.add(namesArray[6]);
        error += mbeanQuery(mbs, "d:k=*", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[3]);
        expectedSet.add(namesArray[4]);
        expectedSet.add(namesArray[5]);
        expectedSet.add(namesArray[7]);
        error += mbeanQuery(mbs, "d:k=*,k2=v2", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[3]);
        expectedSet.add(namesArray[4]);
        expectedSet.add(namesArray[5]);
        expectedSet.add(namesArray[7]);
        error += mbeanQuery(mbs, "d:k=*,k2=v?", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[3]);
        expectedSet.add(namesArray[4]);
        expectedSet.add(namesArray[5]);
        expectedSet.add(namesArray[7]);
        error += mbeanQuery(mbs, "d:*,k2=v2", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[1]);
        error += mbeanQuery(mbs, "d:k=ab?d", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[1]);
        expectedSet.add(namesArray[4]);
        error += mbeanQuery(mbs, "d:k=ab?d,*", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[6]);
        error += mbeanQuery(mbs, "d:k=\"*\"", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[7]);
        error += mbeanQuery(mbs, "d:k=\"*\",k2=v?", expectedSet);

        expectedSet = new HashSet<ObjectName>();
        expectedSet.add(namesArray[8]);
        error += mbeanQuery(mbs,
                            "d:p1=v?,p2=v?,p3=v?,p4=v?,p5=v?,p6=v?," +
                            "p7=v?,p8=v?,p9=v?,p10=v??,p11=v??,p12=v??",
                            expectedSet);

        System.out.println("----------------------------------------------");
        System.out.println("TEST unregisterMBean WITH PATTERNS");
        System.out.println("----------------------------------------------");

        error += mbeanDeletion(mbs, "d:k=*");
        error += mbeanDeletion(mbs, "d:k=\"*\"");

        if (error > 0) {
            final String msg = "Test FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("Test PASSED!");
        }
    }
}
