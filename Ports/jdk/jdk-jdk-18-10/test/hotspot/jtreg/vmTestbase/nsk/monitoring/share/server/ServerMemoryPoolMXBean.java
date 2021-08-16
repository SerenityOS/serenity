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
 * MemoryPoolMXBean implementation that delegates functionality to MBeanServer.
 */
public class ServerMemoryPoolMXBean extends ServerMXBean implements MemoryPoolMXBean {
        public ServerMemoryPoolMXBean(MBeanServer mbeanServer, String name) {
                super(mbeanServer, name);
        }

        public ServerMemoryPoolMXBean(MBeanServer mbeanServer, ObjectName name) {
                super(mbeanServer, name);
        }

        public MemoryUsage getCollectionUsage() {
                return getMemoryUsageAttribute("CollectionUsage");
        }

        public long getCollectionUsageThreshold() {
                return getLongAttribute("CollectionUsageThreshold");
        }

        public long getCollectionUsageThresholdCount() {
                return getLongAttribute("CollectionUsageThresholdCount");
        }

        public String[] getMemoryManagerNames() {
                return getStringArrayAttribute("MemoryManagerNames");
        }

        public String getName() {
                return getStringAttribute("Name");
        }

        public MemoryUsage getPeakUsage() {
                return getMemoryUsageAttribute("PeakUsage");
        }

        public MemoryType getType() {
                return getMemoryTypeAttribute("MemoryType");
        }

        public MemoryUsage getUsage() {
                return getMemoryUsageAttribute("Usage");
        }

        public long getUsageThreshold() {
                return getLongAttribute("UsageThreshhold");
        }

        public long getUsageThresholdCount() {
                return getLongAttribute("UsageThreshholdCount");
        }

        public boolean isCollectionUsageThresholdExceeded() {
                return getBooleanAttribute("CollectionUsageThresholdExceeded");
        }

        public boolean isCollectionUsageThresholdSupported() {
                return getBooleanAttribute("CollectionUsageThresholdSupported");
        }

        public boolean isUsageThresholdExceeded() {
                return getBooleanAttribute("UsageThresholdExceeded");
        }

        public boolean isUsageThresholdSupported() {
                return getBooleanAttribute("UsageThresholdSupported");
        }

        public boolean isValid() {
                return getBooleanAttribute("Valid");
        }

        public void resetPeakUsage() {
                invokeVoidMethod("resetPeakUsage");
        }

        public void setCollectionUsageThreshold(long threshold) {
                setLongAttribute("CollectionUsageThreshold", threshold);
        }

        public void setUsageThreshold(long threshold) {
                setLongAttribute("UsageThreshold", threshold);
        }
}
