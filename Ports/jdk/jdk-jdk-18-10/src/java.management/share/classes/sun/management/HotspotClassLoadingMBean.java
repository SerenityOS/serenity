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

import java.lang.management.ClassLoadingMXBean;
import sun.management.counter.Counter;

/**
 * Hotspot internal management interface for the class loading system.
 *
 * This management interface is internal and uncommitted
 * and subject to change without notice.
 */
public interface HotspotClassLoadingMBean {
    /**
     * Returns the amount of memory in bytes occupied by loaded classes
     * in the Java virtual machine.
     *
     * @return the amount of memory in bytes occupied by loaded classes
     * in the Java virtual machine.
     */
    public long getLoadedClassSize();

    /**
     * Returns the number of bytes that the Java virtual machine
     * collected due to class unloading.
     *
     * @return the number of bytes that the VM collected due to
     * class unloading.
     */
    public long getUnloadedClassSize();

    /**
     * Returns the accumulated elapsed time spent by class loading
     * in milliseconds.
     *
     * @return the accumulated elapsed time spent by class loading
     * in milliseconds.
     */
    public long getClassLoadingTime();

    /**
     * Returns the amount of memory in bytes occupied by the method
     * data.
     *
     * @return the amount of memory in bytes occupied by the method
     * data.
     */
    public long getMethodDataSize();

    /**
     * Returns the number of classes for which initializers were run.
     *
     * @return the number of classes for which initializers were run.
     */
    public long getInitializedClassCount();

    /**
     * Returns the accumulated elapsed time spent in class initializers
     * in milliseconds.
     *
     * @return the accumulated elapsed time spent in class initializers
     * in milliseconds.
     */
    public long getClassInitializationTime();

    /**
     * Returns the accumulated elapsed time spent in class verifier
     * in milliseconds.
     *
     * @return the accumulated elapsed time spent in class verifier
     * in milliseconds.
     */
    public long getClassVerificationTime();

    /**
     * Returns a list of internal counters maintained in the Java
     * virtual machine for the class loading system.
     *
     * @return a list of internal counters maintained in the VM
     * for the class loading system.
     */
    public java.util.List<Counter> getInternalClassLoadingCounters();

}
