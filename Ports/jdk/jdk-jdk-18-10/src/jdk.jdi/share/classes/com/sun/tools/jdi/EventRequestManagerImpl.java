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

package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.NativeMethodException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
import com.sun.jdi.request.DuplicateRequestException;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.InvalidRequestStateException;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.MonitorContendedEnterRequest;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import com.sun.jdi.request.MonitorWaitRequest;
import com.sun.jdi.request.MonitorWaitedRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;
import com.sun.jdi.request.VMDeathRequest;
import com.sun.jdi.request.WatchpointRequest;

/**
 * This interface is used to create and remove Breakpoints, Watchpoints,
 * etc.
 * It include implementations of all the request interfaces..
 */
// Warnings from List filters and List[] requestLists is  hard to fix.
// Remove SuppressWarning when we fix the warnings from List filters
// and List[] requestLists. The generic array is not supported.
@SuppressWarnings({"unchecked", "rawtypes"})
class EventRequestManagerImpl extends MirrorImpl
                              implements EventRequestManager
{
    private final List<? extends EventRequest>[] requestLists;
    private static int methodExitEventCmd = 0;

    static int JDWPtoJDISuspendPolicy(byte jdwpPolicy) {
        switch(jdwpPolicy) {
            case JDWP.SuspendPolicy.ALL:
                return EventRequest.SUSPEND_ALL;
            case JDWP.SuspendPolicy.EVENT_THREAD:
                return EventRequest.SUSPEND_EVENT_THREAD;
        case JDWP.SuspendPolicy.NONE:
                return EventRequest.SUSPEND_NONE;
            default:
                throw new IllegalArgumentException("Illegal policy constant: " + jdwpPolicy);
        }
    }

    static byte JDItoJDWPSuspendPolicy(int jdiPolicy) {
        switch(jdiPolicy) {
            case EventRequest.SUSPEND_ALL:
                return JDWP.SuspendPolicy.ALL;
            case EventRequest.SUSPEND_EVENT_THREAD:
                return JDWP.SuspendPolicy.EVENT_THREAD;
            case EventRequest.SUSPEND_NONE:
                return JDWP.SuspendPolicy.NONE;
            default:
                throw new IllegalArgumentException("Illegal policy constant: " + jdiPolicy);
        }
    }

    /*
     * Override superclass back to default equality
     */
    public boolean equals(Object obj) {
        return this == obj;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    private abstract class EventRequestImpl extends MirrorImpl implements EventRequest {
        int id;

        /*
         * This list is not protected by a synchronized wrapper. All
         * access/modification should be protected by synchronizing on
         * the enclosing instance of EventRequestImpl.
         */
        List<Object> filters = new ArrayList<>();

        boolean isEnabled = false;
        boolean deleted = false;
        byte suspendPolicy = JDWP.SuspendPolicy.ALL;
        private Map<Object, Object> clientProperties = null;

        EventRequestImpl() {
            super(EventRequestManagerImpl.this.vm);
        }

        /*
         * Override superclass back to default equality
         */
        public boolean equals(Object obj) {
            return this == obj;
        }

        public int hashCode() {
            return System.identityHashCode(this);
        }

        abstract int eventCmd();

        InvalidRequestStateException invalidState() {
            return new InvalidRequestStateException(toString());
        }

        String state() {
            return deleted? " (deleted)" :
                (isEnabled()? " (enabled)" : " (disabled)");
        }

        /**
         * @return all the event request of this kind
         */
        List requestList() {
            return EventRequestManagerImpl.this.requestList(eventCmd());
        }

        /**
         * delete the event request
         */
        void delete() {
            if (!deleted) {
                requestList().remove(this);
                disable(); /* must do BEFORE delete */
                deleted = true;
            }
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public void enable() {
            setEnabled(true);
        }

        public void disable() {
            setEnabled(false);
        }

        public synchronized void setEnabled(boolean val) {
            if (deleted) {
                throw invalidState();
            } else {
                if (val != isEnabled) {
                    if (isEnabled) {
                        clear();
                    } else {
                        set();
                    }
                }
            }
        }

        public synchronized void addCountFilter(int count) {
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            if (count < 1) {
                throw new IllegalArgumentException("count is less than one");
            }
            filters.add(JDWP.EventRequest.Set.Modifier.Count.create(count));
        }

        public void setSuspendPolicy(int policy) {
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            suspendPolicy = JDItoJDWPSuspendPolicy(policy);
        }

        public int suspendPolicy() {
            return JDWPtoJDISuspendPolicy(suspendPolicy);
        }

        /**
         * set (enable) the event request
         */
        synchronized void set() {
            JDWP.EventRequest.Set.Modifier[] mods =
                filters.toArray(
                    new JDWP.EventRequest.Set.Modifier[filters.size()]);
            try {
                id = JDWP.EventRequest.Set.process(vm, (byte)eventCmd(),
                                                   suspendPolicy, mods).requestID;
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
            isEnabled = true;
        }

        synchronized void clear() {
            try {
                JDWP.EventRequest.Clear.process(vm, (byte)eventCmd(), id);
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
            isEnabled = false;
        }

        /**
         * @return a small Map
         * @see #putProperty
         * @see #getProperty
         */
        private Map<Object, Object> getProperties() {
            if (clientProperties == null) {
                clientProperties = new HashMap<>(2);
            }
            return clientProperties;
        }

        /**
         * Returns the value of the property with the specified key.  Only
         * properties added with <code>putProperty</code> will return
         * a non-null value.
         *
         * @return the value of this property or null
         * @see #putProperty
         */
        public final Object getProperty(Object key) {
            if (clientProperties == null) {
                return null;
            } else {
                return getProperties().get(key);
            }
        }

        /**
         * Add an arbitrary key/value "property" to this component.
         *
         * @see #getProperty
         */
        public final void putProperty(Object key, Object value) {
            if (value != null) {
                getProperties().put(key, value);
            } else {
                getProperties().remove(key);
            }
        }
    }

    abstract class ThreadVisibleEventRequestImpl extends EventRequestImpl {
        public synchronized void addThreadFilter(ThreadReference thread) {
            validateMirror(thread);
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            filters.add(JDWP.EventRequest.Set.Modifier.ThreadOnly
                                      .create((ThreadReferenceImpl)thread));
        }
    }

    abstract class ClassVisibleEventRequestImpl
                                  extends ThreadVisibleEventRequestImpl {
        public synchronized void addClassFilter(ReferenceType clazz) {
            validateMirror(clazz);
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            filters.add(JDWP.EventRequest.Set.Modifier.ClassOnly
                                      .create((ReferenceTypeImpl)clazz));
        }

        public synchronized void addClassFilter(String classPattern) {
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            if (classPattern == null) {
                throw new NullPointerException();
            }
            filters.add(JDWP.EventRequest.Set.Modifier.ClassMatch
                                      .create(classPattern));
        }

        public synchronized void addClassExclusionFilter(String classPattern) {
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            if (classPattern == null) {
                throw new NullPointerException();
            }
            filters.add(JDWP.EventRequest.Set.Modifier.ClassExclude
                                      .create(classPattern));
        }

        public synchronized void addInstanceFilter(ObjectReference instance) {
            validateMirror(instance);
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            if (!vm.canUseInstanceFilters()) {
                throw new UnsupportedOperationException(
                     "target does not support instance filters");
            }
            filters.add(JDWP.EventRequest.Set.Modifier.InstanceOnly
                                      .create((ObjectReferenceImpl)instance));
        }
    }

    class BreakpointRequestImpl extends ClassVisibleEventRequestImpl
                                     implements BreakpointRequest {
        private final Location location;

        BreakpointRequestImpl(Location location) {
            this.location = location;
            filters.add(0,JDWP.EventRequest.Set.Modifier.LocationOnly
                                                 .create(location));
            requestList().add(this);
        }

        public Location location() {
            return location;
        }

        int eventCmd() {
            return JDWP.EventKind.BREAKPOINT;
        }

        public String toString() {
            return "breakpoint request " + location() + state();
        }
    }

    class ClassPrepareRequestImpl extends ClassVisibleEventRequestImpl
                                  implements ClassPrepareRequest {
        ClassPrepareRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.CLASS_PREPARE;
        }

        public synchronized void addSourceNameFilter(String sourceNamePattern) {
            if (isEnabled() || deleted) {
                throw invalidState();
            }
            if (!vm.canUseSourceNameFilters()) {
                throw new UnsupportedOperationException(
                     "target does not support source name filters");
            }
            if (sourceNamePattern == null) {
                throw new NullPointerException();
            }

            filters.add(JDWP.EventRequest.Set.Modifier.SourceNameMatch
                                      .create(sourceNamePattern));
        }

        public String toString() {
            return "class prepare request " + state();
        }
    }

    class ClassUnloadRequestImpl extends ClassVisibleEventRequestImpl
                                 implements ClassUnloadRequest {
        ClassUnloadRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.CLASS_UNLOAD;
        }

        public String toString() {
            return "class unload request " + state();
        }
    }

    class ExceptionRequestImpl extends ClassVisibleEventRequestImpl
                               implements ExceptionRequest {
        ReferenceType exception = null;
        boolean caught = true;
        boolean uncaught = true;

        ExceptionRequestImpl(ReferenceType refType,
                          boolean notifyCaught, boolean notifyUncaught) {
            exception = refType;
            caught = notifyCaught;
            uncaught = notifyUncaught;
            {
                ReferenceTypeImpl exc;
                if (exception == null) {
                    exc = new ClassTypeImpl(vm, 0);
                } else {
                    exc = (ReferenceTypeImpl)exception;
                }
                filters.add(JDWP.EventRequest.Set.Modifier.ExceptionOnly.
                            create(exc, caught, uncaught));
            }
            requestList().add(this);
        }

        public ReferenceType exception() {
            return exception;
        }

        public boolean notifyCaught() {
            return caught;
        }

        public boolean notifyUncaught() {
            return uncaught;
        }

        int eventCmd() {
            return JDWP.EventKind.EXCEPTION;
        }

        public String toString() {
            return "exception request " + exception() + state();
        }
    }

    class MethodEntryRequestImpl extends ClassVisibleEventRequestImpl
                                 implements MethodEntryRequest {
        MethodEntryRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.METHOD_ENTRY;
        }

        public String toString() {
            return "method entry request " + state();
        }
    }

    class MethodExitRequestImpl extends ClassVisibleEventRequestImpl
                                implements MethodExitRequest {
        MethodExitRequestImpl() {
            if (methodExitEventCmd == 0) {
                /*
                 * If we can get return values, then we always get them.
                 * Thus, for JDI MethodExitRequests, we always use the
                 * same JDWP EventKind.  Here we decide which to use and
                 * save it so that it will be used for all future
                 * MethodExitRequests.
                 *
                 * This call to canGetMethodReturnValues can't
                 * be done in the EventRequestManager ctor because that is too early.
                 */
                if (vm.canGetMethodReturnValues()) {
                    methodExitEventCmd = JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE;
                } else {
                    methodExitEventCmd = JDWP.EventKind.METHOD_EXIT;
                }
            }
            requestList().add(this);
        }

        int eventCmd() {
            return EventRequestManagerImpl.methodExitEventCmd;
        }

        public String toString() {
            return "method exit request " + state();
        }
    }

    class MonitorContendedEnterRequestImpl extends ClassVisibleEventRequestImpl
                                           implements MonitorContendedEnterRequest {
        MonitorContendedEnterRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.MONITOR_CONTENDED_ENTER;
        }

        public String toString() {
            return "monitor contended enter request " + state();
        }
    }

    class MonitorContendedEnteredRequestImpl extends ClassVisibleEventRequestImpl
                                             implements MonitorContendedEnteredRequest {
        MonitorContendedEnteredRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.MONITOR_CONTENDED_ENTERED;
        }

        public String toString() {
            return "monitor contended entered request " + state();
        }
    }

    class MonitorWaitRequestImpl extends ClassVisibleEventRequestImpl
                                 implements MonitorWaitRequest {
        MonitorWaitRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.MONITOR_WAIT;
        }

        public String toString() {
            return "monitor wait request " + state();
        }
    }

    class MonitorWaitedRequestImpl extends ClassVisibleEventRequestImpl
                                   implements MonitorWaitedRequest {
        MonitorWaitedRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.MONITOR_WAITED;
        }

        public String toString() {
            return "monitor waited request " + state();
        }
    }

    class StepRequestImpl extends ClassVisibleEventRequestImpl
                          implements StepRequest {
        ThreadReferenceImpl thread;
        int size;
        int depth;

        StepRequestImpl(ThreadReference thread, int size, int depth) {
            this.thread = (ThreadReferenceImpl)thread;
            this.size = size;
            this.depth = depth;

            /*
             * Translate size and depth to corresponding JDWP values.
             */
            int jdwpSize;
            switch (size) {
                case STEP_MIN:
                    jdwpSize = JDWP.StepSize.MIN;
                    break;
                case STEP_LINE:
                    jdwpSize = JDWP.StepSize.LINE;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid step size");
            }

            int jdwpDepth;
            switch (depth) {
                case STEP_INTO:
                    jdwpDepth = JDWP.StepDepth.INTO;
                    break;
                case STEP_OVER:
                    jdwpDepth = JDWP.StepDepth.OVER;
                    break;
                case STEP_OUT:
                    jdwpDepth = JDWP.StepDepth.OUT;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid step depth");
            }

            /*
             * Make sure this isn't a duplicate
             */
            List<StepRequest> requests = stepRequests();
            Iterator<StepRequest> iter = requests.iterator();
            while (iter.hasNext()) {
                StepRequest request = iter.next();
                if ((request != this) &&
                        request.isEnabled() &&
                        request.thread().equals(thread)) {
                    throw new DuplicateRequestException(
                        "Only one step request allowed per thread");
                }
            }

            filters.add(JDWP.EventRequest.Set.Modifier.Step.
                        create(this.thread, jdwpSize, jdwpDepth));
            requestList().add(this);

        }
        public int depth() {
            return depth;
        }

        public int size() {
            return size;
        }

        public ThreadReference thread() {
            return thread;
        }

        int eventCmd() {
            return JDWP.EventKind.SINGLE_STEP;
        }

        public String toString() {
            return "step request " + thread() + state();
        }
    }

    class ThreadDeathRequestImpl extends ThreadVisibleEventRequestImpl
                                 implements ThreadDeathRequest {
        ThreadDeathRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.THREAD_DEATH;
        }

        public String toString() {
            return "thread death request " + state();
        }
    }

    class ThreadStartRequestImpl extends ThreadVisibleEventRequestImpl
                                 implements ThreadStartRequest {
        ThreadStartRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.THREAD_START;
        }

        public String toString() {
            return "thread start request " + state();
        }
    }

    abstract class WatchpointRequestImpl extends ClassVisibleEventRequestImpl
                                         implements WatchpointRequest {
        final Field field;

        WatchpointRequestImpl(Field field) {
            this.field = field;
            filters.add(0,
                   JDWP.EventRequest.Set.Modifier.FieldOnly.create(
                    (ReferenceTypeImpl)field.declaringType(),
                    ((FieldImpl)field).ref()));
        }

        public Field field() {
            return field;
        }
    }

    class AccessWatchpointRequestImpl extends WatchpointRequestImpl
                                      implements AccessWatchpointRequest {
        AccessWatchpointRequestImpl(Field field) {
            super(field);
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.FIELD_ACCESS;
        }

        public String toString() {
            return "access watchpoint request " + field + state();
        }
    }

    class ModificationWatchpointRequestImpl extends WatchpointRequestImpl
                                            implements ModificationWatchpointRequest {
        ModificationWatchpointRequestImpl(Field field) {
            super(field);
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.FIELD_MODIFICATION;
        }

        public String toString() {
            return "modification watchpoint request " + field + state();
        }
    }

    class VMDeathRequestImpl extends EventRequestImpl
                             implements VMDeathRequest {
        VMDeathRequestImpl() {
            requestList().add(this);
        }

        int eventCmd() {
            return JDWP.EventKind.VM_DEATH;
        }

        public String toString() {
            return "VM death request " + state();
        }
    }

    /**
     * Constructor.
     */
    EventRequestManagerImpl(VirtualMachine vm) {
        super(vm);
        java.lang.reflect.Field[] ekinds =
            JDWP.EventKind.class.getDeclaredFields();
        int highest = 0;
        for (int i = 0; i < ekinds.length; ++i) {
            int val;
            try {
                val = ekinds[i].getInt(null);
            } catch (IllegalAccessException exc) {
                throw new RuntimeException("Got: " + exc);
            }
            if (val > highest) {
                highest = val;
            }
        }
        requestLists = new List[highest+1];
        for (int i=0; i <= highest; i++) {
            requestLists[i] = Collections.synchronizedList(new ArrayList<>());
        }
    }

    public ClassPrepareRequest createClassPrepareRequest() {
        return new ClassPrepareRequestImpl();
    }

    public ClassUnloadRequest createClassUnloadRequest() {
        return new ClassUnloadRequestImpl();
    }

    public ExceptionRequest createExceptionRequest(ReferenceType refType,
                                                   boolean notifyCaught,
                                                   boolean notifyUncaught) {
        validateMirrorOrNull(refType);
        return new ExceptionRequestImpl(refType, notifyCaught, notifyUncaught);
    }

    public StepRequest createStepRequest(ThreadReference thread,
                                         int size, int depth) {
        validateMirror(thread);
        return new StepRequestImpl(thread, size, depth);
    }

    public ThreadDeathRequest createThreadDeathRequest() {
        return new ThreadDeathRequestImpl();
    }

    public ThreadStartRequest createThreadStartRequest() {
        return new ThreadStartRequestImpl();
    }

    public MethodEntryRequest createMethodEntryRequest() {
        return new MethodEntryRequestImpl();
    }

    public MethodExitRequest createMethodExitRequest() {
        return new MethodExitRequestImpl();
    }

    public MonitorContendedEnterRequest createMonitorContendedEnterRequest() {
        if (!vm.canRequestMonitorEvents()) {
            throw new UnsupportedOperationException(
          "target VM does not support requesting Monitor events");
        }
        return new MonitorContendedEnterRequestImpl();
    }

    public MonitorContendedEnteredRequest createMonitorContendedEnteredRequest() {
        if (!vm.canRequestMonitorEvents()) {
            throw new UnsupportedOperationException(
          "target VM does not support requesting Monitor events");
        }
        return new MonitorContendedEnteredRequestImpl();
    }

    public MonitorWaitRequest createMonitorWaitRequest() {
        if (!vm.canRequestMonitorEvents()) {
            throw new UnsupportedOperationException(
          "target VM does not support requesting Monitor events");
        }
        return new MonitorWaitRequestImpl();
    }

    public MonitorWaitedRequest createMonitorWaitedRequest() {
        if (!vm.canRequestMonitorEvents()) {
            throw new UnsupportedOperationException(
          "target VM does not support requesting Monitor events");
        }
        return new MonitorWaitedRequestImpl();
    }

    public BreakpointRequest createBreakpointRequest(Location location) {
        validateMirror(location);
        if (location.codeIndex() == -1) {
            throw new NativeMethodException("Cannot set breakpoints on native methods");
        }
        return new BreakpointRequestImpl(location);
    }

    public AccessWatchpointRequest
                              createAccessWatchpointRequest(Field field) {
        validateMirror(field);
        if (!vm.canWatchFieldAccess()) {
            throw new UnsupportedOperationException(
          "target VM does not support access watchpoints");
        }
        return new AccessWatchpointRequestImpl(field);
    }

    public ModificationWatchpointRequest
                        createModificationWatchpointRequest(Field field) {
        validateMirror(field);
        if (!vm.canWatchFieldModification()) {
            throw new UnsupportedOperationException(
          "target VM does not support modification watchpoints");
        }
        return new ModificationWatchpointRequestImpl(field);
    }

    public VMDeathRequest createVMDeathRequest() {
        if (!vm.canRequestVMDeathEvent()) {
            throw new UnsupportedOperationException(
          "target VM does not support requesting VM death events");
        }
        return new VMDeathRequestImpl();
    }

    public void deleteEventRequest(EventRequest eventRequest) {
        validateMirror(eventRequest);
        ((EventRequestImpl)eventRequest).delete();
    }

    public void deleteEventRequests(List<? extends EventRequest> eventRequests) {
        validateMirrors(eventRequests);
        // copy the eventRequests to avoid ConcurrentModificationException
        Iterator<? extends EventRequest> iter = (new ArrayList<>(eventRequests)).iterator();
        while (iter.hasNext()) {
            ((EventRequestImpl)iter.next()).delete();
        }
    }

    public void deleteAllBreakpoints() {
        requestList(JDWP.EventKind.BREAKPOINT).clear();

        try {
            JDWP.EventRequest.ClearAllBreakpoints.process(vm);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    public List<StepRequest> stepRequests() {
        return (List<StepRequest>)unmodifiableRequestList(JDWP.EventKind.SINGLE_STEP);
    }

    public List<ClassPrepareRequest> classPrepareRequests() {
        return (List<ClassPrepareRequest>)unmodifiableRequestList(JDWP.EventKind.CLASS_PREPARE);
    }

    public List<ClassUnloadRequest> classUnloadRequests() {
        return (List<ClassUnloadRequest>)unmodifiableRequestList(JDWP.EventKind.CLASS_UNLOAD);
    }

    public List<ThreadStartRequest> threadStartRequests() {
        return (List<ThreadStartRequest>)unmodifiableRequestList(JDWP.EventKind.THREAD_START);
    }

    public List<ThreadDeathRequest> threadDeathRequests() {
        return (List<ThreadDeathRequest>)unmodifiableRequestList(JDWP.EventKind.THREAD_DEATH);
    }

    public List<ExceptionRequest> exceptionRequests() {
        return (List<ExceptionRequest>)unmodifiableRequestList(JDWP.EventKind.EXCEPTION);
    }

    public List<BreakpointRequest> breakpointRequests() {
        return (List<BreakpointRequest>)unmodifiableRequestList(JDWP.EventKind.BREAKPOINT);
    }

    public List<AccessWatchpointRequest> accessWatchpointRequests() {
        return (List<AccessWatchpointRequest>)unmodifiableRequestList(JDWP.EventKind.FIELD_ACCESS);
    }

    public List<ModificationWatchpointRequest> modificationWatchpointRequests() {
        return (List<ModificationWatchpointRequest>)unmodifiableRequestList(JDWP.EventKind.FIELD_MODIFICATION);
    }

    public List<MethodEntryRequest> methodEntryRequests() {
        return (List<MethodEntryRequest>)unmodifiableRequestList(JDWP.EventKind.METHOD_ENTRY);
    }

    public List<MethodExitRequest> methodExitRequests() {
        return (List<MethodExitRequest>)unmodifiableRequestList(EventRequestManagerImpl.methodExitEventCmd);
    }

    public List<MonitorContendedEnterRequest> monitorContendedEnterRequests() {
        return (List<MonitorContendedEnterRequest>)unmodifiableRequestList(JDWP.EventKind.MONITOR_CONTENDED_ENTER);
    }

    public List<MonitorContendedEnteredRequest> monitorContendedEnteredRequests() {
        return (List<MonitorContendedEnteredRequest>)unmodifiableRequestList(JDWP.EventKind.MONITOR_CONTENDED_ENTERED);
    }

    public List<MonitorWaitRequest> monitorWaitRequests() {
        return (List<MonitorWaitRequest>)unmodifiableRequestList(JDWP.EventKind.MONITOR_WAIT);
    }

    public List<MonitorWaitedRequest> monitorWaitedRequests() {
        return (List<MonitorWaitedRequest>)unmodifiableRequestList(JDWP.EventKind.MONITOR_WAITED);
    }

    public List<VMDeathRequest> vmDeathRequests() {
        return (List<VMDeathRequest>)unmodifiableRequestList(JDWP.EventKind.VM_DEATH);
    }

    List<? extends EventRequest> unmodifiableRequestList(int eventCmd) {
        // No need of explicit synchronization for requestList here.
        // It is taken care internally by SynchronizedList class.
        return Collections.unmodifiableList(new ArrayList<>(requestList(eventCmd)));
    }

    EventRequest request(int eventCmd, int requestId) {
        List<? extends EventRequest> rl = requestList(eventCmd);
        synchronized(rl) {   // Refer Collections.synchronizedList javadoc.
            Iterator<? extends EventRequest> itr = rl.iterator();
            while (itr.hasNext()){
                EventRequestImpl er = (EventRequestImpl)itr.next();
                if (er.id == requestId)
                    return er;
            }
        }
        return null;
    }

    private List<? extends EventRequest> requestList(int eventCmd) {
        return requestLists[eventCmd];
    }
}
