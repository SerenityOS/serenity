/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share.jdi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nsk.share.jdi.Binder;
import nsk.share.jdi.Debugee;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.jpda.StratumUtils;
import vm.share.options.Option;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

/**
 * Option value syntax:
 *
 * <pre>
 * breakpoints        := breakpoint breakpoints?
 * breakpoint         := implicitOpt? methodName options? stratum? subBreakpoints?
 *
 * implicitOpt        := "~"
 *
 * methodName         := STRING
 * methodName         := className "." STRING
 * className          := STRING
 *
 * options            :=
 * options            := ":" option options
 * option             := lineOption | requiredHitsOption | stepsToTraceOption
 * lineOption         := "L" INTEGER                                                     // Line number
 * requiredHitsOption := "H" INTEGER | "H*"                                              // Required number of hits
 * stepsToTraceOption  := "S" INTEGER                                                    // Steps to trace when this breakpoint is hit
 *
 * stratum            := "/" stratumName "=" stratumSourceName ":" stratumSourceLine     // Also check stratum information when this breakpoint is hit
 * stratumName        := STRING
 * stratumSourceName  := STRING
 * stratumSourceLine  := INTEGER
 *
 * subBreakpoints := "=>(" breakpoints ")"                                               // subBreakpoints are only set when its main breakpoint is hit.
 * </pre>
 */

public abstract class JDIBreakpointTest extends MlvmTest {

    @Option(name="debugger.debuggeeClass", default_value="", description="Debuggee class name")
    public String _debuggeeClass = "DEBUGGEE-CLASS-NOT-DEFINED";

    @Option(name="debugger.terminateWhenAllBPHit", default_value="", description="Hang up in specified point")
    public boolean _terminateWhenAllBreakpointsHit;

    protected static int _jdiEventWaitTimeout = 3000;

    private static final int MAX_EVENT_COUNT = 50000;

    private static final int SHORT_STACK_TRACE_FRAMES_NUM = 2;

    protected VirtualMachine _vm;
    protected EventQueue _eventQueue;

    private abstract static class BreakpointListIterator {
        List<BreakpointInfo> _biList;

        public BreakpointListIterator(List<BreakpointInfo> biList) {
            _biList = biList;
        }

        public Object go() throws Throwable {
            return iterate(_biList);
        }

        public Object iterate(List<BreakpointInfo> biList) throws Throwable {
            for ( BreakpointInfo bi : biList ) {
                Object result = apply(bi);
                if ( result != null )
                    return result;

                if ( bi.subBreakpoints != null ) {
                    result = iterate(bi.subBreakpoints);
                    if ( result != null )
                        return result;
                }
            }

            return null;
        }

        protected abstract Object apply(BreakpointInfo bi) throws Throwable;
    }

    protected String getDebuggeeClassName() throws Throwable {
        String debuggeeClass = _debuggeeClass.trim();
        if ( debuggeeClass == null || debuggeeClass.isEmpty() )
            throw new Exception("Please specify debuggee class name");

        return debuggeeClass;
    }

    protected abstract List<BreakpointInfo> getBreakpoints(String debuggeeClassName);

    protected boolean getTerminateWhenAllBPHit() {
        return _terminateWhenAllBreakpointsHit;
    }

    protected void breakpointEventHook(BreakpointEvent bpe) {}
    protected void stepEventHook(StepEvent se) {}
    protected void classPrepareEventHook(ClassPrepareEvent cpe) {}
    protected void eventHook(Event e) {}

