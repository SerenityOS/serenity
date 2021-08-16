/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;

import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * Implementation class for a memory manager.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getMemoryManagerMXBeans() returns a list
 * of instances of this class.
 */
class MemoryManagerImpl extends NotificationEmitterSupport
    implements MemoryManagerMXBean {

    private final String  name;
    private final boolean isValid;
    private MemoryPoolMXBean[] pools;

    MemoryManagerImpl(String name) {
        this.name = name;
        this.isValid = true;
        this.pools = null;
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return isValid;
    }

    public String[] getMemoryPoolNames() {
        MemoryPoolMXBean[] ps = getMemoryPools();

        String[] names = new String[ps.length];
        for (int i = 0; i < ps.length; i++) {
            names[i] = ps[i].getName();
        }
        return names;
    }

    synchronized MemoryPoolMXBean[] getMemoryPools() {
        if (pools == null) {
            pools = getMemoryPools0();
        }
        return pools;
    }
    private native MemoryPoolMXBean[] getMemoryPools0();

    private MBeanNotificationInfo[] notifInfo = null;
    public MBeanNotificationInfo[] getNotificationInfo() {
        synchronized (this) {
            if(notifInfo == null) {
                notifInfo = new MBeanNotificationInfo[0];
            }
        }
        return notifInfo;
    }

    public ObjectName getObjectName() {
        return Util.newObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, getName());
    }

}
