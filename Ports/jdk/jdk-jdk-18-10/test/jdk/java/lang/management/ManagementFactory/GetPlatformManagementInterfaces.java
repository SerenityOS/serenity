/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7074616
 * @summary Basic unit test of the
 *          ManagementFactory.getPlatformManagementInterfaces() method
 * @author  Frederic Parain
 *
 * @run main GetPlatformManagementInterfaces
 */

import java.lang.management.*;
import java.io.IOException;
import java.util.*;
import javax.management.*;

import static java.lang.management.ManagementFactory.*;

public class GetPlatformManagementInterfaces {

    private static enum ManagementInterfaces {
        CLASS_LOADING_MXBEAN(ClassLoadingMXBean.class),
        COMPILATION_MXBEAN(CompilationMXBean.class),
        MEMORY_MXBEAN(MemoryMXBean.class),
        OPERATING_SYSTEM_MXBEAN(OperatingSystemMXBean.class),
        RUNTIME_MXBEAN(RuntimeMXBean.class),
        THREAD_MXBEAN(ThreadMXBean.class),
        GARBAGE_COLLECTOR_MXBEAN(GarbageCollectorMXBean.class),
        MEMORY_MANAGER_MXBEAN(MemoryManagerMXBean.class),
        MEMORY_POOL_MXBEAN(MemoryPoolMXBean.class);

        private final Class<? extends PlatformManagedObject> managementInterface;
        private ManagementInterfaces(Class<? extends PlatformManagedObject> minterface) {
            managementInterface = minterface;
        }
        public Class<? extends PlatformManagedObject> getManagementInterface() {
            return managementInterface;
        }
    };

    public static void main(String[] args) {
        Set<Class<? extends PlatformManagedObject>> interfaces =
            ManagementFactory.getPlatformManagementInterfaces();
        for(Class<? extends PlatformManagedObject> pom : interfaces) {
            List<? extends PlatformManagedObject> list =
                ManagementFactory.getPlatformMXBeans(pom);
        }
        for(ManagementInterfaces mi : ManagementInterfaces.values()) {
            if(!interfaces.contains(mi.getManagementInterface())) {
                throw new RuntimeException(mi.getManagementInterface() + " not in ManagementInterfaces set");
            }
        }
    }
}
