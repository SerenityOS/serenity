/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

import sun.management.counter.*;

/**
 * Implementation class of HotspotClassLoadingMBean interface.
 *
 * Internal, uncommitted management interface for Hotspot class loading
 * system.
 */
class HotspotClassLoading
    implements HotspotClassLoadingMBean {

    private VMManagement jvm;

    /**
     * Constructor of HotspotClassLoading class.
     */
    HotspotClassLoading(VMManagement vm) {
        jvm = vm;
    }

    public long getLoadedClassSize() {
        return jvm.getLoadedClassSize();
    }

    public long getUnloadedClassSize() {
        return jvm.getUnloadedClassSize();
    }

    public long getClassLoadingTime() {
        return jvm.getClassLoadingTime();
    }

    public long getMethodDataSize() {
        return jvm.getMethodDataSize();
    }

    public long getInitializedClassCount() {
        return jvm.getInitializedClassCount();
    }

    public long getClassInitializationTime() {
        return jvm.getClassInitializationTime();
    }

    public long getClassVerificationTime() {
        return jvm.getClassVerificationTime();
    }

    // Performance counter support
    private static final String JAVA_CLS    = "java.cls.";
    private static final String COM_SUN_CLS = "com.sun.cls.";
    private static final String SUN_CLS     = "sun.cls.";
    private static final String CLS_COUNTER_NAME_PATTERN =
        JAVA_CLS + "|" + COM_SUN_CLS + "|" + SUN_CLS;

    public java.util.List<Counter> getInternalClassLoadingCounters() {
        return jvm.getInternalCounters(CLS_COUNTER_NAME_PATTERN);
    }
}
