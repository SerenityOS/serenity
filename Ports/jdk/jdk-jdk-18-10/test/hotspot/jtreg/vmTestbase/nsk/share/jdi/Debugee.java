/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import nsk.share.*;
import nsk.share.jpda.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;

/**
 * This class is used to interact with debugee VM using JDI features.
 * <p>
 * This class is wrapper for debugee VM constructed by <code>Binder</code>
 * and it uses <code>com.sun.jdi.VirtualMachine</code> to interact with debugee VM.
 * <p>
 * In addition to the general abities to control of debugee VM process,
 * provided by the base class <code>DebugeeProcess</code>, this class
 * adds also several service methods over the JDI features to simplify interaction
 * with debugee VM (such as finding classes, setting breakpoints,
 * handling events, and so on.).
 *
 * @see Binder
 * @see DebugeeProcess
 */
abstract public class Debugee extends DebugeeProcess {

    /**
     * Mirror of the debugee VM. This must be initialized by every
     * particular non-abstract class extending Debugee class.
     */
    protected VirtualMachine vm = null;

    /** Binder that created this debugee. */
    protected Binder binder = null;

    /** Argument handler. */
    protected ArgumentHandler argumentHandler = null;

    /** Create new <code>Debugee</code> object for a given binder. */
    protected Debugee (Binder binder) {
        super(binder);
        this.binder = binder;
        this.argumentHandler = (ArgumentHandler)binder.getArgumentHandler();
    }

    /** Setup <code>Debugee</code> object with given VM mirror. */
    public void setupVM(VirtualMachine vm) {
        if (this.vm != null) {
            throw new TestBug("Setting duplicated VM mirror for Debugee object");
        }
        this.vm = vm;
        int traceMode = argumentHandler.getTraceMode();
        if (traceMode != VirtualMachine.TRACE_NONE) {
            display("Setting JDI trace mode to: " + argumentHandler.getTraceModeString());
            setDebugTraceMode(traceMode);
        }
    }

    /** Return <code>Binder</code> of the debugee object. */
    public Binder getBinder() {
        return binder;
    }

    /** Return JDI mirror of the debugee VM. */
    public VirtualMachine VM() {
        return vm;
    }

    /** Return <code>EventRequestManager</code> of the debugee object. */
    public EventRequestManager getEventRequestManager() {
        return vm.eventRequestManager();
    }

    // --------------------------------------------------- //

    /** List of the currently running threads. */
    public ThreadReference[] threads () {
        List list = vm.allThreads();
        int size = list.size();
        ThreadReference array[] = new ThreadReference[size];
        Iterator iterator = list.iterator();
        for (int i = 0; i < size; i++)
            array[i] = (ThreadReference) iterator.next();
        if (iterator.hasNext())
            throw new Oddity("extra element in a list?");
        return array;
    }

    /** List of all types loaded by the debugee VM. */
    public ReferenceType[] classes() {
        return classes(null);
    }

    /**
     * List of all classes of the given <code>name</code> loaded by
     * the debugee VM; or list of all classes, if <code>name</code>
     * is <code>null</code>.
     */
    private ReferenceType[] classes (String name) {
        List list = (name==null)? vm.allClasses(): vm.classesByName(name);
        int size = list.size();
        ReferenceType array[] = new ReferenceType [ size ];
        Iterator iterator = list.iterator();
        for (int i=0; i<size; i++)
            array[i] = (ReferenceType) iterator.next();
        if (iterator.hasNext())
            throw new Oddity("extra element in a list?");
        return array;
    }

    // --------------------------------------------------- //

    /**
     * Return mirror for the only class of the given <code>name</code>
     * loaded by the debugee VM; or throw TestBug exception if there
     * are more than one such class found. TestFailure exception
     * will be thrown in case when mirrors for classes with different
     * names or duplicated mirrors were returned.
     * Return <code>null</code> if there is no such class loaded.
     */
    public ReferenceType classByName (String name) {
        ReferenceType classes[] = this.classes(name);

        // if on first call debuggee doesn't return needed class try get this class one more time after delay to avoid 6446633
        if (classes == null || classes.length == 0) {
            try {
                Thread.sleep(1000);
            }
            catch(InterruptedException e) {
                throw new TestBug("Unexpected InterruptedException");
            }

            classes = this.classes(name);
        }

        if (classes == null || classes.length == 0)
            return null;

        // analyze returned mirrors and throw appropriate exception
        if (classes.length > 1) {
            boolean duplicatesFound = false;
            boolean differentNamesFound = false;
            boolean visited[] = new boolean[classes.length];
            complain("Classes that were found for name \"" + name + "\":");
            for(ReferenceType klass : classes) {
                complain("\t" + klass);
            }
            for(int c = 0; c < classes.length; c++) {
                if(visited[c]) {
                    continue;
                }
                if(!classes[c].name().equals(name)) {
                    differentNamesFound = true;
                    continue;
                }
                for(int i = c + 1; i < classes.length; i++) {
                    if(visited[i]) {
                        continue;
                    } else {
                        visited[i] = true;
                    }
                    if(classes[c].classLoader() == classes[i].classLoader()) {
                        duplicatesFound = true;
                    }
                }
            }
            if(duplicatesFound) {
                throw new TestFailure("classes with the same name and " +
                                      "loaded with the same class loader " +
                                      "were found.");
            } else if(differentNamesFound) {
                throw new TestFailure("class with name different from '" + name +
                                      "' was returned by VirutualMachine.classesByName.");
            } else {
                throw new TestBug("found " + classes.length + " such classes: " + name);
            }
        }
        return classes[0];
    }