    @Override
    public boolean run() throws Throwable {
        JDIBreakpointTest._jdiEventWaitTimeout = getArgumentParser().getWaitTime() * 60000;

        boolean terminateWhenAllBPHit = getTerminateWhenAllBPHit();

        final String debuggeeClass = getDebuggeeClassName();

        Binder binder = new Binder((ArgumentHandler) getArgumentParser(), getLog());

        Debugee debuggee = binder.bindToDebugee(debuggeeClass);
        if (debuggee == null)
            throw new Exception("Can't launch debuggee");

        debuggee.redirectOutput(getLog());

        _vm = debuggee.VM();
        _eventQueue = _vm.eventQueue();
        EventRequestManager erm = _vm.eventRequestManager();

        // Breakpoints

        final List<BreakpointInfo> breakpoints = getBreakpoints(debuggeeClass);

        final HashMap<String, ClassPrepareRequest> bpClassNames = new HashMap<String, ClassPrepareRequest>();

        new BreakpointListIterator(breakpoints) {
            @Override protected Object apply(BreakpointInfo bi) {
                if ( bi.className.isEmpty() )
                    bi.className = debuggeeClass;
                bpClassNames.put(bi.className, null);
                return null;
            }
        }.go();

        for (String className : bpClassNames.keySet()) {
            Env.traceNormal("Requesting ClassPrepareEvent for [" + className + "]");

            ClassPrepareRequest cpReq = erm.createClassPrepareRequest();
            cpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            cpReq.addClassFilter(className);
            cpReq.enable();

            bpClassNames.put(className, cpReq);
        }

        _vm.resume();

        StepRequest currentStepReq = null;
        int stepsToTrace = 0;
        int stepCount = 0;
        boolean stop = false;

        EVENT_LOOP: while (!stop) {
            EventIterator ei = getNextEvent();

            Map<Location, ThreadReference> currentLocations = new HashMap<Location, ThreadReference>();

            while (ei.hasNext()) {
                Event e = ei.next();
                Env.traceVerbose("Got JDI event: " + e);
                eventHook(e);
                if (e instanceof VMDisconnectEvent)
                    break EVENT_LOOP;

                ThreadReference thread = null;
                Location location = null;
                boolean fullStackTrace = false;

                if (e instanceof ClassPrepareEvent) {
                    ClassPrepareEvent cpe = (ClassPrepareEvent) e;
                    classPrepareEventHook(cpe);

                    ReferenceType classRef = cpe.referenceType();

                    setBreakpoints(erm, breakpoints, classRef);

                } else if (e instanceof BreakpointEvent) {

                    BreakpointEvent bpe = (BreakpointEvent) e;
                    breakpointEventHook(bpe);
                    thread = bpe.thread();
                    location = bpe.location();
                    fullStackTrace = true;

                } else if (e instanceof StepEvent) {

                    StepEvent se = (StepEvent) e;
                    stepEventHook(se);
                    thread = se.thread();
                    location = se.location();
                }

                if (thread != null) {
                    try {
                        Env.traceDebug("Event thread suspends: " + thread.suspendCount());
                        if (thread.suspendCount() > 0)
                            Env.traceDebug("Stack trace:" + getStackTraceStr(thread.frames(), fullStackTrace));

                        currentLocations.put(location, thread);

                    } catch (IncompatibleThreadStateException ex) {
                        Env.traceNormal("Exception: ", ex);
                    }
                }

                if (++stepCount > MAX_EVENT_COUNT) {
                    Env.display("Maximum number of events reached ("
                            + MAX_EVENT_COUNT + ") for this test. Exiting.");
                    stop = true;
                }
            }

            for (Map.Entry<Location, ThreadReference> e : currentLocations.entrySet()) {
                final Location location = e.getKey();
                final ThreadReference thread = e.getValue();

                BreakpointInfo bpInfo = (BreakpointInfo) new BreakpointListIterator(breakpoints) {
                    @Override protected Object apply(BreakpointInfo bi) throws Throwable {
                        if ( location.method().name().equals(bi.methodName) && location.codeIndex() == bi.bci )
                            return bi;
                        else
                            return null;
                    }
                }.go();

                int s = 0;

                if (bpInfo != null) {
                    Env.traceNormal("Execution hit our breakpoint: [" + bpInfo.methodName + ":" + bpInfo.methodLine + "] on step " + stepCount);

                    bpInfo.hits++;
                    s = bpInfo.stepsToTrace;

                    if (bpInfo.stratumInfo != null) {
                        if ( ! StratumUtils.checkStratum(location, bpInfo.stratumInfo) )
                            markTestFailed("Stratum " + bpInfo.stratumInfo + " mismatch");
                    }

                    if ( bpInfo.subBreakpoints != null ) {
                        Env.traceNormal("Enabling sub-breakpoints");
                        for ( BreakpointInfo subBP : bpInfo.subBreakpoints ) {
                            if (  subBP.type == BreakpointInfo.Type.IMPLICIT )
                                continue;

                            if ( subBP.bpReq == null ) {
                                Env.complain("Breakpoint " + subBP + " was not set. Skipping.");
                                continue;
                            }

                            if ( subBP.bpReq.isEnabled() ) {
                                Env.traceVerbose("Breakpoint " + subBP + " is already enabled. Skipping.");
                                continue;
                            }

                            subBP.bpReq.enable();
                        }
                    }

                    if ( terminateWhenAllBPHit && areAllBreakpointsHit(breakpoints) ) {
                        Env.traceNormal("All breakpoints are hit. Terminating.");
                        stop = true;
                        s = 0;
                    }
                }

                if (s > 0) {
                    Env.traceVerbose("Stepping " + s + " step or breakpoint events from here");
                    stepsToTrace = s;

                    if (currentStepReq == null) {
                        currentStepReq = erm.createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
                        currentStepReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        currentStepReq.enable();
                    }
                } else {
                    if (currentStepReq != null && --stepsToTrace <= 0) {
                        Env.traceVerbose("Continue without stepping");
                        erm.deleteEventRequest(currentStepReq);
                        currentStepReq = null;
                    }
                }
            }

            Env.traceDebug("Resuming execution");
            _vm.resume();
        }

        new BreakpointListIterator(breakpoints) {
            @Override protected Object apply(BreakpointInfo bi) {
                if (!bi.isHit()) {
                        markTestFailed("Breakpoint for method "
                                     + bi.methodName
                                     + ": required hits "
                                     + (bi.requiredHits == null ? "> 0" : (" = " + bi.requiredHits))
                                     + "; actual hits = " + bi.hits);
                } else {
                    Env.display("Breakpoint for method " + bi.methodName + " was hit " + bi.hits + " times. OK.");
                }

                return null;
            }
        }.go();

        if (!debuggee.terminated()) {
            try {
                debuggee.dispose();
            } catch (VMDisconnectedException ignore) {
            }
        }

        debuggee.waitFor();
        return true;
    }

