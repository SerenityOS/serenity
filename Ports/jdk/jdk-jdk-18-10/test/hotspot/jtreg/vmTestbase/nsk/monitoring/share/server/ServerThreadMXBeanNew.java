/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share.server;

import javax.management.*;
import com.sun.management.*;

/**
 * com.sun.management.ThreadMXBean implementation that delegates functionality to MBeanServer.
 */
public class ServerThreadMXBeanNew extends ServerThreadMXBean implements ThreadMXBean{

    public ServerThreadMXBeanNew(MBeanServer mbeanServer) {
        super(mbeanServer);
    }

    public long[] getThreadUserTime(long[] ids) {
        return (long[]) invokeMethod("getThreadUserTime",
                new Object[] { ids },
                new String[] { long[].class.getName() });
    }

    public long[] getThreadCpuTime(long[] ids) {
        return (long[]) invokeMethod("getThreadCpuTime",
                new Object[] { ids },
                new String[] { long[].class.getName() });
    }

    public long[] getThreadAllocatedBytes(long[] ids) {
        return (long[]) invokeMethod("getThreadAllocatedBytes",
                new Object[] { ids },
                new String[] { long[].class.getName() });
    }

    public long getThreadAllocatedBytes(long id) {
        return (Long) invokeMethod("getThreadAllocatedBytes",
            new Object[] { id },
            new String[] { long.class.getName() });
    }

    public long getCurrentThreadAllocatedBytes() {
        return getLongAttribute("CurrentThreadAllocatedBytes");
    }

    public void setThreadAllocatedMemoryEnabled(boolean enabled) {
        setBooleanAttribute("ThreadAllocatedMemoryEnabled", enabled);
    }

    public boolean isThreadAllocatedMemorySupported() {
        return getBooleanAttribute("ThreadAllocatedMemorySupported");
    }

    public boolean isThreadAllocatedMemoryEnabled() {
        return getBooleanAttribute("ThreadAllocatedMemoryEnabled");
    }
}