    /**
     * Return mirror for the only method of the given <code>refType</code>
     * class in the debugee VM; or throw TestBug exception if there
     * are more than one such method found. Return <code>null</code> if
     * there is no such method found.
     */
    public Method methodByName(ReferenceType refType, String name) {
        List methods = refType.methodsByName(name);
        if (methods == null || methods.isEmpty()) return null;
        if (methods.size() > 1)
            throw new TestBug(
                "found " + methods.size() + " such methods: " + name);
        Method method = (Method)methods.get(0);
        return method;
    }

    /**
     * Return a currently running thread of the given <code>name</code>; or
     * throw TestBug exception if there are more than one such thread found.
     * Return <code>null</code> if there is no such thread.
     */
    public ThreadReference threadByName (String name) {
        ThreadReference threads[] = this.threads();
        int count = 0, index = -1;
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].name().compareTo(name)==0) {
                count++;
                index = i;
            }
        }
        if (count == 0)
            return null;
        if (count > 1)
            throw new TestBug(
                "found " + count + " such threads: " + name);
        return threads[index];
    }


    public ThreadReference threadByNameOrThrow(String name) throws JDITestRuntimeException {

        List all = vm.allThreads();
        ListIterator li = all.listIterator();
        for (; li.hasNext(); ) {
            ThreadReference thread = (ThreadReference) li.next();
            if (thread.name().equals(name))
                return thread;
        }
        throw new JDITestRuntimeException("** Thread IS NOT found ** : " + name);
    }

    // --------------------------------------------------- //

    /**
     * Returns Location object for given line number in specified method or null
     * if no location for this line is found.
     *
     * @param method method mirror containing given line number
     * @param line line number to find location
     */
    public Location getLineLocation(Method method, int line) {
        List locs = null;
        try {
            locs = method.allLineLocations();
        } catch(AbsentInformationException e) {
            throw new TestBug("Unable to find location for line " + line + ": " + e);
        }
        Iterator iter = locs.iterator();
        while (iter.hasNext()) {
            Location location = (Location)iter.next();
            if (location.lineNumber() == line) {
                return location;
            }
        }
        return null;
    }

    /**
     * Returns Location object for given line number in specified reference type or null
     * if no location for this line is found.
     *
     * @param refType reference type mirror containing given line number
     * @param line line number to find location
     */
    public Location getLineLocation(ReferenceType refType, int line) {
        List locs = null;
        try {
            locs = refType.allLineLocations();
        } catch(AbsentInformationException e) {
            throw new TestBug("Unable to find location for line " + line + ": " + e);
        }
        Iterator iter = locs.iterator();
        while (iter.hasNext()) {
            Location location = (Location)iter.next();
            if (location.lineNumber() == line) {
                return location;
            }
        }
        return null;
    }

    // --------------------------------------------------- //

    /**
     * Make disabled breakpoint to given location and return BreakpointRequest.
     *
     * @param location location to set breakpoint
     *
     * @see #setBreakpoint(Method, int)
     * @see #setBreakpoint(ReferenceType, String, int)
     */
    public BreakpointRequest makeBreakpoint(Location location) {
        EventRequestManager evm = getEventRequestManager();
        BreakpointRequest request = evm.createBreakpointRequest(location);
        display("Breakpoint set:\n\t" + request);
        return request;
    }

    /**
     * Make disabled breakpoint to given line number in specified method
     * and return BreakpointRequest.
     *
     * @param method method mirror to set breakpoint
     * @param lineNumber line number inside the method
     *
     * @throws Failure if no location found for specified line number
     *
     * @see #makeBreakpoint(Location)
     * @see #makeBreakpoint(ReferenceType, String, int)
     */
    public BreakpointRequest makeBreakpoint(Method method, int lineNumber) {
        Location location = getLineLocation(method, lineNumber);
        if (location == null) {
            throw new Failure("No location found for setting breakpoint to line " + lineNumber);
        }
        return makeBreakpoint(location);
    }

    /**
     * Make disabled breakpoint to given line number for specified method name
     * of the given reference type and return BreakpointRequest.
     *
     * @param refType reference type for specified method
     * @param methodName method name to set breakpoint
     * @param lineNumber line number inside the method
     *
     * @throws Failure if no location found for specified line number
     *
     * @see #makeBreakpoint(Method, int)
     */
    public BreakpointRequest makeBreakpoint(ReferenceType refType,
                                            String methodName, int lineNumber) {
        Method method = methodByName(refType, methodName);
        if (method == null) {
            throw new Failure("No method found for setting breakpoint: " + methodName);
        }
        return makeBreakpoint(method, lineNumber);
    }

    /**
     * Set and enable breakpoint to given line number for specified method
     * and return BreakpointRequest.
     *
     * @param method method mirror to set breakpoint
     * @param lineNumber line number inside the method
     *
     * @throws Failure if no location found for specified line number
     *
     * @see #setBreakpoint(ReferenceType, String, int)
     */
    public BreakpointRequest setBreakpoint(Method method, int lineNumber) {
        BreakpointRequest request = makeBreakpoint(method, lineNumber);
        request.enable();
        return request;
    }

    /**
     * Set and enable breakpoint to given line number for specified method name
     * of the given reference type and return BreakpointRequest.
     *
     * @param refType reference type for specified method
     * @param methodName method name to set breakpoint
     * @param lineNumber line number inside the method
     *
     * @throws Failure if no location found for specified line number
     *
     * @see #setBreakpoint(Method, int)
     */
    public BreakpointRequest setBreakpoint(ReferenceType refType,
                                            String methodName, int lineNumber) {
        BreakpointRequest request = makeBreakpoint(refType, methodName, lineNumber);
        request.enable();
        return request;
    }

    // --------------------------------------------------- //

    /** Suspend the debugee VM. */
    public void suspend() {
        vm.suspend();
    }

    /** Resume the debugee VM. */
    public void resume() {
        vm.resume();
    }

    /** Dispose the debugee VM. */
    public void dispose() {
        vm.dispose();
    }

    /*
     * Set internal JDI tracing mode.
     */
    public void setDebugTraceMode(int traceMode) {
        vm.setDebugTraceMode(traceMode);
    }

    // --------------------------------------------------- //

    /**
     * Wait for the requested event and skip other events.
     *
     * @param request non-null value for events generated by this
     * event request; null value for <code>VMStartEvent</code>.
     * @param timeout timeout in milliseconds to wait for the requested event.
     *
     * @throws InterruptedException if another thread has interrupted this thread
     */
    public Event waitingEvent(EventRequest request, long timeout)
                                                throws InterruptedException {

        if (request == null) {
            throw new Failure("Null request specified for waiting events: " + request);
        }

        long timeToFinish = System.currentTimeMillis() + timeout;
        long timeLeft = timeout;
        boolean exit = false;

        display("Waiting for event by request:\n\t" + request);

        EventQueue eventQueue = vm.eventQueue();
        while (timeLeft > 0 && !exit) {

            EventSet eventSet = eventQueue.remove(timeLeft);
            if (eventSet == null) {
                continue;
            }

            EventIterator eventIterator = eventSet.eventIterator();
            while (eventIterator.hasNext()) {

                Event event = eventIterator.nextEvent();
                EventRequest eventRequest = event.request();

                if (request == eventRequest || request.equals(eventRequest)) {
                    display("Got requested event:\n\t" + event);
                    return event;
                } else if (event instanceof VMDeathEvent) {
                    display("Ignore unexpected VMDeathEvent");
                } else if (event instanceof VMDisconnectEvent) {
                    display("Got unexpected VMDisconnectEvent");
                    exit = true;
                    break;
                } else {
                    display("Ignore unexpected event:\n\t" + event);
                } // if

            } // while

            timeLeft = timeToFinish - System.currentTimeMillis();

        } // while

        return null;
    }

    /*
     * Wait for VM to initialize by receiving initial VM_START event for specified timeout.
     */
    public void waitForVMInit(long timeout) {
        waitForVMInit(vm ,log, timeout);
    }

    /*
     * This static method is also used by nsk.share.jdi.ConnectorTest
     */
    static public void waitForVMInit(VirtualMachine vm, Log log, long timeout) {
        try {
            EventSet eventSet = vm.eventQueue().remove(timeout);
            if (eventSet == null) {
                throw new Failure("No VMStartEvent received for timeout: " + timeout + " ms");
            }
            EventIterator iterator = eventSet.eventIterator();
            while (iterator.hasNext()) {
                Event event = iterator.nextEvent();
                if (event == null) {
                    throw new Failure("Null event received instead of VMStartEvent");
                }
                if (event instanceof VMStartEvent) {
                    log.display("Initial VMStartEvent received: " + event);
                } else {
                    throw new Failure("Unexpected event received instead of VMStartEvent: " + event);
                }
            }
            int suspendPolicy = eventSet.suspendPolicy();
            if (suspendPolicy != EventRequest.SUSPEND_ALL) {
                throw new Failure("Suspend policy of VMStartEvent is not SUSPEND_ALL: " + suspendPolicy);
            }
        } catch (InterruptedException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Thread interrupted while waiting for VMStartEvent:\n\t" + e);
        }
    }

    // --------------------------------------------------- //

    /**
     * Bind to debuggee VM using <code>Binder</code> and make initial
     * synchronization via IOPipe.
     *
     * @param argHandler command line arguments handler to make <code>Binder</code> object
     * @param log <code>Log</code> object to log messages
     * @param mainClassName main class of debugee
     *
     * @throws Failure if there were problems with binding to debuggee VM
     *
     * @see Binder#bindToDebugee(String)
     */
    public static Debugee prepareDebugee(ArgumentHandler argHandler, Log log,
                                                String mainClassName) {
        Binder binder = new Binder(argHandler, log);
        Debugee debugee = binder.bindToDebugee(mainClassName);

        debugee.createIOPipe();

        debugee.redirectStderr(log, DEBUGEE_STDERR_LOG_PREFIX);
        debugee.resume();

        debugee.receiveExpectedSignal("ready");

        return debugee;
    }

    /**
     * Send <code>"quit"</code> signal, wait for debugee VM exit and check exit.
     *
     * @throws Failure if exit status is not <code>Consts.JCK_STATUS_BASE</code>
     *
     * @see #endDebugee()
     */
    public void quit() {
        sendSignal("quit");
        int status = endDebugee();
        if ( status != Consts.JCK_STATUS_BASE ) {
            throw new Failure("Got unexpected debugee VM exit status: " + status
                                + " (not " + Consts.JCK_STATUS_BASE + ")");
        }
        display("Got expected debugee VM exit status: " + status);
    }

    /*
     * Dispose debuggee VM, wait for it to exit, close all resources and return
     * exit status code.
     */
    public int endDebugee() {
        int status = waitFor();
        if (vm != null) {
            try {
                vm.dispose();
            } catch (VMDisconnectedException ignore) {
            }
            vm = null;
        }
        return status;
    }

    /*
     * Print information about all threads in debuggee VM
     */
    protected void printThreadsInfo(VirtualMachine vm)  {
        try {
            log.display("------------ Try to print debuggee threads before killing process ------------");
            if (vm == null) {
                log.display("Can't print threads info because 'vm' is null");
                return;
            }
            List<ThreadReference> threads = vm.allThreads();
            log.display("Threads: " + threads);
            log.display("Total threads: " + threads.size());
            for (ThreadReference thread : threads) {
                log.display("\nThread: " + thread.name());
                log.display("Is suspended: " + thread.isSuspended());
                log.display("Is at breakpoint: " + thread.isAtBreakpoint());
                boolean wasSuspended = false;
                try {
                    if (!thread.isSuspended()) {
                        log.display("\n suspend thread to get its stack \n");
                        thread.suspend();
                        wasSuspended = true;
                    }
                    log.display("Stack frame count: " + thread.frameCount());
                    if (thread.frameCount() > 0) {
                        log.display("Frames:");
                        for (StackFrame frame : thread.frames()) {
                            Location location = frame.location();
                            log.display(location.declaringType().name() + "." + location.method().name() + ", line: " + location.lineNumber());
                        }
                    }
                } finally {
                    if (wasSuspended) {
                        log.display("\n resume thread \n");
                        thread.resume();
                    }
                }
            }
            log.display("----------------------------------------------------------------------");
        } catch (Throwable t) {
            log.complain("");
            t.printStackTrace(log.getOutStream());
        }
    }

    /**
     * Force debugge VM to exit using JDI interface if possible.
     */
    protected void killDebugee() {
        try {
            // print information about debuggee threads to simplify failure analysis
            printThreadsInfo(vm);
        } finally {
            if (vm != null) {
                try {
                    display("Killing debuggee by forcing target VM to exit");
                    vm.exit(97);
                    display("Debugee VM successfully forced to exit");
                    vm = null;
                } catch (VMDisconnectedException e) {
                    display("Ignore VMDisconnectedException while forcing debuggee VM to exit:\n\t"
                            + e);
                }
            }
        }
    }

}
