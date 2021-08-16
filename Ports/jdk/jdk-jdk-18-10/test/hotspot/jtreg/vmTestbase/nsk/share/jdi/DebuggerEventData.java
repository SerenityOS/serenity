/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import com.sun.jdi.*;
import com.sun.jdi.event.*;

/*
 * Classes included in this class represent JDI events on debugger VM's side
 * All this classes contain information about JDI event which should be generated during test execution,
 * instances of this classes should be created using instances of corresponding classes from debuggee VM
 */
public class DebuggerEventData
{
    static abstract class DebugEventData
    {

        protected Class<?> eventClass;
        public DebugEventData(Class<?> eventClass) {
            this.eventClass = eventClass;
        }

        public boolean shouldCheckEvent(Event event) {
            return this.eventClass.isAssignableFrom(event.getClass());
        }

        // is given event's data identical with data stored in this object
        abstract public boolean checkEvent(Event event);
    }

    /*
     * debug information about monitor event
     */
    static abstract class DebugMonitorEventData extends DebugEventData {
        protected ObjectReference monitor;

        protected ThreadReference thread;

        public DebugMonitorEventData(Class<?> eventClass, ObjectReference debuggeeMirror) {
            super(eventClass);

            this.eventClass = eventClass;
            monitor = (ObjectReference) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("monitor"));
            thread = (ThreadReference) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("thread"));
        }

        public String toString() {
            return eventClass.getName() + " monitor: " + monitor + " thread: " + thread;
        }
    }

    /*
     * information about MonitorContendedEnterEvent
     */
    static class DebugMonitorEnterEventData extends DebugMonitorEventData {
        public DebugMonitorEnterEventData(ObjectReference debuggeeMirror) {
            super(MonitorContendedEnterEvent.class, debuggeeMirror);
        }

        public boolean checkEvent(Event event) {
            MonitorContendedEnterEvent monitorEnterEvent = (MonitorContendedEnterEvent) event;

            return monitorEnterEvent.monitor().equals(monitor) && monitorEnterEvent.thread().equals(thread);
        }
    }

    /*
     * information about MonitorContendedEnteredEvent
     */
    static class DebugMonitorEnteredEventData extends DebugMonitorEventData {
        public DebugMonitorEnteredEventData(ObjectReference debuggeeMirror) {
            super(MonitorContendedEnteredEvent.class, debuggeeMirror);
        }

        public boolean checkEvent(Event event) {
            MonitorContendedEnteredEvent monitorEnterEvent = (MonitorContendedEnteredEvent) event;

            return monitorEnterEvent.monitor().equals(monitor) && monitorEnterEvent.thread().equals(thread);
        }
    }

    /*
     * information about MonitorWaitEvent
     */
    static class DebugMonitorWaitEventData extends DebugMonitorEventData {
        private long timeout;

        public DebugMonitorWaitEventData(ObjectReference debuggeeMirror) {
            super(MonitorWaitEvent.class, debuggeeMirror);

            timeout = ((LongValue) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("timeout"))).longValue();
        }

        public boolean checkEvent(Event event) {
            MonitorWaitEvent monitorWaitEvent = (MonitorWaitEvent) event;

            return monitorWaitEvent.monitor().equals(monitor) && monitorWaitEvent.thread().equals(thread) && (monitorWaitEvent.timeout() == timeout);
        }

        public String toString() {
            return eventClass.getName() + " monitor: " + monitor + " thread: " + thread + " timeout: " + timeout;
        }
    }

    /*
     * information about MonitorWaitedEvent
     */
    static class DebugMonitorWaitedEventData extends DebugMonitorEventData {
        private boolean timedout;

        public DebugMonitorWaitedEventData(ObjectReference debuggeeMirror) {
            super(MonitorWaitedEvent.class, debuggeeMirror);

            timedout = ((BooleanValue) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("timedout"))).booleanValue();
        }

        public boolean checkEvent(Event event) {
            MonitorWaitedEvent monitorWaitedEvent = (MonitorWaitedEvent) event;

            return monitorWaitedEvent.monitor().equals(monitor) && monitorWaitedEvent.thread().equals(thread)
                    && (monitorWaitedEvent.timedout() == timedout);
        }

        public String toString() {
            return eventClass.getName() + " monitor: " + monitor + " thread: " + thread + " timedout: " + timedout;
        }
    }
}
