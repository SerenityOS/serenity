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
 * @bug 6283045
 * @summary Test the correctness of immutableInfo field in MBeanInfo descriptor
 *          when overriding the methods cacheMBeanInfo, getCachedMBeanInfo,
 *          getMBeanInfo and getNotificationInfo in StandardMBean and
 *          StandardEmitterMBean.
 * @author Luis-Miguel Alventosa
 *
 * @run clean StandardMBeanOverrideTest
 * @run build StandardMBeanOverrideTest
 * @run main StandardMBeanOverrideTest
 */

import java.io.*;
import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.*;

public class StandardMBeanOverrideTest {

    private static Object testInstances[] = {
        new TestClass0(false),
        new TestClass1(false),
        new TestClass2(false),
        new TestClass3(false),
        new TestClass4(false),
        new TestClass5(false),
        new TestClass6(false),
        new TestClass7(false),
        new TestClass8(false),
        new TestClass9(false),
        new TestClass0(true),
        new TestClass1(true),
        new TestClass2(true),
        new TestClass3(true),
        new TestClass4(true),
        new TestClass5(true),
        new TestClass6(true),
        new TestClass7(true),
        new TestClass8(true),
        new TestClass9(true),
    };

    public interface ImmutableInfo {
    }

    public interface NonImmutableInfo {
    }

    public interface TestInterface {
    }

    public static class TestClass0
        extends StandardMBean
        implements TestInterface, ImmutableInfo {
        public TestClass0(boolean mxbean) {
            super(TestInterface.class, mxbean);
        }
    }

    public static class TestClass1
        extends StandardMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass1(boolean mxbean) {
            super(TestInterface.class, mxbean);
        }
        protected void cacheMBeanInfo(MBeanInfo info) {
            super.cacheMBeanInfo(info);
        }
    }

    public static class TestClass2
        extends StandardMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass2(boolean mxbean) {
            super(TestInterface.class, mxbean);
        }
        protected MBeanInfo getCachedMBeanInfo() {
            return super.getCachedMBeanInfo();
        }
    }

    public static class TestClass3
        extends StandardMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass3(boolean mxbean) {
            super(TestInterface.class, mxbean);
        }
        public MBeanInfo getMBeanInfo() {
            return super.getMBeanInfo();
        }
    }

    public static class TestClass4
        extends StandardMBean
        implements TestInterface, ImmutableInfo {
        public TestClass4(boolean mxbean) {
            super(TestInterface.class, mxbean);
        }
        public MBeanNotificationInfo[] getNotificationInfo() {
            return new MBeanNotificationInfo[0];
        }
    }

    public static class TestClass5
        extends StandardEmitterMBean
        implements TestInterface, ImmutableInfo {
        public TestClass5(boolean mxbean) {
            super(TestInterface.class, mxbean,
                  new NotificationBroadcasterSupport());
        }
    }

    public static class TestClass6
        extends StandardEmitterMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass6(boolean mxbean) {
            super(TestInterface.class, mxbean,
                  new NotificationBroadcasterSupport());
        }
        protected void cacheMBeanInfo(MBeanInfo info) {
            super.cacheMBeanInfo(info);
        }
    }

    public static class TestClass7
        extends StandardEmitterMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass7(boolean mxbean) {
            super(TestInterface.class, mxbean,
                  new NotificationBroadcasterSupport());
        }
        protected MBeanInfo getCachedMBeanInfo() {
            return super.getCachedMBeanInfo();
        }
    }

    public static class TestClass8
        extends StandardEmitterMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass8(boolean mxbean) {
            super(TestInterface.class, mxbean,
                  new NotificationBroadcasterSupport());
        }
        public MBeanInfo getMBeanInfo() {
            return super.getMBeanInfo();
        }
    }

    public static class TestClass9
        extends StandardEmitterMBean
        implements TestInterface, NonImmutableInfo {
        public TestClass9(boolean mxbean) {
            super(TestInterface.class, mxbean,
                  new NotificationBroadcasterSupport());
        }
        public MBeanNotificationInfo[] getNotificationInfo() {
            return new MBeanNotificationInfo[0];
        }
    }

    public static void main(String[] args) throws Exception {

        int error = 0;

        // Instantiate the MBean server
        //
        echo("\n>>> Create the MBean server");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Get default domain
        //
        echo("\n>>> Get the MBean server's default domain");
        String domain = mbs.getDefaultDomain();
        echo("\tDefault Domain = " + domain);

        for (int i = 0; i < testInstances.length; i++) {
            // Create and register the TestClass MBean
            //
            String cn = testInstances[i].getClass().getName();
            String ons = domain + ":type=" + cn + ",name=" + i;
            echo("\n>>> Create the " + cn +
                 " MBean within the MBeanServer");
            echo("\tObjectName = " + ons);
            ObjectName on = ObjectName.getInstance(ons);
            mbs.registerMBean(testInstances[i], on);

            // Check immutableInfo field in descriptor
            //
            MBeanInfo mbi = mbs.getMBeanInfo(on);
            Descriptor d = mbi.getDescriptor();
            echo("MBeanInfo descriptor = " + d);
            if (d == null || d.getFieldNames().length == 0) {
                error++;
                echo("Descriptor is null or empty");
                continue;
            }
            if (testInstances[i] instanceof ImmutableInfo) {
                if ("true".equals(d.getFieldValue("immutableInfo"))) {
                    echo("OK: immutableInfo field is true");
                } else {
                    echo("KO: immutableInfo field should be true");
                    error++;
                }
                continue;
            }
            if (testInstances[i] instanceof NonImmutableInfo) {
                if ("false".equals(d.getFieldValue("immutableInfo"))) {
                    echo("OK: immutableInfo field is false");
                } else {
                    echo("KO: immutableInfo field should be false");
                    error++;
                }
                continue;
            }
        }

        if (error > 0) {
            echo("\nTest failed! " + error + " errors.\n");
            throw new IllegalArgumentException("Test failed");
        } else {
            echo("\nTest passed!\n");
        }
    }

    private static void echo(String msg) {
        System.out.println(msg);
    }
}
