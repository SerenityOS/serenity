/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import javax.management.ObjectName;

/**
 * Implementation class for the runtime subsystem.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getRuntimeMXBean() returns an instance
 * of this class.
 */
class RuntimeImpl implements RuntimeMXBean {

    private final VMManagement jvm;
    private final long vmStartupTime;

    /**
     * Constructor of RuntimeImpl class.
     */
    RuntimeImpl(VMManagement vm) {
        this.jvm = vm;
        this.vmStartupTime = jvm.getStartupTime();
    }

    public String getName() {
        return jvm.getVmId();
    }

    public String getManagementSpecVersion() {
        return jvm.getManagementVersion();
    }

    public String getVmName() {
        return jvm.getVmName();
    }

    public String getVmVendor() {
        return jvm.getVmVendor();
    }

    public String getVmVersion() {
        return jvm.getVmVersion();
    }

    public String getSpecName() {
        return jvm.getVmSpecName();
    }

    public String getSpecVendor() {
        return jvm.getVmSpecVendor();
    }

    public String getSpecVersion() {
        return jvm.getVmSpecVersion();
    }

    public String getClassPath() {
        return jvm.getClassPath();
    }

    public String getLibraryPath() {
        return jvm.getLibraryPath();
    }

    public String getBootClassPath() {
        throw new UnsupportedOperationException(
            "Boot class path mechanism is not supported");
    }

    public List<String> getInputArguments() {
        Util.checkMonitorAccess();
        return jvm.getVmArguments();
    }

    public long getUptime() {
        return jvm.getUptime();
    }

    public long getStartTime() {
        return vmStartupTime;
    }

    public boolean isBootClassPathSupported() {
        return false;
    }

    public Map<String,String> getSystemProperties() {
        Properties sysProps = System.getProperties();
        Map<String,String> map = new HashMap<>();

        // Properties.entrySet() does not include the entries in
        // the default properties.  So use Properties.stringPropertyNames()
        // to get the list of property keys including the default ones.
        Set<String> keys = sysProps.stringPropertyNames();
        for (String k : keys) {
            String value = sysProps.getProperty(k);
            map.put(k, value);
        }

        return map;
    }

    public ObjectName getObjectName() {
        return Util.newObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
    }

}
