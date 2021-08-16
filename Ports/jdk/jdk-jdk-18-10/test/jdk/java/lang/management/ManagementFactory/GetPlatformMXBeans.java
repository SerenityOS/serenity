/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6610094 7024172
 * @summary Basic unit test of ManagementFactory.getPlatformMXBean(s)
 *          methods and PlatformManagedObject.getObjectName()
 * @author  Mandy Chung
 *
 * @run main GetPlatformMXBeans
 */

import java.lang.management.*;
import java.io.IOException;
import java.util.*;
import javax.management.*;

import static java.lang.management.ManagementFactory.*;

public class GetPlatformMXBeans {
    private static MBeanServer platformMBeanServer =
            getPlatformMBeanServer();
    public static void main(String[] argv) throws Exception {
        // singleton platform MXBean
        checkPlatformMXBean(getClassLoadingMXBean(),
                            ClassLoadingMXBean.class,
                            CLASS_LOADING_MXBEAN_NAME);
        checkPlatformMXBean(getCompilationMXBean(),
                            CompilationMXBean.class,
                            COMPILATION_MXBEAN_NAME);
        checkPlatformMXBean(getMemoryMXBean(),
                            MemoryMXBean.class,
                            MEMORY_MXBEAN_NAME);
        checkPlatformMXBean(getOperatingSystemMXBean(),
                            OperatingSystemMXBean.class,
                            OPERATING_SYSTEM_MXBEAN_NAME);
        checkPlatformMXBean(getRuntimeMXBean(),
                            RuntimeMXBean.class,
                            RUNTIME_MXBEAN_NAME);
        checkPlatformMXBean(getThreadMXBean(),
                            ThreadMXBean.class,
                            THREAD_MXBEAN_NAME);

        // the following MXBean can have more than one instances
        checkGarbageCollectorMXBeans(getGarbageCollectorMXBeans());
        checkMemoryManagerMXBeans(getMemoryManagerMXBeans());
        checkMemoryPoolMXBeans(getMemoryPoolMXBeans());

        // check invalid platform MXBean
        checkInvalidPlatformMXBean();
    }

    private static <T extends PlatformManagedObject>
            void checkPlatformMXBean(T obj, Class<T> mxbeanInterface,
                                     String mxbeanName)
        throws Exception
    {
        // getPlatformMXBean may return null if the mxbean is not implemented
        PlatformManagedObject mxbean = getPlatformMXBean(mxbeanInterface);
        if (obj != mxbean) {
            throw new RuntimeException("Singleton MXBean returned not matched");
        }

        int numElements = obj == null ? 0 : 1;
        List<? extends PlatformManagedObject> mxbeans =
            getPlatformMXBeans(mxbeanInterface);
        if (mxbeans.size() != numElements) {
            throw new RuntimeException("Unmatched number of platform MXBeans "
                + mxbeans.size() + ". Expected = " + numElements);
        }

        if (obj != null) {
            if (obj != mxbeans.get(0)) {
                throw new RuntimeException("The list returned by getPlatformMXBeans"
                    + " not matched");
            }
            ObjectName on = new ObjectName(mxbeanName);
            if (!on.equals(mxbean.getObjectName())) {
                throw new RuntimeException("Unmatched ObjectName " +
                    mxbean.getObjectName() + " Expected = " + on);
            }
            checkRemotePlatformMXBean(obj, platformMBeanServer,
                                      mxbeanInterface, mxbeanName);
        }
    }

    // verify platform MXBeans in the platform MBeanServer
    private static <T extends PlatformManagedObject>
            void checkRemotePlatformMXBean(T obj,
                                           MBeanServerConnection mbs,
                                           Class<T> mxbeanInterface,
                                           String mxbeanName)
        throws Exception
    {
        PlatformManagedObject mxbean = getPlatformMXBean(mbs, mxbeanInterface);
        if ((obj == null && mxbean != null) || (obj != null && mxbean == null)) {
            throw new RuntimeException("Singleton MXBean returned not matched");
        }

        int numElements = obj == null ? 0 : 1;
        List<? extends PlatformManagedObject> mxbeans =
            getPlatformMXBeans(mbs, mxbeanInterface);
        if (mxbeans.size() != numElements) {
            throw new RuntimeException("Unmatched number of platform MXBeans "
                + mxbeans.size() + ". Expected = " + numElements);
        }

        ObjectName on = new ObjectName(mxbeanName);
        if (!on.equals(mxbean.getObjectName())) {
            throw new RuntimeException("Unmatched ObjectName " +
                mxbean.getObjectName() + " Expected = " + on);
        }
    }

