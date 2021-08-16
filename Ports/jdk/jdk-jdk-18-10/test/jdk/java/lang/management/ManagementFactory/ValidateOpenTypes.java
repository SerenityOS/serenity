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
 * @summary Validate open types mapped for the MXBeans in the platform
 *          MBeanServer.
 * @author  Mandy Chung
 *
 * @compile ValidateOpenTypes.java
 * @run main/othervm -verbose:gc ValidateOpenTypes
 */
import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import static java.lang.management.ManagementFactory.*;
import java.util.List;
import java.util.Map;
import com.sun.management.GcInfo;

public class ValidateOpenTypes {
    private static MBeanServer server =
        ManagementFactory.getPlatformMBeanServer();
    private static ObjectName memory;
    private static ObjectName thread;
    private static ObjectName runtime;
    private static ObjectName os;
    private static ObjectName heapPool = null;
    private static ObjectName nonHeapPool = null;

    public static void main(String[] argv) throws Exception {
        memory = new ObjectName(MEMORY_MXBEAN_NAME);
        runtime = new ObjectName(RUNTIME_MXBEAN_NAME);
        thread = new ObjectName(THREAD_MXBEAN_NAME);
        os = new ObjectName(OPERATING_SYSTEM_MXBEAN_NAME);

        List<MemoryPoolMXBean> pools = getMemoryPoolMXBeans();
        for (MemoryPoolMXBean p : pools) {
            if (heapPool == null &&
                p.getType() == MemoryType.HEAP &&
                p.isUsageThresholdSupported() &&
                p.isCollectionUsageThresholdSupported()) {
                heapPool = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE +
                               ",name=" + p.getName());
            }
            if (nonHeapPool == null &&
                p.getType() == MemoryType.NON_HEAP &&
                p.isUsageThresholdSupported()) {
                nonHeapPool = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE +
                               ",name=" + p.getName());
            }
        }

        // Check notification emitters
        MyListener listener = new MyListener();
        server.addNotificationListener(memory, listener, null, null);
        server.removeNotificationListener(memory, listener);

        checkEnum();
        checkList();
        checkMap();
        checkMemoryUsage();
        checkThreadInfo();

        checkOS();
        checkSunGC();

        System.out.println("Test passed.");
    }

    private static void checkEnum() throws Exception {
        String type = (String) server.getAttribute(heapPool, "Type");
        if (!type.equals("HEAP")) {
            throw new RuntimeException("TEST FAILED: " +
                " incorrect memory type for " + heapPool);
        }

        type = (String) server.getAttribute(nonHeapPool, "Type");
        if (!type.equals("NON_HEAP")) {
            throw new RuntimeException("TEST FAILED: " +
                " incorrect memory type for " + nonHeapPool);
        }
    }

    private static final String OPTION = "-verbose:gc";
    private static void checkList() throws Exception {
        String[] args = (String[]) server.getAttribute(runtime,
                                                       "InputArguments");
        if (args.length < 1) {
           throw new RuntimeException("TEST FAILED: " +
                " empty input arguments");
        }
        // check if -verbose:gc exists
        boolean found = false;
        for (String option : args) {
           if (option.equals(OPTION)) {
               found = true;
               break;
           }
        }
        if (!found) {
            throw new RuntimeException("TEST FAILED: " +
                "VM option " + OPTION + " not found");
        }
    }

    private static final String KEY1   = "test.property.key1";
    private static final String VALUE1 = "test.property.value1";
    private static final String KEY2   = "test.property.key2";
    private static final String VALUE2 = "test.property.value2";
    private static final String KEY3   = "test.property.key3";
    private static void checkMap() throws Exception {
        // Add new system properties
        System.setProperty(KEY1, VALUE1);
        System.setProperty(KEY2, VALUE2);

        TabularData props1 = (TabularData)
            server.getAttribute(runtime, "SystemProperties");

        String value1 = getProperty(props1, KEY1);
        if (value1 == null || !value1.equals(VALUE1)) {
            throw new RuntimeException("TEST FAILED: " +
                 KEY1 + " property found" +
                 " with value = " + value1 +
                 " but expected to be " + VALUE1);
        }

        String value2 = getProperty(props1, KEY2);
        if (value2 == null || !value2.equals(VALUE2)) {
            throw new RuntimeException("TEST FAILED: " +
                 KEY2 + " property found" +
                 " with value = " + value2 +
                 " but expected to be " + VALUE2);
        }

        String value3 = getProperty(props1, KEY3);
        if (value3 != null) {
            throw new RuntimeException("TEST FAILED: " +
                 KEY3 + " property found" +
                 " but should not exist" );
        }
    }
    private static String getProperty(TabularData td, String propName) {
        CompositeData cd = td.get(new Object[] { propName});
        if (cd != null) {
            String key = (String) cd.get("key");
            if (!propName.equals(key)) {
                 throw new RuntimeException("TEST FAILED: " +
                     key + " property found" +
                     " but expected to be " + propName);
            }
            return (String) cd.get("value");
        }
        return null;
    }

    private static void checkMemoryUsage() throws Exception {
        // sanity check to have non-negative usage
        Object u1 = server.getAttribute(memory, "HeapMemoryUsage");
        Object u2 = server.getAttribute(memory, "NonHeapMemoryUsage");
        Object u3 = server.getAttribute(heapPool, "Usage");
        Object u4 = server.getAttribute(nonHeapPool, "Usage");
        if (getCommitted(u1) < 0 ||
            getCommitted(u2) < 0 ||
            getCommitted(u3) < 0 ||
            getCommitted(u4) < 0) {
            throw new RuntimeException("TEST FAILED: " +
                " expected non-negative committed usage");
        }
        server.invoke(memory, "gc", new Object[0], new String[0]);
        Object u5 = server.getAttribute(heapPool, "CollectionUsage");
        if (getCommitted(u5) < 0) {
            throw new RuntimeException("TEST FAILED: " +
                " expected non-negative committed collected usage");
        }
    }

    private static long getCommitted(Object data) {
        MemoryUsage u = MemoryUsage.from((CompositeData) data);
        return u.getCommitted();
    }

    private static void checkThreadInfo() throws Exception {
        // assume all threads stay alive
        long[] ids = (long[]) server.getAttribute(thread, "AllThreadIds");
        Object result = server.invoke(thread,
                                      "getThreadInfo",
                                      new Object[] { ids },
                                      new String[] { "[J" });
        for (CompositeData cd : (CompositeData[]) result) {
            printThreadInfo(cd);
        }

        result = server.invoke(thread,
                               "getThreadInfo",
                               new Object[] { ids, new Integer(2) },
                               new String[] { "[J", "int" });
        for (CompositeData cd : (CompositeData[]) result) {
            printThreadInfo(cd);
        }

        long id = Thread.currentThread().getId();
        result = server.invoke(thread,
                               "getThreadInfo",
                               new Object[] { new Long(id) },
                               new String[] { "long" });
        printThreadInfo((CompositeData) result);

        result = server.invoke(thread,
                               "getThreadInfo",
                               new Object[] { new Long(id), new Integer(2) },
                               new String[] { "long", "int" });
        printThreadInfo((CompositeData) result);
    }

    private static void printThreadInfo(CompositeData cd) {
        ThreadInfo info = ThreadInfo.from(cd);
        if (info == null) {
            throw new RuntimeException("TEST FAILED: " +
                " Null ThreadInfo");
        }

        System.out.print(info.getThreadName());
        System.out.print(" id=" + info.getThreadId());
        System.out.println(" " + info.getThreadState());

        for (StackTraceElement s : info.getStackTrace()) {
            System.out.println(s);
        }
    }

    private static void checkOS() throws Exception {
        Integer cpus = (Integer) server.getAttribute(os, "AvailableProcessors");
        System.out.println("# CPUs = " + cpus);
        Long vmem = (Long) server.getAttribute(os, "CommittedVirtualMemorySize");
        System.out.println("Committed virtual memory = " + vmem);
    }

    private static void checkSunGC() throws Exception {
       // Test com.sun.management proxy
        List<GarbageCollectorMXBean> gcs = getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            ObjectName sunGc =
                new ObjectName(GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE +
                               ",name=" + gc.getName());
            CompositeData cd = (CompositeData) server.getAttribute(sunGc, "LastGcInfo");
            if (cd != null) {
                System.out.println("GC statistic for : " + gc.getName());
                printGcInfo(cd);
            }
        }
    }

    private static void printGcInfo(CompositeData cd) throws Exception {
        GcInfo info = GcInfo.from(cd);
        System.out.print("GC #" + info.getId());
        System.out.print(" start:" + info.getStartTime());
        System.out.print(" end:" + info.getEndTime());
        System.out.println(" (" + info.getDuration() + "ms)");
        Map<String,MemoryUsage> usage = info.getMemoryUsageBeforeGc();

        for (Map.Entry<String,MemoryUsage> entry : usage.entrySet()) {
            String poolname = entry.getKey();
            MemoryUsage busage = entry.getValue();
            MemoryUsage ausage = info.getMemoryUsageAfterGc().get(poolname);
            if (ausage == null) {
                throw new RuntimeException("After Gc Memory does not exist" +
                    " for " + poolname);
            }
            System.out.println("Usage for pool " + poolname);
            System.out.println("   Before GC: " + busage);
            System.out.println("   After GC: " + ausage);
        }
    }

    static class MyListener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            return;
        }
    }
}
