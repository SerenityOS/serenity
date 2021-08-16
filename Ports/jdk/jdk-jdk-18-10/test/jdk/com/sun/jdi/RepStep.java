/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4334008
 * @summary RepStep detects missed step events due to lack of
 * frame pop events (in back-end).
 * @author Robert Field
 * @library /test/lib
 *
 * @run compile -g RepStepTarg.java
 * @run build VMConnection RepStep
 *
 * @run driver RepStep
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import com.sun.jdi.connect.*;
import jdk.test.lib.process.StreamPumper;

import java.util.*;

public class RepStep {
    static final String TARGET = "RepStepTarg";
    static final int GRANULARITY = StepRequest.STEP_LINE;
    static final int DEPTH = StepRequest.STEP_INTO;
    static final int SUCCESS_COUNT = 30;

    VirtualMachine vm;
    final EventRequestManager requestManager;
    int stepNum = 0;
    boolean passed = false;

    public static void main(String args[]) throws Exception {
        new RepStep(args);
    }

    RepStep(String args[]) throws Exception {
        if (args.length > 0) {
            attachTarget(args[0]);
        } else {
            launchTarget();
        }
        //        vm.setDebugTraceMode(VirtualMachine.TRACE_ALL);
        requestManager = vm.eventRequestManager();
        runTests();
        dieNice();
    }

    private void createStep(ThreadReference thread) {
        final StepRequest sr =
                  requestManager.createStepRequest(thread,
                                                   GRANULARITY,
                                                   DEPTH);

        sr.addClassExclusionFilter("java.*");
        sr.addClassExclusionFilter("javax.*");
        sr.addClassExclusionFilter("sun.*");
        sr.addClassExclusionFilter("com.sun.*");
        sr.addClassExclusionFilter("com.oracle.*");
        sr.addClassExclusionFilter("oracle.*");
        sr.addClassExclusionFilter("jdk.internal.*");
        sr.enable();
    }

    private void runTests() throws Exception {
        ThreadReference thread = null;
        EventQueue queue = vm.eventQueue();
        while (true) {
            EventSet set = queue.remove();
            for (EventIterator it = set.eventIterator(); it.hasNext(); ) {
                Event event = it.nextEvent();
                System.out.println("event: " + String.valueOf(event));
                if (event instanceof VMStartEvent) {
                    // get thread for setting step later
                    thread = ((VMStartEvent)event).thread();
                    ClassPrepareRequest cpReq
                        = requestManager.createClassPrepareRequest();
                    cpReq.addClassFilter(TARGET);
                    cpReq.enable();
                } else if (event instanceof ClassPrepareEvent) {
                    createStep(thread);
                    event.request().disable();
                } else if (event instanceof StepEvent) {
                    // StepEvent stepEvent = (StepEvent)event;
                    // System.out.println(stepEvent);
                    System.out.println(++stepNum);
                    if (stepNum >= SUCCESS_COUNT) {
                        // would have failed by now (> 4)
                        System.out.println("RepStep passed");
                        event.request().disable();
                        set.resume();
                        return; // Success exit
                    }
                } else if (event instanceof VMDeathEvent) {
                    throw new Exception("RepStep failed: steps missed");
                } else {
                    throw new Exception("Unexpected event: " + event);
                }
            }
            set.resume();
       }
    }

    private void dieNice() throws Exception {
        EventQueue queue = vm.eventQueue();
        while (true) {
            EventSet set = queue.remove();
            for (EventIterator it = set.eventIterator(); it.hasNext(); ) {
                Event event = it.nextEvent();
                if (event instanceof VMDeathEvent) {
                    // ignore
                } else if (event instanceof VMDisconnectEvent) {
                    set.resume();
                    return; // Success exit
                } else {
                    throw new Exception("Unexpected event: " + event);
                }
            }
            set.resume();
       }
    }

    private Connector findConnector(String name) throws Exception {
        List connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator iter = connectors.iterator();
        while (iter.hasNext()) {
            Connector connector = (Connector)iter.next();
            if (connector.name().equals(name)) {
                return connector;
            }
        }
        throw new Exception("No connector: " + name);
    }

    /* launch child target vm */
    private void launchTarget() throws Exception {
        LaunchingConnector launcher =
          (LaunchingConnector)findConnector("com.sun.jdi.CommandLineLaunch");
        Map connectorArgs = launcher.defaultArguments();
        Connector.Argument mainArg =
            (Connector.Argument)connectorArgs.get("main");
        mainArg.setValue(TARGET);
        Connector.Argument optionsArg =
            (Connector.Argument)connectorArgs.get("options");
        optionsArg.setValue(VMConnection.getDebuggeeVMOptions());

        vm = launcher.launch(connectorArgs);
        // redirect stdout/stderr
        new StreamPumper(vm.process().getInputStream())
                .addPump(new StreamPumper.LinePump() {
                    @Override
                    protected void processLine(String line) {
                        System.out.println("[debugee_stdout] " + line);
                    }
                })
                .process();
        new StreamPumper(vm.process().getErrorStream())
                .addPump(new StreamPumper.LinePump() {
                    @Override
                    protected void processLine(String line) {
                        System.err.println("[debugee_stderr] " + line);
                    }
                })
                .process();
        System.out.println("launched: " + TARGET);
    }

    private void attachTarget(String portNum) throws Exception {
        AttachingConnector conn =
            (AttachingConnector)findConnector("com.sun.jdi.SocketAttach");
        Map connectorArgs = conn.defaultArguments();
        Connector.Argument portArg =
            (Connector.Argument)connectorArgs.get("port");
        portArg.setValue(portNum);
        vm = conn.attach(connectorArgs);
        System.out.println("attached to: " + portNum);
    }

}
