/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.test.StressOptions;
import nsk.share.test.Stresser;

/*
 *  Class can be used as base debugger class in jdi tests.
 *  Class contains common method for initializing log, debugee, pipe, vm, and several common auxiliary methods.
 *  Sublcass should implement doTest() and, if needed, doInit(parse command line parameters) and canRunTest(check if VM support tested functionality)
 */
public class TestDebuggerType2 {
    public class EventListenerThread extends Thread {
        private EventRequest eventRequest;

        private Event event;

        private Wicket wicket = new Wicket();

        public EventListenerThread(EventRequest eventRequest) {
            this.eventRequest = eventRequest;
        }

        public void run() {
            wicket.unlock();

            try {
                event = debuggee.waitingEvent(eventRequest, argHandler.getWaitTime() * 60000);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        public void waitStartListen() {
            wicket.waitFor();
        }

        public Event getEvent() {
            try {
                // wait when EventListenerThread complete execution
                this.join();
            } catch (InterruptedException e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
            }

            return event;
        }
    }

    protected ArgumentHandler argHandler;

    protected Log log;

    protected IOPipe pipe;

    protected Debugee debuggee;

    protected VirtualMachine vm;

    /*
     * this method is called from nsk.share.jdi.SerialExecutionDebugger to set for debugger
     * already created instances of ArgumentHandler, Log, IOPipe, Debugee, VirtualMachine
     */
    public void initDebugger(ArgumentHandler argHandler, Log log, IOPipe pipe, Debugee debuggee, VirtualMachine vm) {
        this.argHandler = argHandler;
        this.log = log;
        this.pipe = pipe;
        this.debuggee = debuggee;
        this.vm = vm;
    }

    private boolean success = true;

    protected void setSuccess(boolean value) {
        success = value;
    }

    protected boolean getSuccess() {
        return success;
    }

    // class name used during initialization
    protected String debuggeeClassName() {
        return null;
    }

    // select only class name if debuggeeClassName() returns className + debuggee parameters
    protected String debuggeeClassNameWithoutArgs() {
        String className = debuggeeClassName();

        int index = className.indexOf(' ');
        if (index > 0) {
            return className.substring(0, index);
        } else
            return className;
    }

    protected String classpath;

    protected String testWorkDir;

    // initialize test and remove unsupported by nsk.share.jdi.ArgumentHandler arguments
    // (ArgumentHandler constructor throws BadOption exception if command line contains unrecognized by ArgumentHandler options)
    // support -testClassPath parameter: path to find classes for custom classloader in debuggee VM
    // (note that in this method stressOptions and stresser are created, so if subclasses override this method
    // overrided version should first call super.doInit())
    protected String[] doInit(String args[], PrintStream out) {
        stressOptions = new StressOptions(args);
        stresser = new Stresser(stressOptions);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testClassPath") && (i < args.length - 1)) {
                classpath = args[i + 1];
                i++;
            } else if (args[i].equals("-testWorkDir") && (i < args.length - 1)) {
                testWorkDir = args[i + 1];

                if (testWorkDir.endsWith(File.separator)) {
                    testWorkDir = testWorkDir.substring(0, testWorkDir.length() - 1);
                }

                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    protected void doTest() {
        setSuccess(false);
        throw new TestBug("TEST BUG: method doTest not implemented");
    }

    protected Stresser stresser;
    protected StressOptions stressOptions;

    // initialize log, debuggee, pipe
    public void init(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(doInit(args, out));
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        debuggee = binder.bindToDebugee(debuggeeClassName());
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "Debugger.err> ");
        vm = debuggee.VM();
        debuggee.resume();

        String command = pipe.readln();

        if (!command.equals(AbstractDebuggeeTest.COMMAND_READY)) {
            setSuccess(false);
            log.complain("TEST BUG: unknown debuggee's command: " + command);
        }
    }

    // check that vm support tested functions
    protected boolean canRunTest() {
        return true;
    }

    // send quit command to debuggee
    protected void quitDebuggee() {
        pipe.println(AbstractDebuggeeTest.COMMAND_QUIT);
        debuggee.waitFor();

        int debStat = debuggee.getStatus();

        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            setSuccess(false);
            log.complain("TEST FAILED: debuggee's process finished with status: " + debStat);
        } else
            log.display("Debuggee's process finished with status: " + debStat);

    }

