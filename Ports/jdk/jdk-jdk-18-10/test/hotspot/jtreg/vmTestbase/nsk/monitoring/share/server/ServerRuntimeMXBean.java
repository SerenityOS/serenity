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
import javax.management.MBeanServer;
import java.lang.management.*;

public class ServerRuntimeMXBean extends ServerMXBean implements RuntimeMXBean {
        private static final String BOOT_CLASSPATH= "BootClassPath";
        private static final String CLASSPATH= "ClassPath";
        private static final String INPUT_ARGUMENTS = "InputArguments";
        private static final String LIBRARY_PATH = "LibraryPath";
        private static final String MANAGEMENT_SPEC_VERSION = "ManagementSpecVersion";
        private static final String NAME = "Name";
        private static final String SPEC_NAME = "SpecName";
        private static final String SPEC_VENDOR = "SpecVendor";
        private static final String SPEC_VERSION = "SpecVersion";
        private static final String START_TIME = "StartTime";
        private static final String UPTIME = "Uptime";
        private static final String VM_NAME = "VmName";
        private static final String VM_VENDOR = "VmVendor";
        private static final String VM_VERSION = "VmVersion";
        private static final String BOOT_CLASSPATH_SUPPORTED = "BootClassPathSupported";

        public ServerRuntimeMXBean(MBeanServer mbeanServer) {
                super(mbeanServer, ManagementFactory.RUNTIME_MXBEAN_NAME);
        }

        public String getBootClassPath() {
                return getStringAttribute(BOOT_CLASSPATH);
        }

        public String getClassPath() {
                return getStringAttribute(CLASSPATH);
        }

        public List<String> getInputArguments() {
                throw new UnsupportedOperationException("TODO");
        }

        public String getLibraryPath() {
                return getStringAttribute(LIBRARY_PATH);
        }

        public String getManagementSpecVersion() {
                return getStringAttribute(MANAGEMENT_SPEC_VERSION);
        }

        public String getName() {
                return getStringAttribute(NAME);
        }

        public String getSpecName() {
                return getStringAttribute(SPEC_NAME);
        }

        public String getSpecVendor() {
                return getStringAttribute(SPEC_VENDOR);
        }

        public String getSpecVersion() {
                return getStringAttribute(SPEC_VERSION);
        }

        public long getStartTime() {
                return getLongAttribute(START_TIME);
        }

        public Map<String, String> getSystemProperties() {
                throw new UnsupportedOperationException("TODO");
        }

        public long getUptime() {
                return getLongAttribute(UPTIME);
        }

        public String getVmName() {
                return getStringAttribute(VM_NAME);
        }

        public String getVmVendor() {
                return getStringAttribute(VM_VENDOR);
        }

        public String getVmVersion() {
                return getStringAttribute(VM_VERSION);
        }

        public boolean isBootClassPathSupported() {
                return getBooleanAttribute(BOOT_CLASSPATH_SUPPORTED);
        }
}
