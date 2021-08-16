/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import nsk.monitoring.share.*;
import javax.management.*;
import java.lang.management.*;

/**
 * MemoryMXBean implementation that delegates functionality to MBeanServer.
 */
public class ServerMemoryMXBean extends ServerMXBean implements MemoryMXBean {
        private static final String GC = "gc";
        private static final String HEAP_MEMORY_USAGE = "MemoryUsage";
        private static final String NONHEAP_MEMORY_USAGE = "MemoryUsage";
        private static final String OBJECT_PENDING_FINALIZATION_COUNT = "ObjectsPendingFinalizationCount";
        private static final String VERBOSE = "Verbose";

        public ServerMemoryMXBean(MBeanServer mbeanServer) {
                super(mbeanServer, ManagementFactory.MEMORY_MXBEAN_NAME);
        }

        public void gc() {
                invokeVoidMethod(GC);
        }

        public MemoryUsage getHeapMemoryUsage() {
                return getMemoryUsageAttribute(HEAP_MEMORY_USAGE);
        }

        public MemoryUsage getNonHeapMemoryUsage() {
                return getMemoryUsageAttribute(NONHEAP_MEMORY_USAGE);
        }

        public int getObjectPendingFinalizationCount() {
                return getIntAttribute(OBJECT_PENDING_FINALIZATION_COUNT);
        }

        public boolean isVerbose() {
                return getBooleanAttribute(VERBOSE);
        }

        public void setVerbose(boolean verbose) {
                setBooleanAttribute(VERBOSE, verbose);
        }
}
