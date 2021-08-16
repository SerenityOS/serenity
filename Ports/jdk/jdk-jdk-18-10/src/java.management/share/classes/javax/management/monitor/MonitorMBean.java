/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.monitor;

// jmx imports
//
import javax.management.ObjectName;

/**
 * Exposes the remote management interface of monitor MBeans.
 *
 *
 * @since 1.5
 */
public interface MonitorMBean {

    /**
     * Starts the monitor.
     */
    public void start();

    /**
     * Stops the monitor.
     */
    public void stop();

    // GETTERS AND SETTERS
    //--------------------

    /**
     * Adds the specified object in the set of observed MBeans.
     *
     * @param object The object to observe.
     * @exception java.lang.IllegalArgumentException the specified object is null.
     *
     */
    public void addObservedObject(ObjectName object) throws java.lang.IllegalArgumentException;

    /**
     * Removes the specified object from the set of observed MBeans.
     *
     * @param object The object to remove.
     *
     */
    public void removeObservedObject(ObjectName object);

    /**
     * Tests whether the specified object is in the set of observed MBeans.
     *
     * @param object The object to check.
     * @return <CODE>true</CODE> if the specified object is in the set, <CODE>false</CODE> otherwise.
     *
     */
    public boolean containsObservedObject(ObjectName object);

    /**
     * Returns an array containing the objects being observed.
     *
     * @return The objects being observed.
     *
     */
    public ObjectName[] getObservedObjects();

    /**
     * Gets the object name of the object being observed.
     *
     * @return The object being observed.
     *
     * @see #setObservedObject
     *
     * @deprecated As of JMX 1.2, replaced by {@link #getObservedObjects}
     */
    @Deprecated
    public ObjectName getObservedObject();

    /**
     * Sets the object to observe identified by its object name.
     *
     * @param object The object to observe.
     *
     * @see #getObservedObject
     *
     * @deprecated As of JMX 1.2, replaced by {@link #addObservedObject}
     */
    @Deprecated
    public void setObservedObject(ObjectName object);

    /**
     * Gets the attribute being observed.
     *
     * @return The attribute being observed.
     *
     * @see #setObservedAttribute
     */
    public String getObservedAttribute();

    /**
     * Sets the attribute to observe.
     *
     * @param attribute The attribute to observe.
     *
     * @see #getObservedAttribute
     */
    public void setObservedAttribute(String attribute);

    /**
     * Gets the granularity period (in milliseconds).
     *
     * @return The granularity period.
     *
     * @see #setGranularityPeriod
     */
    public long getGranularityPeriod();

    /**
     * Sets the granularity period (in milliseconds).
     *
     * @param period The granularity period.
     * @exception java.lang.IllegalArgumentException The granularity
     * period is less than or equal to zero.
     *
     * @see #getGranularityPeriod
     */
    public void setGranularityPeriod(long period) throws java.lang.IllegalArgumentException;

    /**
     * Tests if the monitor MBean is active.
     * A monitor MBean is marked active when the {@link #start start} method is called.
     * It becomes inactive when the {@link #stop stop} method is called.
     *
     * @return <CODE>true</CODE> if the monitor MBean is active, <CODE>false</CODE> otherwise.
     */
    public boolean isActive();
}
