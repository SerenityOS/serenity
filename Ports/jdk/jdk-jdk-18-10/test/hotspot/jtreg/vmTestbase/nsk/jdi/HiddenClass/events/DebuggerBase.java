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
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.Asserts;

import nsk.share.Log;
import nsk.share.jdi.ArgumentHandler;
import nsk.share.jdi.Binder;
import nsk.share.jdi.Debugee;
import nsk.share.jpda.IOPipe;

// This class is the test debugger base class
public class DebuggerBase {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;

    public static final String COMMAND_READY = "ready";
    public static final String COMMAND_RUN   = "run";
    public static final String COMMAND_DONE  = "done";
    public static final String COMMAND_ERROR = "error";
    public static final String COMMAND_QUIT  = "quit";

    public final Log log;

    private VirtualMachine vm;
    private Debugee debuggee = null;
    private IOPipe pipe = null;
    private EventRequestManager erManager = null;

    protected DebuggerBase(ArgumentHandler argHandler) {
        log = new Log(System.out, argHandler);
    }

    VirtualMachine vm() {
        return vm;
    }

    // Find a ReferenceType by a given type name.
    ReferenceType getReferenceType(String typeName) {
        List<ReferenceType> list = vm.classesByName(typeName);

        Asserts.assertFalse(list.size() == 0, "FAIL: type not found: " + typeName);
        Asserts.assertFalse(list.size() > 1, "FAIL: multiple types found: " + typeName);
        log.display("  Found type: " + typeName);
        return list.get(0);
    }

    // Find a Field by a given ReferenceType and a field name.
    Field findField(ReferenceType refType, String fieldName) {
        Field field = refType.fieldByName(fieldName);
        String fullName = refType.name() + "::" + fieldName;

        Asserts.assertNotNull(field, "FAIL: field not found: " + fullName);
        log.display("  Found field: " + fullName);
        return field;
    }

    // Find a Method by a given ReferenceType and a method name.
    Method findMethod(ReferenceType refType, String methodName) {
        List<Method> list = refType.methodsByName(methodName);
        String fullName = refType.name() + "::" + methodName;

        Asserts.assertFalse(list.size() == 0, "FAIL: method not found: " + fullName);
        Asserts.assertFalse(list.size() > 1, "FAIL: multiple methods found: " + fullName);

        log.display("  Found method: " + fullName);
        return list.get(0);
    }

    // Invoke a given static method in debuggee VM at an eventpoint.
    boolean invokeStaticMethod(ThreadReference thread, Method methodToInvoke) {
        boolean failedStatus = false;
        List<? extends Value> args = new ArrayList<>();
        int flags = (ClassObjectReference.INVOKE_NONVIRTUAL |
                     ClassObjectReference.INVOKE_SINGLE_THREADED);
        try {
            log.display("  invoking method: " + methodToInvoke);
            ClassType type = (ClassType)methodToInvoke.declaringType();
            Value val = type.invokeMethod(thread, methodToInvoke, args, flags);
            log.display("   method getHCField returned result: " + val);
        } catch (Exception ex) {
            log.complain("Exception in HC::getHCField method invocation: " + ex);
            failedStatus = true;
        }
        return failedStatus;
    }

    protected void launchDebuggee(ArgumentHandler argHandler, String debuggeeName) {
        Binder binder = new Binder(argHandler, log);
        log.display("\n# Connecting to debuggee");

        debuggee = binder.bindToDebugee(debuggeeName);
        debuggee.redirectStderr(log, "debuggee >");

        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        erManager = vm.eventRequestManager();

        log.display("# Resuming debuggee");
        debuggee.resume();
    }

    protected boolean shutdownDebuggee() {
        boolean debuggeeFailed = false;
        log.display("\n# Shutting down debuggee");

        // wait for debuggee exits and analize its exit code
        log.display("# Waiting for debuggee terminating");
        int debuggeeStatus = debuggee.endDebugee();
        if (debuggeeStatus == PASSED + JCK_STATUS_BASE) {
            log.display("# Debuggee PASSED with exit code: " + debuggeeStatus);
        } else {
            log.complain("# Debuggee FAILED with exit code: " + debuggeeStatus);
            debuggeeFailed = true;
        }
        return debuggeeFailed;
    }

