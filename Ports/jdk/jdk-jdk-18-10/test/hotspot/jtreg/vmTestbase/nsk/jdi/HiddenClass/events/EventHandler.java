/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.HiddenClass.events;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.request.EventRequest;

import jdk.test.lib.Asserts;
import nsk.jdi.HiddenClass.events.DebuggerBase;

import nsk.share.Log;

/* This is a special thread to handle hidden class related events.
 * The thread is looping on accepting events until all the expected
 * event types are received. */
public class EventHandler extends Thread {
    private static final int TIMEOUT_DELTA = 1000; // milliseconds
    private final DebuggerBase debuggerBase;
    private final Log log;

    private volatile boolean testFailed = false;
    private boolean breakpointEventRecieived = false;
    private boolean classPrepareEventRecieived = false;
    private boolean classUnloadEventRecieived = false;
    private boolean modificationWatchpointEventRecieived = false;

    // Hidden class ReferenceType which is saved for debugger.
    private ReferenceType hcRefType = null;

    // This method is called by the debugger main thread.
    static EventHandler createAndStart(DebuggerBase debuggerBase) {
        // start EventHandler thread
        EventHandler handler = new EventHandler(debuggerBase);
        handler.start();
        return handler;
    }

    // Constructor
    private EventHandler(DebuggerBase debuggerBase) {
        this.debuggerBase = debuggerBase;
        this.log = debuggerBase.log;
        log.display("\n# EventHandler is started");
    }

    // This method is called by the debugger main thread.
    void waitForCompleteness() {
        log.display("\n# Waiting for EventHandler to complete");
        // wait for all expected events received or timeout exceeds
        try {
            super.join();
            if (isAlive()) {
                log.complain("FAILURE: Timeout for waiting event was exceeded");
                interrupt();
                testFailed = true;
            } else {
                log.display("# EventHandler completed");
            }
        } catch (InterruptedException ie) {
            log.complain("FAIL: InterruptedException caught while waiting for eventHandler's death");
            testFailed = true;
        }
    }

    // This method is called by the debugger main thread to wait and get
    // the hidden class reference type from its ClassPrepare event.
    // The readyCmdSync with the debuggeee is not enough because a
    // ClassPrepare event is delivered over JDWP protocol with a delay.
    // A wait/notify sync is to ensure the debugger gets non-null value.
    synchronized ReferenceType waitAndGetHCRefType() throws InterruptedException {
        while (hcRefType == null) {
            wait();
        }
        return hcRefType;
    }

    // This method is called by the debugger main thread.
    boolean failedStatus() { return testFailed; }

    // Save hidden class ReferenceType when its ClassPrepare event is received.
    private synchronized void setHCRefType(ReferenceType type) {
        hcRefType = type;
        notifyAll();
    }

    private EventSet getNextEventSet() throws InterruptedException {
        EventSet eventSet = debuggerBase.vm().eventQueue().remove(TIMEOUT_DELTA);
        return eventSet;
    }

    // Breakpoint event handler.
    private void checkBreakpointEvent(BreakpointEvent event) {
        Method method = event.location().method();
        ClassType type = (ClassType)method.declaringType();

        // got expected event in a hidden class method
        log.display("\nBreakpointEvent: " + event.toString());
        log.display("BreakpointEvent: " + type.name() + "::" + method.name());
        breakpointEventRecieived = true;

        // find another method in the same hidden class
        ThreadReference thread = event.thread();
        Method methodToInvoke = debuggerBase.findMethod(type, "getHCField");

        // invoke hidden class static method getNCField in debuggee VM
        testFailed |= debuggerBase.invokeStaticMethod(thread, methodToInvoke);
    }

    // ClassPrepare event handler.
    private void checkClassPrepareEvent(ClassPrepareEvent event) {
        ReferenceType type = event.referenceType();
        String name = type.name();
        String sign = type.signature();

        // set hidden class ReferenceType object for debugger
        setHCRefType(type);

        log.display("\nClassPrepareEvent: " + event.toString());
        log.display("ClassPrepareEvent class name: " + name);
        log.display("ClassPrepareEvent class sign: " + sign);
        classPrepareEventRecieived = true;
        Asserts.assertTrue(name.indexOf("HiddenClass") > 0 && name.indexOf("/0x") > 0,
                           "FAIL: unexpected class in ClassPrepareEvent");
    }

    // ClassUnload event handler.
    private void checkClassUnloadEvent(ClassUnloadEvent event) {
        EventRequest request = event.request();
        String name = event.className();

        log.display("\nClassUnloadEvent class name: " + name);
        log.display("ClassUnloadEvent class sign: " + event.classSignature());
        classUnloadEventRecieived = true;
        Asserts.assertTrue(name.indexOf("HiddenClass") > 0 && name.indexOf("/0x") > 0,
                           "FAIL: unexpected class in ClassUnloadEvent");
    }

    // ModificationWatchpoint event handler.
    private void checkModificationWatchpointEvent(ModificationWatchpointEvent event) {
        EventRequest request = event.request();
        Field field = event.field();
        ReferenceType type = field.declaringType();
        log.display("\nModificationWatchpointEvent: " + event.toString());
        log.display("ModificationWatchpointEvent: field: " + type.name() + "::" + field.name());
        log.display("ModificationWatchpointEvent: value: " + event.valueToBe().toString());
        modificationWatchpointEventRecieived = true;
    }

    private void processEventSet(EventSet eventSet) throws InterruptedException {
        // handle each event from the event set
        EventIterator eventIterator = eventSet.eventIterator();
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();

            if (!breakpointEventRecieived &&
                event instanceof BreakpointEvent) {
                checkBreakpointEvent((BreakpointEvent)event);
            }
            if (!classPrepareEventRecieived &&
                event instanceof ClassPrepareEvent) {
                checkClassPrepareEvent((ClassPrepareEvent)event);
            }
            if (!classUnloadEventRecieived &&
                event instanceof ClassUnloadEvent) {
                checkClassUnloadEvent((ClassUnloadEvent)event);
            }
            if (!modificationWatchpointEventRecieived &&
                event instanceof ModificationWatchpointEvent) {
                checkModificationWatchpointEvent((ModificationWatchpointEvent)event);
            }
            // ignore all other events
        }
    }

    public void run() {
        log.display("\nEventHandler started");
        try {
            // Handle events until all expected events are received.
            while (!breakpointEventRecieived ||
                   !classPrepareEventRecieived ||
                   !classUnloadEventRecieived ||
                   !modificationWatchpointEventRecieived
                  ) {
                EventSet eventSet = getNextEventSet();
                if (eventSet == null) {
                    continue;
                }
                processEventSet(eventSet);
                eventSet.resume();
            }
        } catch (Throwable t) {
            log.complain("Throwable in EventHandler: " + t);
            testFailed = true;
        }
        log.display("\nEventHandler finished");
    }
} // class EventHandler
