/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.management;

import java.lang.management.ThreadInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

/**
 * A CompositeData for ThreadInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class ThreadInfoCompositeData extends LazyCompositeData {
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private final ThreadInfo threadInfo;
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private final CompositeData cdata;

    private ThreadInfoCompositeData(ThreadInfo ti) {
        this.threadInfo = ti;
        this.cdata = null;
    }

    private ThreadInfoCompositeData(CompositeData cd) {
        this.threadInfo = null;
        this.cdata = cd;
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    public static ThreadInfoCompositeData getInstance(CompositeData cd) {
        validateCompositeData(cd);
        return new ThreadInfoCompositeData(cd);
    }

    public static CompositeData toCompositeData(ThreadInfo ti) {
        ThreadInfoCompositeData ticd = new ThreadInfoCompositeData(ti);
        return ticd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // Convert StackTraceElement[] to CompositeData[]
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        CompositeData[] stackTraceData = new CompositeData[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement ste = stackTrace[i];
            stackTraceData[i] = StackTraceElementCompositeData.toCompositeData(ste);
        }

        // Convert MonitorInfo[] and LockInfo[] to CompositeData[]
        CompositeData lockInfoData =
            LockInfoCompositeData.toCompositeData(threadInfo.getLockInfo());

        // Convert LockInfo[] and MonitorInfo[] to CompositeData[]
        LockInfo[] lockedSyncs = threadInfo.getLockedSynchronizers();
        CompositeData[] lockedSyncsData = new CompositeData[lockedSyncs.length];
        for (int i = 0; i < lockedSyncs.length; i++) {
            LockInfo li = lockedSyncs[i];
            lockedSyncsData[i] = LockInfoCompositeData.toCompositeData(li);
        }

        MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
        CompositeData[] lockedMonitorsData = new CompositeData[lockedMonitors.length];
        for (int i = 0; i < lockedMonitors.length; i++) {
            MonitorInfo mi = lockedMonitors[i];
            lockedMonitorsData[i] = MonitorInfoCompositeData.toCompositeData(mi);
        }

        // values may be null; can't use Map.of
        Map<String,Object> items = new HashMap<>();
        items.put(THREAD_ID,        threadInfo.getThreadId());
        items.put(THREAD_NAME,      threadInfo.getThreadName());
        items.put(THREAD_STATE,     threadInfo.getThreadState().name());
        items.put(BLOCKED_TIME,     threadInfo.getBlockedTime());
        items.put(BLOCKED_COUNT,    threadInfo.getBlockedCount());
        items.put(WAITED_TIME,      threadInfo.getWaitedTime());
        items.put(WAITED_COUNT,     threadInfo.getWaitedCount());
        items.put(LOCK_INFO,        lockInfoData);
        items.put(LOCK_NAME,        threadInfo.getLockName());
        items.put(LOCK_OWNER_ID,    threadInfo.getLockOwnerId());
        items.put(LOCK_OWNER_NAME,  threadInfo.getLockOwnerName());
        items.put(STACK_TRACE,      stackTraceData);
        items.put(SUSPENDED,        threadInfo.isSuspended());
        items.put(IN_NATIVE,        threadInfo.isInNative());
        items.put(LOCKED_MONITORS,  lockedMonitorsData);
        items.put(LOCKED_SYNCS,     lockedSyncsData);
        items.put(DAEMON,           threadInfo.isDaemon());
        items.put(PRIORITY,         threadInfo.getPriority());

        try {
            return new CompositeDataSupport(ThreadInfoCompositeTypes.ofVersion(RUNTIME_VERSION), items);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    // Attribute names
    private static final String THREAD_ID       = "threadId";
    private static final String THREAD_NAME     = "threadName";
    private static final String THREAD_STATE    = "threadState";
    private static final String BLOCKED_TIME    = "blockedTime";
    private static final String BLOCKED_COUNT   = "blockedCount";
    private static final String WAITED_TIME     = "waitedTime";
    private static final String WAITED_COUNT    = "waitedCount";
    private static final String LOCK_INFO       = "lockInfo";
    private static final String LOCK_NAME       = "lockName";
    private static final String LOCK_OWNER_ID   = "lockOwnerId";
    private static final String LOCK_OWNER_NAME = "lockOwnerName";
    private static final String STACK_TRACE     = "stackTrace";
    private static final String SUSPENDED       = "suspended";
    private static final String IN_NATIVE       = "inNative";
    private static final String DAEMON          = "daemon";
    private static final String PRIORITY        = "priority";
    private static final String LOCKED_MONITORS = "lockedMonitors";
    private static final String LOCKED_SYNCS    = "lockedSynchronizers";

    private static final String[] V5_ATTRIBUTES = {
        THREAD_ID,
        THREAD_NAME,
        THREAD_STATE,
        BLOCKED_TIME,
        BLOCKED_COUNT,
        WAITED_TIME,
        WAITED_COUNT,
        LOCK_NAME,
        LOCK_OWNER_ID,
        LOCK_OWNER_NAME,
        STACK_TRACE,
        SUSPENDED,
        IN_NATIVE
    };

    private static final String[] V6_ATTRIBUTES = {
        LOCK_INFO,
        LOCKED_MONITORS,
        LOCKED_SYNCS,
    };

    private static final String[] V9_ATTRIBUTES = {
        DAEMON,
        PRIORITY,
    };

    public long threadId() {
        return getLong(cdata, THREAD_ID);
    }

    public String threadName() {
        // The ThreadName item cannot be null so we check that
        // it is present with a non-null value.
        String name = getString(cdata, THREAD_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Invalid composite data: " +
                "Attribute " + THREAD_NAME + " has null value");
        }
        return name;
    }

    public Thread.State threadState() {
        return Thread.State.valueOf(getString(cdata, THREAD_STATE));
    }

    public long blockedTime() {
        return getLong(cdata, BLOCKED_TIME);
    }

    public long blockedCount() {
        return getLong(cdata, BLOCKED_COUNT);
    }

    public long waitedTime() {
        return getLong(cdata, WAITED_TIME);
    }

    public long waitedCount() {
        return getLong(cdata, WAITED_COUNT);
    }

    public String lockName() {
        // The LockName and LockOwnerName can legitimately be null,
        // we don't bother to check the value
        return getString(cdata, LOCK_NAME);
    }

    public long lockOwnerId() {
        return getLong(cdata, LOCK_OWNER_ID);
    }

    public String lockOwnerName() {
        return getString(cdata, LOCK_OWNER_NAME);
    }

    public boolean suspended() {
        return getBoolean(cdata, SUSPENDED);
    }

    public boolean inNative() {
        return getBoolean(cdata, IN_NATIVE);
    }

    /*
     * if daemon attribute is not present, default to false.
     */
    public boolean isDaemon() {
        return cdata.containsKey(DAEMON) ? getBoolean(cdata, DAEMON) : false;
    }

    /*
     * if priority attribute is not present, default to norm priority.
     */
    public int getPriority(){
        return cdata.containsKey(PRIORITY) ? getInt(cdata, PRIORITY) : Thread.NORM_PRIORITY;
    }

    public StackTraceElement[] stackTrace() {
        CompositeData[] stackTraceData =
            (CompositeData[]) cdata.get(STACK_TRACE);

        // The StackTrace item cannot be null, but if it is we will get
        // a NullPointerException when we ask for its length.
        StackTraceElement[] stackTrace =
            new StackTraceElement[stackTraceData.length];
        for (int i = 0; i < stackTraceData.length; i++) {
            CompositeData cdi = stackTraceData[i];
            stackTrace[i] = StackTraceElementCompositeData.from(cdi);
        }
        return stackTrace;
    }

    /*
     * lockInfo is a new attribute added in JDK 6 ThreadInfo
     * If cd is a 5.0 version, construct the LockInfo object
     * from the lockName value.
     */
    public LockInfo lockInfo() {
        if (cdata.containsKey(LOCK_INFO)) {
            CompositeData lockInfoData = (CompositeData) cdata.get(LOCK_INFO);
            return LockInfo.from(lockInfoData);
        } else {
            String lockName = lockName();
            LockInfo lock = null;
            if (lockName != null) {
                String result[] = lockName.split("@");
                if (result.length == 2) {
                    int identityHashCode = Integer.parseInt(result[1], 16);
                    lock = new LockInfo(result[0], identityHashCode);
                }
            }
            return lock;
        }
    }

    /**
     * Returns an empty array if locked_monitors attribute is not present.
     */
    public MonitorInfo[] lockedMonitors() {
        if (!cdata.containsKey(LOCKED_MONITORS)) {
            return new MonitorInfo[0];
        }

        CompositeData[] lockedMonitorsData =
            (CompositeData[]) cdata.get(LOCKED_MONITORS);

        // The LockedMonitors item cannot be null, but if it is we will get
        // a NullPointerException when we ask for its length.
        MonitorInfo[] monitors =
            new MonitorInfo[lockedMonitorsData.length];
        for (int i = 0; i < lockedMonitorsData.length; i++) {
            CompositeData cdi = lockedMonitorsData[i];
            monitors[i] = MonitorInfo.from(cdi);
        }
        return monitors;
    }

    /**
     * Returns an empty array if locked_monitors attribute is not present.
     */
    public LockInfo[] lockedSynchronizers() {
        if (!cdata.containsKey(LOCKED_SYNCS)) {
            return new LockInfo[0];
        }

        CompositeData[] lockedSyncsData =
            (CompositeData[]) cdata.get(LOCKED_SYNCS);

        // The LockedSynchronizers item cannot be null, but if it is we will
        // get a NullPointerException when we ask for its length.
        LockInfo[] locks = new LockInfo[lockedSyncsData.length];
        for (int i = 0; i < lockedSyncsData.length; i++) {
            CompositeData cdi = lockedSyncsData[i];
            locks[i] = LockInfo.from(cdi);
        }
        return locks;
    }

    /**
     * Validate if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        CompositeType type = cd.getCompositeType();
        int version;
        if (Arrays.stream(V9_ATTRIBUTES).anyMatch(type::containsKey)) {
            version = Runtime.version().feature();
        } else if (Arrays.stream(V6_ATTRIBUTES).anyMatch(type::containsKey)) {
            version = 6;
        } else {
            version = 5;
        }

        if (!isTypeMatched(ThreadInfoCompositeTypes.ofVersion(version), type)) {
            throw new IllegalArgumentException(
                "Unexpected composite type for ThreadInfo of version " + version);
        }
    }

    static final int RUNTIME_VERSION =  Runtime.version().feature();
    static class ThreadInfoCompositeTypes {
        static final Map<Integer, CompositeType> compositeTypes = initCompositeTypes();
        /*
         * Returns CompositeType of the given runtime version
         */
        static CompositeType ofVersion(int version) {
            return compositeTypes.get(version);
        }

        static Map<Integer, CompositeType> initCompositeTypes() {
            Map<Integer, CompositeType> types = new HashMap<>();
            CompositeType ctype = initCompositeType();
            types.put(RUNTIME_VERSION, ctype);
            types.put(5, initV5CompositeType(ctype));
            types.put(6, initV6CompositeType(ctype));
            return types;
        }

        static CompositeType initCompositeType() {
            try {
                return (CompositeType)MappedMXBeanType.toOpenType(ThreadInfo.class);
            } catch (OpenDataException e) {
                // Should never reach here
                throw new AssertionError(e);
            }
        }

        static CompositeType initV5CompositeType(CompositeType threadInfoCompositeType) {
            try {
                OpenType<?>[] v5Types = new OpenType<?>[V5_ATTRIBUTES.length];
                for (int i = 0; i < v5Types.length; i++) {
                    String name = V5_ATTRIBUTES[i];
                    v5Types[i] = name.equals(STACK_TRACE)
                        ? new ArrayType<>(1, StackTraceElementCompositeData.v5CompositeType())
                        : threadInfoCompositeType.getType(name);
                }
                return new CompositeType("ThreadInfo",
                                         "JDK 5 ThreadInfo",
                                         V5_ATTRIBUTES,
                                         V5_ATTRIBUTES,
                                         v5Types);
            } catch (OpenDataException e) {
                // Should never reach here
                throw new AssertionError(e);
            }
        }

        static CompositeType initV6CompositeType(CompositeType threadInfoCompositeType) {
            try {
                String[] v6Names = Stream.of(V5_ATTRIBUTES, V6_ATTRIBUTES)
                    .flatMap(Arrays::stream).toArray(String[]::new);
                OpenType<?>[] v6Types = new OpenType<?>[v6Names.length];
                for (int i = 0; i < v6Names.length; i++) {
                    String name = v6Names[i];
                    OpenType<?> ot = threadInfoCompositeType.getType(name);
                    if (name.equals(STACK_TRACE)) {
                        ot = new ArrayType<>(1, StackTraceElementCompositeData.v5CompositeType());
                    } else if (name.equals(LOCKED_MONITORS)) {
                        ot = new ArrayType<>(1, MonitorInfoCompositeData.v6CompositeType());
                    }
                    v6Types[i] = ot;
                }
                return new CompositeType("ThreadInfo",
                                         "JDK 6 ThreadInfo",
                                         v6Names,
                                         v6Names,
                                         v6Types);
            } catch (OpenDataException e) {
                // Should never reach here
                throw new AssertionError(e);
            }
        }
    }
    private static final long serialVersionUID = 2464378539119753175L;
}