    private static void checkMemoryManagerMXBeans(List<MemoryManagerMXBean> objs)
        throws Exception
    {
        checkPlatformMXBeans(objs, MemoryManagerMXBean.class);
        for (MemoryManagerMXBean mxbean : objs) {
            String domainAndType;
            if (mxbean instanceof GarbageCollectorMXBean) {
                domainAndType = GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE;
            } else {
                domainAndType = MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE;
            }
            ObjectName on = new ObjectName(domainAndType +
                                           ",name=" + mxbean.getName());
            if (!on.equals(mxbean.getObjectName())) {
                throw new RuntimeException("Unmatched ObjectName " +
                    mxbean.getObjectName() + " Expected = " + on);
            }
        }
    }
    private static void checkMemoryPoolMXBeans(List<MemoryPoolMXBean> objs)
        throws Exception
    {
        checkPlatformMXBeans(objs, MemoryPoolMXBean.class);
        for (MemoryPoolMXBean mxbean : objs) {
            ObjectName on = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE +
                                           ",name=" + mxbean.getName());
            if (!on.equals(mxbean.getObjectName())) {
                throw new RuntimeException("Unmatched ObjectName " +
                    mxbean.getObjectName() + " Expected = " + on);
            }
        }
    }

    private static void checkGarbageCollectorMXBeans(List<GarbageCollectorMXBean> objs)
        throws Exception
    {
        checkPlatformMXBeans(objs, GarbageCollectorMXBean.class);
        for (GarbageCollectorMXBean mxbean : objs) {
            ObjectName on = new ObjectName(GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE +
                                           ",name=" + mxbean.getName());
            if (!on.equals(mxbean.getObjectName())) {
                throw new RuntimeException("Unmatched ObjectName " +
                    mxbean.getObjectName() + " Expected = " + on);
            }
        }
    }

    private static <T extends PlatformManagedObject>
        void checkPlatformMXBeans(List<T> objs, Class<T> mxbeanInterface)
            throws Exception
    {
        try {
            getPlatformMXBean(mxbeanInterface);
            // mxbeanInterface is not a singleton
            throw new RuntimeException(mxbeanInterface + ": not a singleton MXBean");
        } catch (IllegalArgumentException e) {
            // expect IAE
        }

        // verify local list of platform MXBeans
        List<? extends PlatformManagedObject> mxbeans =
            getPlatformMXBeans(mxbeanInterface);
        if (objs.size() != mxbeans.size()) {
            throw new RuntimeException("Unmatched number of platform MXBeans "
                + mxbeans.size() + ". Expected = " + objs.size());
        }
        List<T> list = new ArrayList<T>(objs);
        for (PlatformManagedObject pmo : mxbeans) {
            if (list.contains(pmo)) {
                list.remove(pmo);
            } else {
                throw new RuntimeException(pmo +
                    " not in the platform MXBean list");
            }
        }

        if (!list.isEmpty()) {
            throw new RuntimeException("The list returned by getPlatformMXBeans"
                + " not matched");
        }

        // verify platform MXBeans in the platform MBeanServer
        mxbeans = getPlatformMXBeans(platformMBeanServer, mxbeanInterface);
        if (objs.size() != mxbeans.size()) {
            throw new RuntimeException("Unmatched number of platform MXBeans "
                + mxbeans.size() + ". Expected = " + objs.size());
        }
    }

    interface FakeMXBean extends PlatformManagedObject {};

    private static void checkInvalidPlatformMXBean() throws IOException {
        try {
            getPlatformMXBean(FakeMXBean.class);
            // mxbeanInterface is not a singleton
            throw new RuntimeException("Expect IllegalArgumentException but not thrown");
        } catch (IllegalArgumentException e) {
            // expect IAE
        }

        try {
            getPlatformMXBeans(FakeMXBean.class);
            // mxbeanInterface is not a singleton
            throw new RuntimeException("Expect IllegalArgumentException but not thrown");
        } catch (IllegalArgumentException e) {
            // expect IAE
        }

        try {
            getPlatformMXBean(platformMBeanServer, FakeMXBean.class);
            // mxbeanInterface is not a singleton
            throw new RuntimeException("Expect IllegalArgumentException but not thrown");
        } catch (IllegalArgumentException e) {
            // expect IAE
        }

        try {
            getPlatformMXBeans(platformMBeanServer, FakeMXBean.class);
            // mxbeanInterface is not a singleton
            throw new RuntimeException("Expect IllegalArgumentException but not thrown");
        } catch (IllegalArgumentException e) {
            // expect IAE
        }
    }
}
