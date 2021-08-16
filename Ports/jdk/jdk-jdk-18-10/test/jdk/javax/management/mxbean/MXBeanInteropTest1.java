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
 * @bug 8058865
 * @summary Test all MXBeans available by default on the platform
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /lib/testlibrary
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD MXBeanInteropTest1
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class MXBeanInteropTest1 {

    /*
     * First Debug properties and arguments are collect in expected
     * map  (argName, value) format, then calls original test's run method.
     */
    public static void main(String args[]) throws Exception {

        System.out.println("=================================================");

        // Parses parameters
        Utils.parseDebugProperties();
        Map<String, Object> map = Utils.parseParameters(args) ;

        // Run test
        MXBeanInteropTest1 test = new MXBeanInteropTest1();
        test.run(map);

    }

    public void run(Map<String, Object> args) {

        System.out.println("MXBeanInteropTest1::run: Start") ;
        int errorCount = 0 ;

        try {
            // JMX MbeanServer used inside single VM as if remote.
            // MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();

            JMXServiceURL addr = cs.getAddress();
            JMXConnector cc = JMXConnectorFactory.connect(addr);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();

            // Print out registered java.lang.management MXBeans found
            // in the remote jvm.
            printMBeans(mbsc) ;

            // For each possible kind of JDK 5 defined MXBean, we retrieve its
            // MBeanInfo and print it and we call all getters and print
            // their output.
            errorCount += doClassLoadingMXBeanTest(mbsc) ;
            errorCount += doMemoryMXBeanTest(mbsc) ;
            errorCount += doThreadMXBeanTest(mbsc) ;
            errorCount += doRuntimeMXBeanTest(mbsc) ;
            errorCount += doOperatingSystemMXBeanTest(mbsc) ;
            errorCount += doCompilationMXBeanTest(mbsc) ;
            errorCount += doGarbageCollectorMXBeanTest(mbsc) ;
            errorCount += doMemoryManagerMXBeanTest(mbsc) ;
            errorCount += doMemoryPoolMXBeanTest(mbsc) ;

            // Terminate the JMX Client
            cc.close();

        } catch(Exception e) {
            Utils.printThrowable(e, true) ;
            throw new RuntimeException(e);
        }

        if ( errorCount == 0 ) {
            System.out.println("MXBeanInteropTest1::run: Done without any error") ;
        } else {
            System.out.println("MXBeanInteropTest1::run: Done with "
                    + errorCount
                    + " error(s)") ;
            throw new RuntimeException("errorCount = " + errorCount);
        }
    }

    /**
     * Prints all MBeans of domain java.lang.
     * They are MBeans related to the JSR 174 that defines
     * package java.lang.management.
     */
    private static void printMBeans(MBeanServerConnection mbsc) throws Exception {
        ObjectName filterName = new ObjectName("java.lang:*");
        Set<ObjectName> set = mbsc.queryNames(filterName, null);

        if ( set.size() == 0 ) {
            throw new RuntimeException("(ERROR) No MBean found with filter "
                    + filterName);
        }

        System.out.println("---- MBeans found in domain java.lang :");

        for (Iterator<ObjectName> iter = set.iterator(); iter.hasNext(); ) {
            System.out.println(iter.next().toString());
        }

        System.out.println("\n") ;
    }


    private final int doClassLoadingMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- ClassLoadingMXBean") ;

        try {
            ObjectName classLoadingName =
                    new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME) ;
            MBeanInfo mbInfo = mbsc.getMBeanInfo(classLoadingName);
            errorCount += checkNonEmpty(mbInfo);
            System.out.println("getMBeanInfo\t\t"
                    + mbInfo);
            ClassLoadingMXBean classLoading = null;

            classLoading = JMX.newMXBeanProxy(mbsc,
                    classLoadingName,
                    ClassLoadingMXBean.class) ;
            System.out.println("getLoadedClassCount\t\t"
                    + classLoading.getLoadedClassCount());
            System.out.println("getTotalLoadedClassCount\t\t"
                    + classLoading.getTotalLoadedClassCount());
            System.out.println("getUnloadedClassCount\t\t"
                    + classLoading.getUnloadedClassCount());
            System.out.println("isVerbose\t\t"
                    + classLoading.isVerbose());

            System.out.println("---- OK\n") ;

        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doMemoryMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- MemoryMXBean") ;

        try {
            ObjectName memoryName =
                    new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME) ;
            MBeanInfo mbInfo = mbsc.getMBeanInfo(memoryName);
            errorCount += checkNonEmpty(mbInfo);
            System.out.println("getMBeanInfo\t\t"
                    + mbInfo);
            MemoryMXBean memory = null ;

            memory =
                    JMX.newMXBeanProxy(mbsc,
                    memoryName,
                    MemoryMXBean.class,
                    true) ;
            System.out.println("getMemoryHeapUsage\t\t"
                    + memory.getHeapMemoryUsage());
            System.out.println("getNonHeapMemoryHeapUsage\t\t"
                    + memory.getNonHeapMemoryUsage());
            System.out.println("getObjectPendingFinalizationCount\t\t"
                    + memory.getObjectPendingFinalizationCount());
            System.out.println("isVerbose\t\t"
                    + memory.isVerbose());

            System.out.println("---- OK\n") ;

        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doThreadMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- ThreadMXBean") ;

        try {
            ObjectName threadName =
                    new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME) ;
            MBeanInfo mbInfo = mbsc.getMBeanInfo(threadName);
            errorCount += checkNonEmpty(mbInfo);
            System.out.println("getMBeanInfo\t\t" + mbInfo);
            ThreadMXBean thread = null ;

            thread =
                    JMX.newMXBeanProxy(mbsc,
                    threadName,
                    ThreadMXBean.class) ;
            System.out.println("findMonitorDeadlockedThreads\t\t"
                    + thread.findMonitorDeadlockedThreads());
            long[] threadIDs = thread.getAllThreadIds() ;
            System.out.println("getAllThreadIds\t\t"
                    + threadIDs);

            for ( long threadID : threadIDs ) {
                System.out.println("getThreadInfo long\t\t"
                        + thread.getThreadInfo(threadID));
                System.out.println("getThreadInfo long, int\t\t"
                        + thread.getThreadInfo(threadID, 2));
            }

            System.out.println("getThreadInfo long[]\t\t"
                    + thread.getThreadInfo(threadIDs));
            System.out.println("getThreadInfo long[], int\t\t"
                    + thread.getThreadInfo(threadIDs, 2));
            System.out.println("getDaemonThreadCount\t\t"
                    + thread.getDaemonThreadCount());
            System.out.println("getPeakThreadCount\t\t"
                    + thread.getPeakThreadCount());
            System.out.println("getThreadCount\t\t"
                    + thread.getThreadCount());
            System.out.println("getTotalStartedThreadCount\t\t"
                    + thread.getTotalStartedThreadCount());
            boolean supported = thread.isThreadContentionMonitoringSupported() ;
            System.out.println("isThreadContentionMonitoringSupported\t\t"
                    + supported);

            if ( supported ) {
                System.out.println("isThreadContentionMonitoringEnabled\t\t"
                        + thread.isThreadContentionMonitoringEnabled());
            }

            supported = thread.isThreadCpuTimeSupported() ;
            System.out.println("isThreadCpuTimeSupported\t\t"
                    + supported);

            if ( supported ) {
                System.out.println("isThreadCpuTimeEnabled\t\t"
                        + thread.isThreadCpuTimeEnabled());

                for (long id : threadIDs) {
                    System.out.println("getThreadCpuTime(" + id + ")\t\t"
                            + thread.getThreadCpuTime(id));
                    System.out.println("getThreadUserTime(" + id + ")\t\t"
                            + thread.getThreadUserTime(id));
                }
            }

            supported = thread.isCurrentThreadCpuTimeSupported() ;
            System.out.println("isCurrentThreadCpuTimeSupported\t\t"
                    + supported);

            if ( supported ) {
                System.out.println("getCurrentThreadCpuTime\t\t"
                        + thread.getCurrentThreadCpuTime());
                System.out.println("getCurrentThreadUserTime\t\t"
                        + thread.getCurrentThreadUserTime());
            }

            thread.resetPeakThreadCount() ;

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doRuntimeMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- RuntimeMXBean") ;

        try {
            ObjectName runtimeName =
                    new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME) ;
            MBeanInfo mbInfo = mbsc.getMBeanInfo(runtimeName);
            errorCount += checkNonEmpty(mbInfo);
            System.out.println("getMBeanInfo\t\t" + mbInfo);
            RuntimeMXBean runtime = null;

            runtime =
                    JMX.newMXBeanProxy(mbsc,
                    runtimeName,
                    RuntimeMXBean.class) ;
            System.out.println("getClassPath\t\t"
                    + runtime.getClassPath());
            System.out.println("getInputArguments\t\t"
                    + runtime.getInputArguments());
            System.out.println("getLibraryPath\t\t"
                    + runtime.getLibraryPath());
            System.out.println("getManagementSpecVersion\t\t"
                    + runtime.getManagementSpecVersion());
            System.out.println("getName\t\t"
                    + runtime.getName());
            System.out.println("getSpecName\t\t"
                    + runtime.getSpecName());
            System.out.println("getSpecVendor\t\t"
                    + runtime.getSpecVendor());
            System.out.println("getSpecVersion\t\t"
                    + runtime.getSpecVersion());
            System.out.println("getStartTime\t\t"
                    + runtime.getStartTime());
            System.out.println("getSystemProperties\t\t"
                    + runtime.getSystemProperties());
            System.out.println("getUptime\t\t"
                    + runtime.getUptime());
            System.out.println("getVmName\t\t"
                    + runtime.getVmName());
            System.out.println("getVmVendor\t\t"
                    + runtime.getVmVendor());
            System.out.println("getVmVersion\t\t"
                    + runtime.getVmVersion());
            boolean supported = runtime.isBootClassPathSupported() ;
            System.out.println("isBootClassPathSupported\t\t"
                    + supported);

            if ( supported ) {
                System.out.println("getBootClassPath\t\t"
                        + runtime.getBootClassPath());
            }

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doOperatingSystemMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- OperatingSystemMXBean") ;

        try {
            ObjectName operationName =
                    new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME) ;
            MBeanInfo mbInfo = mbsc.getMBeanInfo(operationName);
            errorCount += checkNonEmpty(mbInfo);
            System.out.println("getMBeanInfo\t\t" + mbInfo);
            OperatingSystemMXBean operation = null ;

            operation =
                    JMX.newMXBeanProxy(mbsc,
                    operationName,
                    OperatingSystemMXBean.class) ;
            System.out.println("getArch\t\t"
                    + operation.getArch());
            System.out.println("getAvailableProcessors\t\t"
                    + operation.getAvailableProcessors());
            System.out.println("getName\t\t"
                    + operation.getName());
            System.out.println("getVersion\t\t"
                    + operation.getVersion());

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doCompilationMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- CompilationMXBean") ;

        try {
            ObjectName compilationName =
                    new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME);

            if ( mbsc.isRegistered(compilationName) ) {
                MBeanInfo mbInfo = mbsc.getMBeanInfo(compilationName);
                errorCount += checkNonEmpty(mbInfo);
                System.out.println("getMBeanInfo\t\t" + mbInfo);
                CompilationMXBean compilation = null ;

                compilation =
                        JMX.newMXBeanProxy(mbsc,
                        compilationName,
                        CompilationMXBean.class) ;
                System.out.println("getName\t\t"
                        + compilation.getName());
                boolean supported =
                        compilation.isCompilationTimeMonitoringSupported() ;
                System.out.println("isCompilationTimeMonitoringSupported\t\t"
                        + supported);

                if ( supported ) {
                    System.out.println("getTotalCompilationTime\t\t"
                            + compilation.getTotalCompilationTime());
                }
            }

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doGarbageCollectorMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- GarbageCollectorMXBean") ;

        try {
            ObjectName filterName =
                    new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
                    + ",*");
            Set<ObjectName> onSet = mbsc.queryNames(filterName, null);

            for (Iterator<ObjectName> iter = onSet.iterator(); iter.hasNext(); ) {
                ObjectName garbageName = iter.next() ;
                System.out.println("-------- " + garbageName) ;
                MBeanInfo mbInfo = mbsc.getMBeanInfo(garbageName);
                errorCount += checkNonEmpty(mbInfo);
                System.out.println("getMBeanInfo\t\t" + mbInfo);
                GarbageCollectorMXBean garbage = null ;

                garbage =
                        JMX.newMXBeanProxy(mbsc,
                        garbageName,
                        GarbageCollectorMXBean.class) ;
                System.out.println("getCollectionCount\t\t"
                        + garbage.getCollectionCount());
                System.out.println("getCollectionTime\t\t"
                        + garbage.getCollectionTime());
            }

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doMemoryManagerMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- MemoryManagerMXBean") ;

        try {
            ObjectName filterName =
                    new ObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
                    + ",*");
            Set<ObjectName> onSet = mbsc.queryNames(filterName, null);

            for (Iterator<ObjectName> iter = onSet.iterator(); iter.hasNext(); ) {
                ObjectName memoryManagerName = iter.next() ;
                System.out.println("-------- " + memoryManagerName) ;
                MBeanInfo mbInfo = mbsc.getMBeanInfo(memoryManagerName);
                System.out.println("getMBeanInfo\t\t" + mbInfo);
                errorCount += checkNonEmpty(mbInfo);
                MemoryManagerMXBean memoryManager = null;

                memoryManager =
                        JMX.newMXBeanProxy(mbsc,
                        memoryManagerName,
                        MemoryManagerMXBean.class) ;
                System.out.println("getMemoryPoolNames\t\t"
                        + Arrays.deepToString(memoryManager.getMemoryPoolNames()));
                System.out.println("getName\t\t"
                        + memoryManager.getName());
                System.out.println("isValid\t\t"
                        + memoryManager.isValid());
            }

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private final int doMemoryPoolMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- MemoryPoolMXBean") ;

        try {
            ObjectName filterName =
                    new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE
                    + ",*");
            Set<ObjectName> onSet = mbsc.queryNames(filterName, null);

            for (Iterator<ObjectName> iter = onSet.iterator(); iter.hasNext(); ) {
                ObjectName memoryPoolName = iter.next() ;
                System.out.println("-------- " + memoryPoolName) ;
                MBeanInfo mbInfo = mbsc.getMBeanInfo(memoryPoolName);
                errorCount += checkNonEmpty(mbInfo);
                System.out.println("getMBeanInfo\t\t" + mbInfo);
                MemoryPoolMXBean memoryPool = null;

                memoryPool =
                        JMX.newMXBeanProxy(mbsc,
                        memoryPoolName,
                        MemoryPoolMXBean.class,
                        true) ;
                System.out.println("getCollectionUsage\t\t"
                        + memoryPool.getCollectionUsage());
                System.out.println("getMemoryManagerNames\t\t"
                        + Arrays.deepToString(memoryPool.getMemoryManagerNames()));
                System.out.println("getName\t\t"
                        + memoryPool.getName());
                System.out.println("getPeakUsage\t\t"
                        + memoryPool.getPeakUsage());
                System.out.println("getType\t\t"
                        + memoryPool.getType());
                System.out.println("getUsage\t\t"
                        + memoryPool.getUsage());
                System.out.println("isValid\t\t"
                        + memoryPool.isValid());
                boolean supported = memoryPool.isUsageThresholdSupported() ;
                System.out.println("isUsageThresholdSupported\t\t"
                        + supported);

                if ( supported ) {
                    System.out.println("getUsageThreshold\t\t"
                            + memoryPool.getUsageThreshold());
                    System.out.println("isUsageThresholdExceeded\t\t"
                            + memoryPool.isUsageThresholdExceeded());
                    System.out.println("getUsageThresholdCount\t\t"
                            + memoryPool.getUsageThresholdCount());
                }

                supported = memoryPool.isCollectionUsageThresholdSupported() ;
                System.out.println("isCollectionUsageThresholdSupported\t\t"
                        + supported);

                if ( supported ) {
                    System.out.println("getCollectionUsageThreshold\t\t"
                            + memoryPool.getCollectionUsageThreshold());
                    System.out.println("getCollectionUsageThresholdCount\t\t"
                            + memoryPool.getCollectionUsageThresholdCount());
                    System.out.println("isCollectionUsageThresholdExceeded\t\t"
                            + memoryPool.isCollectionUsageThresholdExceeded());
                }

                memoryPool.resetPeakUsage();
            }

            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }


    private int checkNonEmpty(MBeanInfo mbi) {
        if ( mbi.toString().length() == 0 ) {
            System.out.println("(ERROR) MBeanInfo is empty !");
            return 1;
        } else {
            return 0;
        }
    }

}
