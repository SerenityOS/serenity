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

import java.util.List;

import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.Mirror;
import com.sun.jdi.NativeMethodException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.VMDeathEvent;

/**
 * Manages the creation and deletion of {@link EventRequest}s. A single
 * implementor of this interface exists in a particular VM and
 * is accessed through {@link VirtualMachine#eventRequestManager()}
 *
 * @see EventRequest
 * @see Event
 * @see BreakpointRequest
 * @see BreakpointEvent
 * @see VirtualMachine
 *
 * @author Robert Field
 * @since  1.3
 */

public interface EventRequestManager extends Mirror {

    /**
     * Creates a new disabled {@link ClassPrepareRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @return the created {@link ClassPrepareRequest}
     */
    ClassPrepareRequest createClassPrepareRequest();

    /**
     * Creates a new disabled {@link ClassUnloadRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @return the created {@link ClassUnloadRequest}
     */
    ClassUnloadRequest createClassUnloadRequest();

    /**
     * Creates a new disabled {@link ThreadStartRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @return the created {@link ThreadStartRequest}
     */
    ThreadStartRequest createThreadStartRequest();

    /**
     * Creates a new disabled {@link ThreadDeathRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @return the created {@link ThreadDeathRequest}
     */
    ThreadDeathRequest createThreadDeathRequest();

    /**
     * Creates a new disabled {@link ExceptionRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     * <P>
     * A specific exception type and its subclasses can be selected
     * for exception events. Caught exceptions,  uncaught exceptions,
     * or both can be selected. Note, however, that
     * at the time an exception is thrown, it is not always
     * possible to determine whether it is truly caught. See
     * {@link ExceptionEvent#catchLocation} for
     * details.
     * @param refType If non-null, specifies that exceptions which are
     *                instances of refType will be reported. Note: this
     *                will include instances of sub-types.  If null,
     *                all instances will be reported
     * @param notifyCaught If true, caught exceptions will be reported.
     * @param notifyUncaught If true, uncaught exceptions will be reported.
     *
     * @return the created {@link ExceptionRequest}
     */
    ExceptionRequest createExceptionRequest(ReferenceType refType,
                                            boolean notifyCaught,
                                            boolean notifyUncaught);

    /**
     * Creates a new disabled {@link MethodEntryRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @return the created {@link MethodEntryRequest}
     */
    MethodEntryRequest createMethodEntryRequest();

    /**
     * Creates a new disabled {@link MethodExitRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @return the created {@link MethodExitRequest}
     */
    MethodExitRequest createMethodExitRequest();

     /**
     * Creates a new disabled {@link MonitorContendedEnterRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canRequestMonitorEvents()}
     * to determine if the operation is supported.
     *
     * @return the created {@link MonitorContendedEnterRequest}
     * @throws java.lang.UnsupportedOperationException if
     * the target VM does not support this
     * operation.
     *
     * @since 1.6
     */
    MonitorContendedEnterRequest createMonitorContendedEnterRequest();

    /**
     * Creates a new disabled {@link MonitorContendedEnteredRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canRequestMonitorEvents()}
     * to determine if the operation is supported.
     *
     * @return the created {@link MonitorContendedEnteredRequest}
     * @throws java.lang.UnsupportedOperationException if
     * the target VM does not support this
     * operation.
     *
     * @since 1.6
     */

    MonitorContendedEnteredRequest createMonitorContendedEnteredRequest();

    /**
     * Creates a new disabled {@link MonitorWaitRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canRequestMonitorEvents()}
     * to determine if the operation is supported.
     *
     * @return the created {@link MonitorWaitRequest}
     * @throws java.lang.UnsupportedOperationException if
     * the target VM does not support this
     * operation.
     *
     * @since 1.6
     */
    MonitorWaitRequest createMonitorWaitRequest();

    /**
     * Creates a new disabled {@link MonitorWaitedRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canRequestMonitorEvents()}
     * to determine if the operation is supported.
     *
     * @return the created {@link MonitorWaitedRequest}
     * @throws java.lang.UnsupportedOperationException if
     * the target VM does not support this
     * operation.
     *
     * @since 1.6
     */
    MonitorWaitedRequest createMonitorWaitedRequest();