    // init test, execute test, quit debuggee
    protected int runIt(String args[], PrintStream out) {
        init(args, out);

        try {
            if (canRunTest())
                doTest();
        } catch (TestBug testBug) {
            setSuccess(false);
            log.complain("Test bug: " + testBug);
            testBug.printStackTrace(log.getOutStream());
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        }

        quitDebuggee();

        if (getSuccess()) {
            log.display("TEST PASSED");
            return Consts.TEST_PASSED;
        } else {
            log.display("TEST FAILED");
            return Consts.TEST_FAILED;
        }
    }

    // check the debuggee completed pervious command and is ready for new one
    protected boolean isDebuggeeReady() {
        String command = pipe.readln();

        if (!command.equals(AbstractDebuggeeTest.COMMAND_READY)) {
            setSuccess(false);
            log.complain("TEST BUG: unknown debuggee's command: " + command);

            return false;
        }

        return true;
    }

    // find in debuggee VM instance of object with given class and check
    // that there is only one instance of this class
    protected ObjectReference findSingleObjectReference(String className) {
        ReferenceType referenceType = debuggee.classByName(className);

        if (referenceType == null)
            throw new TestBug("There is no class '" + className + "' in debuggee");

        List<ObjectReference> instances = referenceType.instances(0);

        if (instances.size() == 0)
            throw new TestBug("There are no instances of class '" + className + "' in debuggee");

        if (instances.size() > 1)
            throw new TestBug("There are more than one(" + instances.size() + ") instance of '" + className + " in debuggee");

        return instances.get(0);
    }

    protected BreakpointEvent waitForBreakpoint(BreakpointRequest breakpointRequest) {
        BreakpointEvent breakpointEvent;

        try {
            breakpointEvent = (BreakpointEvent) debuggee.waitingEvent(breakpointRequest, argHandler.getWaitTime() * 60000);
        } catch (InterruptedException e) {
            setSuccess(false);
            e.printStackTrace(log.getOutStream());
            log.complain("unexpected InterruptedException: " + e);

            breakpointEvent = null;
        }

        if (breakpointEvent == null) {
            setSuccess(false);
            log.complain("Didn't get expected breakpoint event");
        }

        return breakpointEvent;
    }


    private boolean currentSuccess = false;
    protected void forceGC() {
        pipe.println(AbstractDebuggeeTest.COMMAND_FORCE_GC);
        if (!isDebuggeeReady())
            return;
        currentSuccess = getSuccess();
    }

    // Get GC statistics
    protected void resetStatusIfGC() {
        pipe.println(AbstractDebuggeeTest.COMMAND_GC_COUNT);
        String command = pipe.readln();
        if (command.startsWith(AbstractDebuggeeTest.COMMAND_GC_COUNT)) {
            if (!isDebuggeeReady()) {
                return;
            }
            if (Integer.valueOf(command.substring(AbstractDebuggeeTest.COMMAND_GC_COUNT.length() + 1)) > 0) {
                log.display("WARNING: The GC worked during tests. Results are skipped.");
                setSuccess(currentSuccess);
            }
            return;
        }
        setSuccess(false);
    }


    protected BreakpointRequest defaultBreakpointRequest;

    protected void initDefaultBreakpoint() {
        defaultBreakpointRequest = debuggee.makeBreakpoint(debuggee.classByName(debuggeeClassNameWithoutArgs()),
                AbstractDebuggeeTest.DEFAULT_BREAKPOINT_METHOD_NAME, AbstractDebuggeeTest.DEFAULT_BREAKPOINT_LINE);

        defaultBreakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        defaultBreakpointRequest.enable();
    }

    protected void removeDefaultBreakpoint() {
        defaultBreakpointRequest.disable();
        debuggee.getEventRequestManager().deleteEventRequest(defaultBreakpointRequest);
        defaultBreakpointRequest = null;
    }

    protected Value createVoidValue() {
        return vm.mirrorOfVoid();
    }

    // force debuggee call 'TestDebuggeeType2.breakpointMethod()'
    protected BreakpointEvent forceBreakpoint() {
        pipe.println(AbstractDebuggeeTest.COMMAND_FORCE_BREAKPOINT);

        BreakpointEvent event = waitForBreakpoint(defaultBreakpointRequest);

        return event;
    }

    protected void unexpectedException(Throwable t) {
        setSuccess(false);
        log.complain("Unexpected exception: " + t);
        t.printStackTrace(log.getOutStream());
    }

    protected void display(String msg) {
        log.display(msg);
    }

    protected void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

}