    private void setBreakpoints(
            final EventRequestManager erm,
            final List<BreakpointInfo> breakpoints,
            final ReferenceType classRef)
            throws Throwable {

        Env.traceNormal("Setting breakpoints for class [" + classRef + "]");

        new BreakpointListIterator(breakpoints) {
            @Override
            protected Object apply(BreakpointInfo bpInfo) throws Throwable {
                if ( bpInfo.className.equals(classRef.name()) ) {

                    List<Method> methods = classRef.methodsByName(bpInfo.methodName);
                    if (methods.size() == 0)
                        throw new Exception("No method named [" + bpInfo.methodName + "]");

                    Method method = (Method) methods.get(0);
                    List<Location> allLineLocations = method.allLineLocations();

                    Env.traceVerbose("Method [" + method.name() + "] locations: " + Arrays.toString(allLineLocations.toArray()));

                    if (bpInfo.methodLine > allLineLocations.size())
                        throw new Exception("TEST BUG: no breakpoint line " + bpInfo.methodLine);

                    Location lineLocation = (Location) allLineLocations.get(bpInfo.methodLine);
                    bpInfo.bci = lineLocation.codeIndex();
                    bpInfo.hits = 0;

                    if ( bpInfo.type == BreakpointInfo.Type.EXPLICIT ) {
                        BreakpointRequest bpReq = erm.createBreakpointRequest(lineLocation);
                        // bpReq.addThreadFilter(mainThread);
                        bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);

                        if ( ! bpInfo.isConditional )
                            bpReq.enable();

                        bpInfo.bpReq = bpReq;
                        Env.traceNormal("Breakpoint request for [" + method.name() + "]: " + bpReq);
                    } else {
                        Env.traceNormal("Implicit breakpoint  " + "[" + bpInfo.methodName + ":" + bpInfo.methodLine + "]");
                    }

                }

                return null;
            }
        }.go();
    }

    private static boolean areAllBreakpointsHit(List<BreakpointInfo> breakpoints) throws Throwable {
        return null == new BreakpointListIterator(breakpoints) {
            @Override
            protected Object apply(BreakpointInfo bi) throws Throwable {
                return bi.isHit() ? null : bi;
            }
        }.go();
    }

    public static String getStackTraceStr(List<StackFrame> frames, boolean full)
            throws AbsentInformationException {
        StringBuffer buf = new StringBuffer();

        int frameNum = 0;
        for (StackFrame f : frames) {
            Location l = f.location();

            buf.append(String.format("#%-4d", frameNum))
               .append(l.method())
               .append("\n        source: ")
               .append(l.sourcePath())
               .append(":")
               .append(l.lineNumber())
               .append("; bci=")
               .append(l.codeIndex())
               .append("\n        class:  ")
               .append(l.declaringType())
               .append("\n        strata: ")
               .append(StratumUtils.getStrataStr(f))
               .append("\n        locals: ");

            try {
                for (Map.Entry<LocalVariable, Value> m : f.getValues(f.visibleVariables()).entrySet()) {
                    LocalVariable lv = m.getKey();
                    buf.append("\n            ");

                    if (lv.isArgument()) {
                        buf.append("[arg] ");
                    }
                    buf.append(lv.name())
                       .append(" (")
                       .append(lv.typeName())
                       .append(") = [")
                       .append(m.getValue())
                       .append("]; ");
                }
            } catch (AbsentInformationException e) {
                buf.append("NO INFORMATION")
                   .append("\n        arguments: ");

                List<Value> argumentValues = f.getArgumentValues();

                if (argumentValues == null || argumentValues.size() == 0) {
                    buf.append("none");
                } else {
                    int n = 0;
                    for (Value v : argumentValues) {
                        buf.append("\n            arg");

                        if (v == null) {
                            buf.append(n)
                               .append(" [null]");
                        } else {
                            buf.append(n)
                               .append(" (")
                               .append(v.type())
                               .append(") = [")
                               .append(v)
                               .append("]; ");
                        }
                        n++;
                    }
                }
            }

            buf.append("\n\n");

            ++frameNum;
            if (!full && frameNum >= SHORT_STACK_TRACE_FRAMES_NUM) {
                buf.append("...\n");
                break;
            }
        }
        return buf.toString();
    }

    protected EventIterator getNextEvent() throws Throwable {
        EventSet eventSet = _eventQueue.remove(_jdiEventWaitTimeout);
        if (eventSet == null)
            throw new Exception("Timed out while waiting for an event");

        return eventSet.eventIterator();
    }

}