    /**
     * Creates a new disabled {@link StepRequest}.
     * The new event request is added to the list managed by this
     * EventRequestManager. Use {@link EventRequest#enable()} to
     * activate this event request.
     * <p>
     * The returned request will control stepping only in the specified
     * {@code thread}; all other threads will be unaffected.
     * A {@code size} value of {@link StepRequest#STEP_MIN} will generate a
     * step event each time the code index changes. It represents the
     * smallest step size available and often maps to the instruction
     * level.
     * A {@code size} value of {@link StepRequest#STEP_LINE} will generate a
     * step event each time the source line changes unless line number information is not available,
     * in which case a STEP_MIN will be done instead.  For example, no line number information is
     * available during the execution of a method that has been rendered obsolete by
     * by a {@link VirtualMachine#redefineClasses} operation.
     * A {@code depth} value of {@link StepRequest#STEP_INTO} will generate
     * step events in any called methods.  A {@code depth} value
     * of {@link StepRequest#STEP_OVER} restricts step events to the current frame
     * or caller frames. A {@code depth} value of {@link StepRequest#STEP_OUT}
     * restricts step events to caller frames only. All depth
     * restrictions are relative to the call stack immediately before the
     * step takes place.
     * <p>
     * Only one pending step request is allowed per thread.
     * <p>
     * Note that a typical debugger will want to cancel stepping
     * after the first step is detected.  Thus a next line method
     * would do the following:
     * <pre>{@code
     *     EventRequestManager mgr = myVM.{@link VirtualMachine#eventRequestManager eventRequestManager}();
     *     StepRequest request = mgr.createStepRequest(myThread,
     *                                                 StepRequest.{@link StepRequest#STEP_LINE STEP_LINE},
     *                                                 StepRequest.{@link StepRequest#STEP_OVER STEP_OVER});
     *     request.{@link EventRequest#addCountFilter addCountFilter}(1);  // next step only
     *     request.enable();
     *     myVM.{@link VirtualMachine#resume resume}();
     * }</pre>
     *
     * @param thread the thread in which to step
     * @param depth the step depth
     * @param size the step size
     * @return the created {@link StepRequest}
     * @throws DuplicateRequestException if there is already a pending
     * step request for the specified thread.
     * @throws IllegalArgumentException if the size or depth arguments
     * contain illegal values.
     */
    StepRequest createStepRequest(ThreadReference thread,
                                  int size,
                                  int depth);

    /**
     * Creates a new disabled {@link BreakpointRequest}.
     * The given {@link Location} must have a valid
     * (that is, non-negative) code index. The new
     * breakpoint is added to the list managed by this
     * EventRequestManager. Multiple breakpoints at the
     * same location are permitted. Use {@link EventRequest#enable()} to
     * activate this event request.
     *
     * @param location the location of the new breakpoint.
     * @return the created {@link BreakpointRequest}
     * @throws NativeMethodException if location is within a native method.
     */
    BreakpointRequest createBreakpointRequest(Location location);

    /**
     * Creates a new disabled watchpoint which watches accesses to the
     * specified field. The new
     * watchpoint is added to the list managed by this
     * EventRequestManager. Multiple watchpoints on the
     * same field are permitted.
     * Use {@link EventRequest#enable()} to
     * activate this event request.
     * <P>
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canWatchFieldAccess()}
     * to determine if the operation is supported.
     *
     * @param field the field to watch
     * @return the created watchpoint
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation.
     */
    AccessWatchpointRequest createAccessWatchpointRequest(Field field);

    /**
     * Creates a new disabled watchpoint which watches accesses to the
     * specified field. The new
     * watchpoint is added to the list managed by this
     * EventRequestManager. Multiple watchpoints on the
     * same field are permitted.
     * Use {@link EventRequest#enable()} to
     * activate this event request.
     * <P>
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canWatchFieldModification()}
     * to determine if the operation is supported.
     *
     * @param field the field to watch
     * @return the created watchpoint
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation.
     */
    ModificationWatchpointRequest createModificationWatchpointRequest(Field field);

    /**
     * Creates a new disabled {@link VMDeathRequest}.
     * The new request is added to the list managed by this
     * EventRequestManager.
     * Use {@link EventRequest#enable()} to
     * activate this event request.
     * <P>
     * This request (if enabled) will cause a
     * {@link VMDeathEvent}
     * to be sent on termination of the target VM.
     * <P>
     * A VMDeathRequest with a suspend policy of
     * {@link EventRequest#SUSPEND_ALL SUSPEND_ALL}
     * can be used to assure processing of incoming
     * {@link EventRequest#SUSPEND_NONE SUSPEND_NONE} or
     * {@link EventRequest#SUSPEND_EVENT_THREAD SUSPEND_EVENT_THREAD}
     * events before VM death.  If all event processing is being
     * done in the same thread as event sets are being read,
     * enabling the request is all that is needed since the VM
     * will be suspended until the {@link EventSet}
     * containing the {@link VMDeathEvent}
     * is resumed.
     * <P>
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canRequestVMDeathEvent()}
     * to determine if the operation is supported.
     *
     * @return the created request
     * @throws java.lang.UnsupportedOperationException if
     * the target VM does not support this
     * operation.
     *
     * @since 1.4
     */
    VMDeathRequest createVMDeathRequest();

