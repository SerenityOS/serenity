/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.monitor;

import java.util.List;

import sun.jvmstat.monitor.event.VmListener;

/**
 * Interface for interacting with a monitorable Java Virtual Machine.
 * The MonitoredVm interface provides methods for discovery of exported
 * instrumentation, for attaching event listeners, and for overall
 * maintenance of the connection to the target.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public interface MonitoredVm {

    /**
     * Get the VmIdentifier associated with this MonitoredVm
     *
     * @return VmIdentifier - the fully resolved Vm identifier associated
     *                        with this MonitoredVm.
     */
    VmIdentifier getVmIdentifier();

    /**
     * Find a named Instrumentation object.
     *
     * This method will look for the named instrumentation object in the
     * instrumentation exported by this Java Virtual Machine. If an
     * instrumentation object with the given name exists, a Monitor interface
     * to that object will be return. Otherwise, the method returns
     * {@code null}.
     *
     * @param name the name of the Instrumentation object to find.
     * @return Monitor - the {@link Monitor} object that can be used to
     *                   monitor the named instrumentation object, or
     *                   {@code null} if the named object doesn't exist.
     * @throws MonitorException Thrown if an error occurs while communicating
     *                          with the target Java Virtual Machine.
     */
    Monitor findByName(String name) throws MonitorException;

    /**
     * Find all Instrumentation objects with names matching the given pattern.
     *
     * This method returns a {@link List} of Monitor objects such that
     * the name of each object matches the given pattern.
     *
     * @param patternString a string containing a pattern as described in
     *                      {@link java.util.regex.Pattern}.
     * @return {@code List<Monitor>} - a List of {@link Monitor}
     *                objects that can be used to
     *                monitor the instrumentation objects whose names match
     *                the given pattern. If no instrumentation objects have
     *                names matching the given pattern, then an empty List
     *                is returned.
     * @throws MonitorException Thrown if an error occurs while communicating
     *                          with the target Java Virtual Machine.
     * @see java.util.regex.Pattern
     */
    List<Monitor> findByPattern(String patternString) throws MonitorException;

    /**
     * Detach from target Java Virtual Machine.
     *
     * After calling this method, updates of the instrumentation data values
     * may be halted. All event notifications are halted. Further interactions
     * with this object should be avoided.
     */
    void detach();


    /* ---- Methods to support polled MonitoredVm Implementations ---- */

    /**
     * Set the polling interval to {@code interval} milliseconds.
     *
     * Polling based monitoring implementations need to refresh the
     * instrumentation data on a periodic basis. This interface allows
     * the interval to override the implementation specific default
     * interval.
     *
     * @param interval the polling interval in milliseconds
     */
    void setInterval(int interval);

    /**
     * Get the polling interval.
     *
     * @return int - the current polling interval in milliseconds.
     * @see #setInterval
     */
    int getInterval();

    /**
     * Set the last exception encountered while polling this MonitoredVm.
     *
     * Polling implementations may choose to poll asynchronously. This
     * method allows an asynchronous task to communicate any polling related
     * exceptions with the application. When an a non-null exception is reported
     * through this interface, the MonitoredVm instance is considered to
     * be in the <em>errored</em> state.
     *
     * @param cause the exception to record.
     * @see #isErrored
     */
    void setLastException(Exception cause);

    /**
     * Get the last exception encountered while polling this MonitoredVm.
     *
     * Returns the last exception observed by the implementation dependent
     * polling task or {@code null} if no such error has occurred.
     *
     * @return Exception - the last exception that occurred during polling
     *                     or {@code null} if no error condition exists.
     * @see #isErrored
     * @see #setLastException
     */
    Exception getLastException();

    /**
     * Clear the last exception.
     *
     * Calling this method will clear the <em>errored</em> state of this
     * MonitoredVm. However, there is no guarantee that clearing the
     * the errored state return the asynchronous polling task to an
     * operational state.
     *
     */
    void clearLastException();

    /**
     * Test if this MonitoredVm is in the errored state.
     * The errored state exists only if an error was reported with
     * call to {@link #setLastException} and only if the parameter to
     * that call was non-null and no subsequent calls are made to
     * {@link #clearLastException}.
     *
     * @return boolean - true if the instance has a non-null error condition
     *                   set, false otherwise.
     *
     * @see #setLastException
     * @see #getLastException
     */
    boolean isErrored();

    /**
     * Add a VmListener. The given listener is added to the list of
     * VmListener objects to be notified of MonitoredVm related events.
     *
     * @param listener the VmListener to add.
     * @throws MonitorException Thrown if any problems occur while attempting
     *                          to add this listener.
     */
    void addVmListener(VmListener listener) throws MonitorException;

    /**
     * Remove a VmListener. The given listener is removed from the list of
     * VmListener objects to be notified of MonitoredVm related events.
     *
     * @param listener the VmListener to be removed.
     * @throws MonitorException Thrown if any problems occur while attempting
     *                            to remove this listener.
     */
    void removeVmListener(VmListener listener) throws MonitorException;
}