    protected EventRequest enableBreakpointRequest(Method method) {
        log.display("\n# Creating BreakpointRequest");
        BreakpointRequest request = erManager.createBreakpointRequest(method.location());
        Asserts.assertNotNull(request, "FAIL: unable to create BreakpointRequest");

        // enable event request
        request.enable();
        log.display("  Enabled BreakpointRequest");
        return request;
    }

    protected EventRequest enableClassPrepareRequest(String classFilter) {
        log.display("\n# Creating ClassPrepareRequest");
        ClassPrepareRequest request = erManager.createClassPrepareRequest();
        Asserts.assertNotNull(request, "FAIL: unable to create ClassPrepareRequest");

        if (classFilter != null) {
            log.display("  Adding filter to ClassPrepareRequest: " + classFilter);
            request.addClassFilter(classFilter);
        }
        // enable event request
        request.enable();
        log.display("  Enabled ClassPrepareRequest");
        return request;
    }

    protected EventRequest enableClassUnloadRequest(String classFilter) {
        log.display("\n# Creating request for ClassUnloadEvent");
        ClassUnloadRequest request = erManager.createClassUnloadRequest();
        Asserts.assertNotNull(request, "FAIL: unable to create ClassUnloadRequest");

        if (classFilter != null) {
            log.display("  Adding filter to ClassUnloadRequest: " + classFilter);
            request.addClassFilter(classFilter);
        }
        // enable event request
        request.enable();
        log.display("  Enabled ClassUnloadRequest");
        return request;
    }

    protected EventRequest enableModificationWatchpointRequest(Field field, String classFilter) {
        log.display("\n# Creating request for ModificationWatchpointRequest");

        ModificationWatchpointRequest request = erManager.createModificationWatchpointRequest(field);
        Asserts.assertNotNull(request, "FAIL: unable to create ModificationWatchpointRequest");

        if (classFilter != null) {
            log.display("  Adding filter to ModificationWatchpointRequest: " + classFilter);
            request.addClassFilter(classFilter);
        }
        // enable event request
        request.enable();
        log.display("  Enabled ModificationWatchpointRequest");
        return request;
    }

    protected void disableRequest(EventRequest eq, String reqestName) {
        // disable event requests to prevent appearance of further events
        if (eq != null && eq.isEnabled()) {
            log.display("  Disabling " + reqestName);
            eq.disable();
        }
    }

    // sync on the COMMAND_READY
    protected void readyCmdSync() {
        // wait for READY signal from debugee
        log.display("\n# Waiting for command: " + COMMAND_READY);
        String command = pipe.readln();
        Asserts.assertFalse(command == null || !command.equals(COMMAND_READY),
                            "FAIL: unexpected debuggee's command: " + command);
        log.display("\n# Got command: " + COMMAND_READY);
    }

    // sync on the COMMAND_RUN
    protected void runCmdSync() {
        // activate debugee
        log.display("\n# Sending command: " + COMMAND_RUN);
        pipe.println(COMMAND_RUN);
    }

    // sync on the COMMAND_DONE
    protected void doneCmdSync() {
        // wait for DONE signal from debugee
        log.display("\n# Waiting for command: " + COMMAND_DONE);
        String command = pipe.readln();
        Asserts.assertFalse(command == null || !command.equals(COMMAND_DONE),
                    "FAIL: unexpected debuggee's command: " + command);
        log.display("\n# Got command: " + COMMAND_DONE);
    }

    // sync on the COMMAND_QUIT
    protected void quitCmdSync() {
        // force debugee to exit
        log.display("\n# Sending command: " + COMMAND_QUIT);
        pipe.println(COMMAND_QUIT);
    }
}