    /**
     * Removes an eventRequest. The eventRequest is disabled and
     * the removed from the requests managed by this
     * EventRequestManager. Once the eventRequest is deleted, no
     * operations (for example, {@link EventRequest#setEnabled})
     * are permitted - attempts to do so will generally cause an
     * {@link InvalidRequestStateException}.
     * No other eventRequests are effected.
     * <P>
     * Because this method changes the underlying lists of event
     * requests, attempting to directly delete from a list returned
     * by a request accessor (e.g. below):
     * <PRE>
     *   Iterator iter = requestManager.stepRequests().iterator();
     *   while (iter.hasNext()) {
     *      requestManager.deleteEventRequest(iter.next());
     *  }
     * </PRE>
     * may cause a {@link java.util.ConcurrentModificationException}.
     * Instead use
     * {@link #deleteEventRequests(List) deleteEventRequests(List)}
     * or copy the list before iterating.
     *
     * @param eventRequest the eventRequest to remove
     */
    void deleteEventRequest(EventRequest eventRequest);

    /**
     * Removes a list of {@link EventRequest}s.
     *
     * @see #deleteEventRequest(EventRequest)
     *
     * @param eventRequests the list of eventRequests to remove
     */
    void deleteEventRequests(List<? extends EventRequest> eventRequests);

    /**
     * Remove all breakpoints managed by this EventRequestManager.
     *
     * @see #deleteEventRequest(EventRequest)
     */
    void deleteAllBreakpoints();

    /**
     * Return an unmodifiable list of the enabled and disabled step requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link StepRequest} objects.
     */
    List<StepRequest> stepRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled class prepare requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link ClassPrepareRequest} objects.
     */
    List<ClassPrepareRequest> classPrepareRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled class unload requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link ClassUnloadRequest} objects.
     */
    List<ClassUnloadRequest> classUnloadRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled thread start requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link ThreadStartRequest} objects.
     */
    List<ThreadStartRequest> threadStartRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled thread death requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link ThreadDeathRequest} objects.
     */
    List<ThreadDeathRequest> threadDeathRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled exception requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link ExceptionRequest} objects.
     */
    List<ExceptionRequest> exceptionRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled breakpoint requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link BreakpointRequest} objects.
     */
    List<BreakpointRequest> breakpointRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled access
     * watchpoint requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link AccessWatchpointRequest} objects.
     */
    List<AccessWatchpointRequest> accessWatchpointRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled modification
     * watchpoint requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the all {@link ModificationWatchpointRequest} objects.
     */
    List<ModificationWatchpointRequest> modificationWatchpointRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled method entry requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link MethodEntryRequest} objects.
     */
    List<MethodEntryRequest> methodEntryRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled method exit requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link MethodExitRequest} objects.
     */
    List<MethodExitRequest> methodExitRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled monitor contended enter requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link MonitorContendedEnterRequest} objects.
     *
     * @since 1.6
     */
    List<MonitorContendedEnterRequest> monitorContendedEnterRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled monitor contended entered requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link MonitorContendedEnteredRequest} objects.
     *
     * @since 1.6
     */
    List<MonitorContendedEnteredRequest> monitorContendedEnteredRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled monitor wait requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link MonitorWaitRequest} objects.
     *
     * @since 1.6
     */
    List<MonitorWaitRequest> monitorWaitRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled monitor waited requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * @return the list of all {@link MonitorWaitedRequest} objects.
     *
     * @since 1.6
     */
    List<MonitorWaitedRequest> monitorWaitedRequests();

    /**
     * Return an unmodifiable list of the enabled and disabled VM death requests.
     * This list is a live view of these requests and thus changes as requests
     * are added and deleted.
     * Note: the unsolicited VMDeathEvent does not have a
     * corresponding request.
     * @return the list of all {@link VMDeathRequest} objects.
     *
     * @since 1.4
     */
    List<VMDeathRequest> vmDeathRequests();
}
