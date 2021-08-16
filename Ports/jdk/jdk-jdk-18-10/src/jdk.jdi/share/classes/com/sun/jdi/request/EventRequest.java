/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi.request;

import com.sun.jdi.Mirror;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VMOutOfMemoryException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDisconnectEvent;

/**
 * Represents a request for notification of an event.  Examples include
 * {@link BreakpointRequest} and {@link ExceptionRequest}.
 * When an event occurs for which an enabled request is present,
 * an  {@link EventSet EventSet} will
 * be placed on the {@link EventQueue EventQueue}.
 * The collection of existing event requests is
 * managed by the {@link EventRequestManager}.
 * <p>
 * The number of events generated for an event request can be controlled
 * through filters. Filters provide additional constraints that an event
 * must satisfy before it is placed on the event queue. Multiple filters can
 * be used by making multiple calls to filter addition methods such as
 * {@link ExceptionRequest#addClassFilter(java.lang.String classPattern)}.
 * Filters are added to an event one at a time only while the event is
 * disabled. Multiple filters are applied with CUT-OFF AND, in the order
 * it was added to the request. Only events that satisfy all filters are
 * placed in the event queue.
 * <p>
 * The set of available filters is dependent on the event request,
 * some examples of filters are:
 * <ul>
 * <li>Thread filters allow control over the thread for which events are
 * generated.
 * <li>Class filters allow control over the class in which the event
 * occurs.
 * <li>Instance filters allow control over the instance in which
 * the event occurs.
 * <li>Count filters allow control over the number of times an event
 * is reported.
 * </ul>
 * Filters can dramatically improve debugger performance by reducing the
 * amount of event traffic sent from the target VM to the debugger VM.
 * <p>
 * Any method on {@code EventRequest} which
 * takes {@code EventRequest} as an parameter may throw
 * {@link VMDisconnectedException} if the target VM is
 * disconnected and the {@link VMDisconnectEvent} has been or is
 * available to be read from the {@link EventQueue}.
 * <p>
 * Any method on {@code EventRequest} which
 * takes {@code EventRequest} as an parameter may throw
 * {@link VMOutOfMemoryException} if the target VM has run out of memory.
 *
 * @see BreakpointEvent
 * @see EventQueue
 * @see EventRequestManager
 *
 * @author Robert Field
 * @since  1.3
 */
public interface EventRequest extends Mirror {

    /**
     * Determines if this event request is currently enabled.
     *
     * @return {@code true} if enabled;
     * {@code false} otherwise.
     */
    boolean isEnabled();

    /**
     * Enables or disables this event request. While this event request is
     * disabled, the event request will be ignored and the target VM
     * will not be stopped if any of its threads reaches the
     * event request.  Disabled event requests still exist,
     * and are included in event request lists such as
     * {@link EventRequestManager#breakpointRequests()}.
     *
     * @param val {@code true} if the event request is to be enabled;
     * {@code false} otherwise.
     * @throws InvalidRequestStateException if this request
     * has been deleted.
     * @throws IllegalThreadStateException if this is a StepRequest,
     * {@code val} is {@code true}, and the
     * thread named in the request has died or is not yet started.
     */
    void setEnabled(boolean val);

    /**
     * Same as {@link #setEnabled setEnabled(true)}.
     * @throws InvalidRequestStateException if this request
     * has been deleted.
     * @throws IllegalThreadStateException if this is a StepRequest
     * and the thread named in the request has died or is not yet started.
     */
    void enable();

    /**
     * Same as {@link #setEnabled setEnabled(false)}.
     * @throws InvalidRequestStateException if this request
     * has been deleted.
     */
    void disable();

    /**
     * Limit the requested event to be reported at most once after a
     * given number of occurrences.  The event is not reported
     * the first {@code count - 1} times this filter is reached.
     * To request a one-off event, call this method with a count of 1.
     * <p>
     * Once the count reaches 0, any subsequent filters in this request
     * are applied. If none of those filters cause the event to be
     * suppressed, the event is reported. Otherwise, the event is not
     * reported. In either case subsequent events are never reported for
     * this request.
     *
     * @param count the number of ocurrences before generating an event.
     * @throws InvalidRequestStateException if this request is currently
     * enabled or has been deleted.
     * Filters may be added only to disabled requests.
     * @throws IllegalArgumentException if {@code count}
     * is less than one.
     */
    void addCountFilter(int count);

    /** Suspend no threads when the event occurs */
    int SUSPEND_NONE = 0;
    /** Suspend only the thread which generated the event when the event occurs */
    int SUSPEND_EVENT_THREAD = 1;
    /** Suspend all threads when the event occurs */
    int SUSPEND_ALL = 2;

    /**
     * Determines the threads to suspend when the requested event occurs
     * in the target VM. Use {@link #SUSPEND_ALL} to suspend all
     * threads in the target VM (the default). Use {@link #SUSPEND_EVENT_THREAD}
     * to suspend only the thread which generated the event. Use
     * {@link #SUSPEND_NONE} to suspend no threads.
     * <p>
     * Thread suspensions through events have the same functionality
     * as explicitly requested suspensions. See
     * {@link ThreadReference#suspend} and
     * {@link VirtualMachine#suspend} for details.
     *
     * @param policy the selected suspend policy.
     * @throws InvalidRequestStateException if this request is currently
     * enabled or has been deleted.
     * Suspend policy may only be set in disabled requests.
     * @throws IllegalArgumentException if the policy argument
     * contains an illegal value.
     */
    void setSuspendPolicy(int policy);

    /**
     * Returns a value which describes the threads to suspend when the
     * requested event occurs in the target VM.
     * The returned value is  {@link #SUSPEND_ALL},
     * {@link #SUSPEND_EVENT_THREAD}, or {@link #SUSPEND_NONE}.
     *
     * @return the current suspend mode for this request
     */
    int suspendPolicy();

    /**
     * Add an arbitrary key/value "property" to this request.
     * The property can be used by a client of the JDI to
     * associate application information with the request;
     * These client-set properties are not used internally
     * by the JDI.
     * <p>
     * The {@code get/putProperty} methods provide access to
     * a small per-instance map. This is <b>not</b> to be confused
     * with {@link java.util.Properties}.
     * <p>
     * If value is null this method will remove the property.
     *
     * @see #getProperty
     */
    void putProperty(Object key, Object value);

    /**
     * Returns the value of the property with the specified key.  Only
     * properties added with {@link #putProperty} will return
     * a non-null value.
     *
     * @return the value of this property or null
     * @see #putProperty
     */
    Object getProperty(Object key);
}
