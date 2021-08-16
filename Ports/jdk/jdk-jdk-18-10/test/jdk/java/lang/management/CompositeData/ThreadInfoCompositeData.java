/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4982289 8198253
 * @summary Test ThreadInfo.from to return a valid
 *          ThreadInfo object. Or throw exception if
 *          the input CompositeData is invalid.
 * @author  Mandy Chung
 *
 * @modules java.management/sun.management
 * @build ThreadInfoCompositeData OpenTypeConverter
 * @run testng/othervm ThreadInfoCompositeData
 */


import javax.management.openmbean.*;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ThreadInfoCompositeData {
    private static String lockClassName = "myClass";
    private static int lockIdentityHashCode = 123456;
    private static String lockName = lockClassName + '@' +
        Integer.toHexString(lockIdentityHashCode);
    private static LockInfo lockInfo =
        new LockInfo(lockClassName, lockIdentityHashCode);

    @Test
    public static void createGoodCompositeData() throws Exception {
        CompositeData cd = Factory.makeThreadInfoCompositeData();
        ThreadInfo info = ThreadInfo.from(cd);
        checkThreadInfo(info);
    }

    /*
     * An invalid CompositeData with JDK 9 attributes but missing JDK 6 attributes
     */
    @Test
    public static void badMissingCompositeData() throws Exception {
        CompositeData cd = Factory.makeCompositeDataMissingV6();
        try {
            ThreadInfo info = ThreadInfo.from(cd);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
    }

    static final StackTraceElement STE =
        new StackTraceElement("FooClass", "getFoo", "Foo.java", 100);


    /*
     * Current version of ThreadInfo but an older version of StackTraceElement
     */
    @Test
    public static void withV5StackTraceCompositeData() throws Exception {
        CompositeData cd = Factory.makeThreadInfoWithV5StackTrace();
        try {
            ThreadInfo info = ThreadInfo.from(cd);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
    }

    /*
     * Current version of ThreadInfo but an older version of MonitorInfo
     * and the value of "lockedStackFrame" attribute is null.
     */
    @Test
    public static void withInvalidMonitorInfoCompositeData() throws Exception {
        CompositeData cd = Factory.makeThreadInfoWithIncompatibleMonitorInfo();

        // verify MonitorInfo is valid
        CompositeData[] monitors = (CompositeData[])cd.get("lockedMonitors");
        CompositeData ste = (CompositeData)monitors[0].get("lockedStackFrame");
        if (((Integer)monitors[0].get("lockedStackDepth")) >= 0 || ste != null) {
            throw new RuntimeException("Expected negative stack depth and null stack frame");
        }
        MonitorInfo minfo = MonitorInfo.from(monitors[0]);
        checkLockInfo(minfo);
        if (minfo.getLockedStackFrame() != null) {
            throw new RuntimeException("Expected null stack frame");
        }

        try {
            ThreadInfo info = ThreadInfo.from(cd);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
    }

    /*
     * ThreadInfo of version N can accept lockedMonitors of version >= N
     */
    @Test
    public static void withNewMonitorInfoCompositeData() throws Exception {
        CompositeData cd = Factory.makeThreadInfoWithNewMonitorInfo();
        ThreadInfo info = ThreadInfo.from(cd);
        checkThreadInfo(info);
    }

    /*
     * Test CompositeData representing JDK 5 ThreadInfo
     */
    @Test
    public static void createV5ThreadInfo() throws Exception {
        CompositeData cd = Factory.makeThreadInfoV5CompositeData();
        ThreadInfo info = ThreadInfo.from(cd);
        checkThreadInfoV5(info);
    }

    /*
     * Test ThreadInfoCompositeData.toCompositeData
     */
    @Test
    public static void internalToCompositeData() throws Exception {
        CompositeData cd = Factory.makeThreadInfoCompositeData();
        ThreadInfo info = ThreadInfo.from(cd);
        cd = sun.management.ThreadInfoCompositeData.toCompositeData(info);
        info = ThreadInfo.from(cd);
        checkThreadInfo(info);
    }

   static void checkThreadInfoV5(ThreadInfo info) {
       Object[] values = Factory.VALUES;

       if (info.getThreadId() != ((Long) values[THREAD_ID]).longValue()) {
            throw new RuntimeException("Thread Id = " + info.getThreadId() +
               " expected = " + values[THREAD_ID]);
        }
        if (!info.getThreadName().equals(values[THREAD_NAME])) {
            throw new RuntimeException("Thread Name = " +
               info.getThreadName() + " expected = " + values[THREAD_NAME]);
        }
        if (info.getThreadState() != Thread.State.RUNNABLE) {
            throw new RuntimeException("Thread Name = " +
               info.getThreadName() + " expected = " + Thread.State.RUNNABLE);
        }
        if (info.getBlockedTime() != ((Long) values[BLOCKED_TIME]).longValue()) {
            throw new RuntimeException("blocked time = " +
               info.getBlockedTime() +
               " expected = " + values[BLOCKED_TIME]);
        }
        if (info.getBlockedCount() != ((Long) values[BLOCKED_COUNT]).longValue()) {
            throw new RuntimeException("blocked count = " +
               info.getBlockedCount() +
               " expected = " + values[BLOCKED_COUNT]);
        }
        if (info.getWaitedTime() != ((Long) values[WAITED_TIME]).longValue()) {
            throw new RuntimeException("waited time = " +
               info.getWaitedTime() +
               " expected = " + values[WAITED_TIME]);
        }
        if (info.getWaitedCount() != ((Long) values[WAITED_COUNT]).longValue()) {
            throw new RuntimeException("waited count = " +
               info.getWaitedCount() +
               " expected = " + values[WAITED_COUNT]);
        }
        if (!info.getLockName().equals(values[LOCK_NAME])) {
            throw new RuntimeException("Lock Name = " +
               info.getLockName() + " expected = " + values[LOCK_NAME]);
        }
        if (info.getLockOwnerId() !=
                ((Long) values[LOCK_OWNER_ID]).longValue()) {
            throw new RuntimeException(
               "LockOwner Id = " + info.getLockOwnerId() +
               " expected = " + values[LOCK_OWNER_ID]);
        }
        if (!info.getLockOwnerName().equals(values[LOCK_OWNER_NAME])) {
            throw new RuntimeException("LockOwner Name = " +
               info.getLockOwnerName() + " expected = " +
               values[LOCK_OWNER_NAME]);
        }

        checkStackTrace(info.getStackTrace());
        checkLockInfo(info.getLockInfo());
   }

    static void checkThreadInfo(ThreadInfo info) {
        Object[] values = Factory.VALUES;

        checkThreadInfoV5(info);

        if (!values[DAEMON].equals(info.isDaemon())) {
            throw new RuntimeException("Daemon = " +
               info.isDaemon() + " expected = " + values[DAEMON]);
        }
    }

    private static void checkStackTrace(StackTraceElement[] s) {
        if (s.length != 1) {
            throw new RuntimeException("Stack Trace length = " +
                s.length + " expected = 1");
        }

        StackTraceElement s1 = STE;
        StackTraceElement s2 = s[0];

        // these attributes may be null
        if (!Objects.equals(s1.getClassLoaderName(), s2.getClassLoaderName())) {
            throw new RuntimeException("Class loader name = " +
                s2.getClassLoaderName() + " expected = " + s1.getClassLoaderName());
        }
        if (!Objects.equals(s1.getModuleName(), s2.getModuleName())) {
            throw new RuntimeException("Module name = " +
                s2.getModuleName() + " expected = " + s1.getModuleName());
        }
        if (!Objects.equals(s1.getModuleVersion(), s2.getModuleVersion())) {
            throw new RuntimeException("Module version = " +
                s2.getModuleVersion() + " expected = " + s1.getModuleVersion());
        }

        if (!s1.getClassName().equals(s2.getClassName())) {
            throw new RuntimeException("Class name = " +
                s2.getClassName() + " expected = " + s1.getClassName());
        }
        if (!s1.getMethodName().equals(s2.getMethodName())) {
            throw new RuntimeException("Method name = " +
                s2.getMethodName() + " expected = " + s1.getMethodName());
        }
        if (!s1.getFileName().equals(s2.getFileName())) {
            throw new RuntimeException("File name = " +
                s2.getFileName() + " expected = " + s1.getFileName());
        }
        if (s1.getLineNumber() != s2.getLineNumber()) {
            throw new RuntimeException("Line number = " +
                s2.getLineNumber() + " expected = " + s1.getLineNumber());
        }
    }

    private static void checkLockInfo(LockInfo li) {
        if (!li.getClassName().equals(lockInfo.getClassName())) {
            throw new RuntimeException("Class Name = " +
                li.getClassName() + " expected = " + lockInfo.getClassName());
        }
        if (li.getIdentityHashCode() != lockInfo.getIdentityHashCode()) {
            throw new RuntimeException("Class Name = " +
                li.getIdentityHashCode() + " expected = " +
                lockInfo.getIdentityHashCode());
        }
    }

    @Test
    public static void badNameCompositeData() throws Exception {
        CompositeData cd = Factory.makeCompositeDataWithBadNames();
        try {
            ThreadInfo info = ThreadInfo.from(cd);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public static void badTypeCompositeData() throws Exception {
        CompositeData cd = Factory.makeCompositeDataWithBadTypes();

        try {
            ThreadInfo info = ThreadInfo.from(cd);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) { }
    }

    private static final int THREAD_ID = 0;
    private static final int THREAD_NAME = 1;
    private static final int THREAD_STATE = 2;
    private static final int BLOCKED_TIME = 3;
    private static final int BLOCKED_COUNT = 4;
    private static final int WAITED_TIME = 5;
    private static final int WAITED_COUNT = 6;
    private static final int LOCK_NAME = 7;
    private static final int LOCK_OWNER_ID = 8;
    private static final int LOCK_OWNER_NAME = 9;
    private static final int STACK_TRACE = 10;
    private static final int SUSPENDED = 11;
    private static final int IN_NATIVE = 12;
    // JDK 6 ThreadInfo attributes
    private static final int LOCK_INFO = 13;
    private static final int LOCKED_MONITORS = 14;
    private static final int LOCKED_SYNCS = 15;
    // JDK 9 ThreadInfo attributes
    private static final int DAEMON = 16;
    private static final int PRIORITY = 17;

    private static class Factory {

        static final CompositeType STE_COMPOSITE_TYPE;
        static final CompositeType LOCK_INFO_COMPOSITE_TYPE;
        static final CompositeType MONITOR_INFO_COMPOSITE_TYPE;
        static final ArrayType STE_ARRAY_COMPOSITE_TYPE;
        static final ArrayType LOCK_INFO_ARRAY_COMPOSITE_TYPE;
        static final ArrayType MONITOR_INFO_ARRAY_COMPOSITE_TYPE;

        static {
            CompositeType steCType = null;
            CompositeType lockInfoCType = null;
            CompositeType monitorInfoCType = null;
            ArrayType steArrayType = null;
            ArrayType lockInfoArrayType = null;
            ArrayType monitorInfoArrayType = null;

            try {
                steCType = (CompositeType) OpenTypeConverter.toOpenType(StackTraceElement.class);
                lockInfoCType = (CompositeType) OpenTypeConverter.toOpenType(LockInfo.class);
                monitorInfoCType = (CompositeType) OpenTypeConverter.toOpenType(MonitorInfo.class);
                steArrayType = new ArrayType(1, steCType);
                lockInfoArrayType = new ArrayType(1, lockInfoCType);
                monitorInfoArrayType = new ArrayType(1, monitorInfoCType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            STE_COMPOSITE_TYPE = steCType;
            LOCK_INFO_COMPOSITE_TYPE = lockInfoCType;
            MONITOR_INFO_COMPOSITE_TYPE = monitorInfoCType;
            STE_ARRAY_COMPOSITE_TYPE = steArrayType;
            LOCK_INFO_ARRAY_COMPOSITE_TYPE = lockInfoArrayType;
            MONITOR_INFO_ARRAY_COMPOSITE_TYPE = monitorInfoArrayType;
        }

        static CompositeData makeThreadInfoCompositeData() throws OpenDataException {
            CompositeType ct = new CompositeType("MyCompositeType",
                "CompositeType for ThreadInfo",
                ITEM_NAMES,
                ITEM_NAMES,
                ITEM_TYPES);
            return new CompositeDataSupport(ct, ITEM_NAMES, VALUES);
        }

        static CompositeData makeThreadInfoV5CompositeData() throws OpenDataException {
            CompositeType ct = new CompositeType("MyCompositeType",
                "CompositeType for JDK 5 ThreadInfo",
                V5_ITEM_NAMES,
                V5_ITEM_NAMES,
                V5_ITEM_TYPES);
            return new CompositeDataSupport(ct, V5_ITEM_NAMES, V5_VALUES);
        }

        static CompositeData makeCompositeDataWithBadTypes() throws OpenDataException {
            OpenType[] badItemTypes = {
                SimpleType.LONG,
                SimpleType.STRING,
                SimpleType.STRING,
                SimpleType.LONG,
                SimpleType.LONG,
                SimpleType.LONG,
                SimpleType.LONG,
                SimpleType.STRING,
                SimpleType.LONG,
                SimpleType.STRING,
                SimpleType.LONG,  // bad type
                SimpleType.BOOLEAN,
                SimpleType.BOOLEAN,
                SimpleType.LONG,  // bad type
                SimpleType.LONG,  // bad type
                SimpleType.LONG,  // bad type
                SimpleType.BOOLEAN,
                SimpleType.INTEGER,
            };

            CompositeType ct =
                new CompositeType("Bad item types",
                    "CompositeType for ThreadInfo",
                    ITEM_NAMES,
                    ITEM_NAMES,
                    badItemTypes);

            // Copy before mutating to avoid affecting other tests.
            Object[] localValues = VALUES.clone();

            // patch values[STACK_TRACE] to Long
            localValues[STACK_TRACE] = Long.valueOf(1000);
            localValues[LOCK_INFO] = Long.valueOf(1000);
            localValues[LOCKED_MONITORS] = Long.valueOf(1000);
            localValues[LOCKED_SYNCS] = Long.valueOf(1000);
            return new CompositeDataSupport(ct, ITEM_NAMES, localValues);
        }

        static CompositeData makeCompositeDataWithBadNames() throws OpenDataException {
            String[] badItemNames = ITEM_NAMES.clone();
            badItemNames[STACK_TRACE] = "BadStackTrace"; // bad item name

            CompositeType ct = new CompositeType("Bad item names",
                "CompositeType for ThreadInfo",
                badItemNames,
                badItemNames,
                ITEM_TYPES);
            return new CompositeDataSupport(ct,
                badItemNames,
                VALUES);
        }

        /*
         * Create a CompositeData of ThreadInfo without JDK 6 attributes
         */
        static CompositeData makeCompositeDataMissingV6() throws OpenDataException {
            String[] itemNames = concat(V5_ITEM_NAMES, V9_ITEM_NAMES).toArray(String[]::new);
            OpenType[] itemTypes = concat(V5_ITEM_TYPES, V9_ITEM_TYPES).toArray(OpenType[]::new);
            Object[] values = concat(V5_VALUES, V9_VALUES).toArray(Object[]::new);

            CompositeType ct =
                new CompositeType("InvalidCompositeType",
                    "CompositeType for ThreadInfo",
                    itemNames,
                    itemNames,
                    itemTypes);
            return new CompositeDataSupport(ct, itemNames, values);
        }

        static CompositeData makeStackTraceElement() {
            Object[] steValue = {
                STE.getClassLoaderName(),
                STE.getModuleName(),
                STE.getModuleVersion(),
                STE.getClassName(),
                STE.getMethodName(),
                STE.getFileName(),
                Integer.valueOf(STE.getLineNumber()),
                Boolean.valueOf(STE.isNativeMethod()),
            };

            try {
                return new CompositeDataSupport(STE_COMPOSITE_TYPE,
                                                STE_ITEM_NAMES,
                                                steValue);
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        static CompositeData makeStackTraceElementV5() throws OpenDataException {
            CompositeType steV5CType =
                new CompositeType("JDK 5 StackTraceElement",
                    "CompositeType for JDK 5 StackTraceElement",
                    STE_V5_ITEM_NAMES,
                    STE_V5_ITEM_NAMES,
                    STE_V5_ITEM_TYPES);

            Object[] steV5Value = {
                STE.getClassName(),
                STE.getMethodName(),
                STE.getFileName(),
                Integer.valueOf(STE.getLineNumber()),
                Boolean.valueOf(STE.isNativeMethod()),
            };

            return new CompositeDataSupport(steV5CType, STE_V5_ITEM_NAMES, steV5Value);
        }

        /*
         * Create a CompositeData of ThreadInfo without JDK 5 StackTraceElement
         */
        static CompositeData makeThreadInfoWithV5StackTrace() throws OpenDataException {
            OpenType[] badTypes = ITEM_TYPES.clone();
            Object[] badValues = VALUES.clone();

            CompositeData[] stackTrace = new CompositeData[1];
            stackTrace[0] = makeStackTraceElementV5();
            badTypes[STACK_TRACE] = new ArrayType(1, stackTrace[0].getCompositeType());
            badValues[STACK_TRACE] = stackTrace;
            CompositeType ct = new CompositeType("CompositeType",
                "ThreadInfo with JDK 5 StackTraceElement",
                ITEM_NAMES,
                ITEM_NAMES,
                badTypes);
            return new CompositeDataSupport(ct, ITEM_NAMES, badValues);
        }

        /*
         * Create MonitorInfo with JDK 5 StackTraceElement (i.e. JDK 6 MonitorInfo)
         * The value of "lockedStackFrame" attribute is null to ensure that
         * the validation is done.
         */
        static CompositeData makeV6MonitorInfo() throws OpenDataException {
            CompositeData steV5 = makeStackTraceElementV5();

            String[] names = MONITOR_INFO_COMPOSITE_TYPE.keySet().toArray(new String[0]);
            OpenType[] types = new OpenType[names.length];
            for (int i=0; i < names.length; i++) {
                String n = names[i];
                types[i] = "lockedStackFrame".equals(n)
                                ? steV5.getCompositeType()
                                : MONITOR_INFO_COMPOSITE_TYPE.getType(n);
            }

            CompositeType ctype =
                new CompositeType("JDK 6 MonitorInfo",
                    "CompositeType for JDK 6 MonitorInfo",
                    names,
                    names,
                    types);

            Object[] values = {
                lockClassName,
                lockIdentityHashCode,
                -1,
                null
            };

            return new CompositeDataSupport(ctype, names, values);
        }

        /*
         * Create a CompositeData of ThreadInfo with incompatible MonitorInfo
         */
        static CompositeData makeThreadInfoWithIncompatibleMonitorInfo() throws OpenDataException {
            OpenType[] badTypes = ITEM_TYPES.clone();
            Object[] badValues = VALUES.clone();

            CompositeData[] lockedMonitors = new CompositeData[1];
            lockedMonitors[0] = makeV6MonitorInfo();
            badTypes[LOCKED_MONITORS] = new ArrayType(1, lockedMonitors[0].getCompositeType());
            badValues[LOCKED_MONITORS] = lockedMonitors;
            CompositeType ct = new CompositeType("CompositeType",
                "ThreadInfo with incompatible MonitorInfo",
                ITEM_NAMES,
                ITEM_NAMES,
                badTypes);
            return new CompositeDataSupport(ct, ITEM_NAMES, badValues);
        }

        static CompositeData makeNewMonitorInfo() throws OpenDataException {
            String[] names = Stream.concat(MONITOR_INFO_COMPOSITE_TYPE.keySet().stream(),
                                           Stream.of("extra")).toArray(String[]::new);
            OpenType[] types = new OpenType[names.length];
            for (int i=0; i < names.length; i++) {
                String n = names[i];
                types[i] = "extra".equals(n)
                                ? SimpleType.STRING
                                : MONITOR_INFO_COMPOSITE_TYPE.getType(n);
            }

            CompositeType compositeType =
                new CompositeType("JDK X MonitorInfo",
                    "CompositeType for JDK X MonitorInfo",
                    names,
                    names,
                    types);

            Object[] values = {
                lockClassName,
                lockIdentityHashCode,
                Integer.valueOf(1),
                makeStackTraceElement(),
                "extra"
            };

            return new CompositeDataSupport(compositeType, names, values);
        }

        /*
         * Create a CompositeData of ThreadInfo with a newer version of MonitorInfo
         */
        static CompositeData makeThreadInfoWithNewMonitorInfo() throws OpenDataException {
            OpenType[] types = ITEM_TYPES.clone();
            Object[] badValues = VALUES.clone();

            CompositeData[] lockedMonitors = new CompositeData[1];
            lockedMonitors[0] = makeNewMonitorInfo();
            types[LOCKED_MONITORS] = new ArrayType(1, lockedMonitors[0].getCompositeType());
            badValues[LOCKED_MONITORS] = lockedMonitors;
            CompositeType ct = new CompositeType("CompositeType",
                            "ThreadInfo with JDK 5 MonitorInfo",
                            ITEM_NAMES,
                            ITEM_NAMES,
                            types);
            return new CompositeDataSupport(ct, ITEM_NAMES, badValues);
        }

        static CompositeData makeLockInfo() {
            Object[] lockInfoValue = {
                lockInfo.getClassName(),
                lockInfo.getIdentityHashCode(),
            };

            try {
                return new CompositeDataSupport(LOCK_INFO_COMPOSITE_TYPE,
                    LOCK_INFO_ITEM_NAMES,
                    lockInfoValue);
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        static CompositeData[] makeLockedSynchronizers() {
            CompositeData[] lockedSyncs = new CompositeData[1];
            lockedSyncs[0] = makeLockInfo();
            return lockedSyncs;
        }

        static CompositeData[] makeLockedMonitors() {
            CompositeData[] lockedMonitorsCD = new CompositeData[1];

            Object[] lockedMonitorsValue = {
                lockInfo.getClassName(),
                lockInfo.getIdentityHashCode(),
                makeStackTraceElement(),
                Integer.valueOf(1),
            };
            try {
                lockedMonitorsCD[0] =
                    new CompositeDataSupport(MONITOR_INFO_COMPOSITE_TYPE,
                        LOCKED_MONITORS_ITEM_NAMES,
                        lockedMonitorsValue);
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
            return lockedMonitorsCD;
        }

        static final String[] V5_ITEM_NAMES = {
            "threadId",
            "threadName",
            "threadState",
            "blockedTime",
            "blockedCount",
            "waitedTime",
            "waitedCount",
            "lockName",
            "lockOwnerId",
            "lockOwnerName",
            "stackTrace",
            "suspended",
            "inNative",
        };

        static final String[] V6_ITEM_NAMES = {
            "lockInfo",
            "lockedMonitors",
            "lockedSynchronizers",
        };

        static final String[] V9_ITEM_NAMES = {
            "daemon",
            "priority",
        };

        static final OpenType[] V5_ITEM_TYPES = {
            SimpleType.LONG,
            SimpleType.STRING,
            SimpleType.STRING,
            SimpleType.LONG,
            SimpleType.LONG,
            SimpleType.LONG,
            SimpleType.LONG,
            SimpleType.STRING,
            SimpleType.LONG,
            SimpleType.STRING,
            STE_ARRAY_COMPOSITE_TYPE,
            SimpleType.BOOLEAN,
            SimpleType.BOOLEAN,
        };

        static final OpenType[] V6_ITEM_TYPES = {
            LOCK_INFO_COMPOSITE_TYPE,
            MONITOR_INFO_ARRAY_COMPOSITE_TYPE,
            LOCK_INFO_ARRAY_COMPOSITE_TYPE,
        };

        static final OpenType[] V9_ITEM_TYPES = {
            SimpleType.BOOLEAN,
            SimpleType.INTEGER,
        };

        static final String[] STE_ITEM_NAMES = {
            "classLoaderName",
            "moduleName",
            "moduleVersion",
            "className",
            "methodName",
            "fileName",
            "lineNumber",
            "nativeMethod",
        };

        static final String[] STE_V5_ITEM_NAMES = Arrays.copyOfRange(STE_ITEM_NAMES, 3, 8);

        static final OpenType[] STE_V5_ITEM_TYPES = {
            SimpleType.STRING,
            SimpleType.STRING,
            SimpleType.STRING,
            SimpleType.INTEGER,
            SimpleType.BOOLEAN
        };

        static final String[] LOCK_INFO_ITEM_NAMES = {
            "className",
            "identityHashCode",
        };

        static final String[] LOCKED_MONITORS_ITEM_NAMES = {
            LOCK_INFO_ITEM_NAMES[0],
            LOCK_INFO_ITEM_NAMES[1],
            "lockedStackFrame",
            "lockedStackDepth",
        };

        static final Object[] V5_VALUES = {
            Long.valueOf(100),
            "FooThread",
            "RUNNABLE",
            Long.valueOf(200),
            Long.valueOf(10),
            Long.valueOf(300),
            Long.valueOf(20),
            lockName,
            Long.valueOf(99),
            "BarThread",
            new CompositeData[] { makeStackTraceElement() },
            Boolean.valueOf(false),
            Boolean.valueOf(false),
        };

        static final Object[] V6_VALUES = {
            makeLockInfo(),
            makeLockedMonitors(),
            makeLockedSynchronizers(),
        };

        static final Object[] V9_VALUES = {
            Boolean.valueOf(true),
            Thread.NORM_PRIORITY,
        };

        static final String[] ITEM_NAMES =
            concat(V5_ITEM_NAMES, V6_ITEM_NAMES, V9_ITEM_NAMES).toArray(String[]::new);

        static final OpenType[] ITEM_TYPES =
            concat(V5_ITEM_TYPES, V6_ITEM_TYPES, V9_ITEM_TYPES).toArray(OpenType[]::new);

        static final Object[] VALUES =
            concat(V5_VALUES, V6_VALUES, V9_VALUES).toArray(Object[]::new);

        static <T> Stream<T> concat(T[]... streams) {
            return Stream.of(streams).flatMap(a -> Arrays.stream(a));
        }
    }
}
