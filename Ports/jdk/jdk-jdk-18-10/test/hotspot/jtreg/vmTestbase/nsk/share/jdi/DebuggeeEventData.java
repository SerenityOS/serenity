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

/*
 * Classes included in this class represent JDI events on debuggee VM's side
 * All this classes contain information about JDI event which should be generated during test execution
 */
public class DebuggeeEventData {
    // base event data class
    public static class DebugEventData {
    }

    /*
     * debug information about monitor event
     */
    public static class DebugMonitorEventData extends DebugEventData {
        public DebugMonitorEventData(Object monitor, Thread thread, Object eventObject) {
            this.monitor = monitor;
            this.thread = thread;
            this.eventObject = eventObject;
        }

        public Object monitor;

        public Thread thread;

        public Object eventObject;
    }

    /*
     * information about MonitorContendedEnterEvent
     */
    public static class DebugMonitorEnterEventData extends DebugMonitorEventData {
        public DebugMonitorEnterEventData(Object monitor, Thread thread, Object eventObject) {
            super(monitor, thread, eventObject);
        }
    }

    /*
     * information about MonitorContendedEnteredEvent
     */
    public static class DebugMonitorEnteredEventData extends DebugMonitorEventData {
        public DebugMonitorEnteredEventData(Object monitor, Thread thread, Object eventObject) {
            super(monitor, thread, eventObject);
        }
    }

    /*
     * information about MonitorWaitEvent
     */
    public static class DebugMonitorWaitEventData extends DebugMonitorEventData {
        public long timeout;

        public DebugMonitorWaitEventData(Object monitor, Thread thread, long timeout, Object eventObject) {
            super(monitor, thread, eventObject);
            this.timeout = timeout;
        }
    }

    /*
     * information about MonitorWaitedEvent
     */
    public static class DebugMonitorWaitedEventData extends DebugMonitorEventData {
        public boolean timedout;

        public DebugMonitorWaitedEventData(Object monitor, Thread thread, boolean timedout, Object eventObject) {
            super(monitor, thread, eventObject);
            this.timedout = timedout;
        }
    }

}
