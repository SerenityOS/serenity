/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.management;

import javax.management.ObjectName;

/**
 * A platform managed object is a {@linkplain javax.management.MXBean JMX MXBean}
 * for monitoring and managing a component in the Java platform.
 * Each platform managed object has a unique
 * <a href="ManagementFactory.html#MXBean">object name</a>
 * for the {@linkplain ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} access.
 * All platform MXBeans will implement this interface.
 *
 * <p>
 * Note:
 * The platform MXBean interfaces (i.e. all subinterfaces
 * of {@code PlatformManagedObject}) are implemented
 * by the Java platform only.  New methods may be added in these interfaces
 * in future Java SE releases.
 * In addition, this {@code PlatformManagedObject} interface is only
 * intended for the management interfaces for the platform to extend but
 * not for applications.
 *
 * @see ManagementFactory
 * @since 1.7
 */
public interface PlatformManagedObject {
    /**
     * Returns an {@link ObjectName ObjectName} instance representing
     * the object name of this platform managed object.
     *
     * @return an {@link ObjectName ObjectName} instance representing
     * the object name of this platform managed object.
     */
    public ObjectName getObjectName();
}
