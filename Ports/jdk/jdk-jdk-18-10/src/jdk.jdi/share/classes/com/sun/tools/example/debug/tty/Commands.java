/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.request.*;
import com.sun.tools.example.debug.expr.ExpressionParser;
import com.sun.tools.example.debug.expr.ParseException;

import java.text.*;
import java.util.*;
import java.io.*;

class Commands {

    abstract class AsyncExecution {
        abstract void action();

        AsyncExecution() {
            execute();
        }

        void execute() {
            /*
             * Save current thread and stack frame. (BugId 4296031)
             */
            final ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
            final int stackFrame = threadInfo == null? 0 : threadInfo.getCurrentFrameIndex();
            Thread thread = new Thread("asynchronous jdb command") {
                    @Override
                    public void run() {
                        try {
                            action();
                        } catch (UnsupportedOperationException uoe) {
                            //(BugId 4453329)
                            MessageOutput.println("Operation is not supported on the target VM");
                        } catch (Exception e) {
                            MessageOutput.println("Internal exception during operation:",
                                                  e.getMessage());
                        } finally {
                            /*
                             * This was an asynchronous command.  Events may have been
                             * processed while it was running.  Restore the thread and
                             * stack frame the user was looking at.  (BugId 4296031)
                             */
                            if (threadInfo != null) {
                                ThreadInfo.setCurrentThreadInfo(threadInfo);
                                try {
                                    threadInfo.setCurrentFrameIndex(stackFrame);
                                } catch (IncompatibleThreadStateException e) {
                                    MessageOutput.println("Current thread isnt suspended.");
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    MessageOutput.println("Requested stack frame is no longer active:",
                                                          new Object []{stackFrame});
                                }
                            }
                            MessageOutput.printPrompt();
                        }
                    }
                };
            thread.start();
        }
    }

    Commands() {
    }

    private Value evaluate(String expr) {
        Value result = null;
        ExpressionParser.GetFrame frameGetter = null;
        try {
            final ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
            if ((threadInfo != null) && (threadInfo.getCurrentFrame() != null)) {
                frameGetter = new ExpressionParser.GetFrame() {
                        @Override
                        public StackFrame get() throws IncompatibleThreadStateException {
                            return threadInfo.getCurrentFrame();
                        }
                    };
            }
            result = ExpressionParser.evaluate(expr, Env.vm(), frameGetter);
        } catch (InvocationException ie) {
            MessageOutput.println("Exception in expression:",
                                  ie.exception().referenceType().name());
        } catch (Exception ex) {
            String exMessage = ex.getMessage();
            if (exMessage == null) {
                MessageOutput.printException(exMessage, ex);
            } else {
                String s;
                try {
                    s = MessageOutput.format(exMessage);
                } catch (MissingResourceException mex) {
                    s = ex.toString();
                }
                MessageOutput.printDirectln(s);// Special case: use printDirectln()
            }
        }
        return result;
    }

    private String getStringValue() {
         Value val = null;
         String valStr = null;
         try {
              val = ExpressionParser.getMassagedValue();
              valStr = val.toString();
         } catch (ParseException e) {
              String msg = e.getMessage();
              if (msg == null) {
                  MessageOutput.printException(msg, e);
              } else {
                  String s;
                  try {
                      s = MessageOutput.format(msg);
                  } catch (MissingResourceException mex) {
                      s = e.toString();
                  }
                  MessageOutput.printDirectln(s);
              }
         }
         return valStr;
    }

    private ThreadInfo doGetThread(String idToken) {
        ThreadInfo threadInfo = ThreadInfo.getThreadInfo(idToken);
        if (threadInfo == null) {
            MessageOutput.println("is not a valid thread id", idToken);
        }
        return threadInfo;
    }

    String typedName(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.name());
        sb.append("(");

        List<String> args = method.argumentTypeNames();
        int lastParam = args.size() - 1;
        // output param types except for the last
        for (int ii = 0; ii < lastParam; ii++) {
            sb.append(args.get(ii));
            sb.append(", ");
        }
        if (lastParam >= 0) {
            // output the last param
            String lastStr = args.get(lastParam);
            if (method.isVarArgs()) {
                // lastParam is an array.  Replace the [] with ...
                sb.append(lastStr.substring(0, lastStr.length() - 2));
                sb.append("...");
            } else {
                sb.append(lastStr);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    void commandConnectors(VirtualMachineManager vmm) {
        Collection<Connector> ccs = vmm.allConnectors();
        if (ccs.isEmpty()) {
            MessageOutput.println("Connectors available");
        }
        for (Connector cc : ccs) {
            String transportName =
                cc.transport() == null ? "null" : cc.transport().name();
            MessageOutput.println();
            MessageOutput.println("Connector and Transport name",
                                  new Object [] {cc.name(), transportName});
            MessageOutput.println("Connector description", cc.description());

            for (Connector.Argument aa : cc.defaultArguments().values()) {
                    MessageOutput.println();

                    boolean requiredArgument = aa.mustSpecify();
                    if (aa.value() == null || aa.value() == "") {
                        //no current value and no default.
                        MessageOutput.println(requiredArgument ?
                                              "Connector required argument nodefault" :
                                              "Connector argument nodefault", aa.name());
                    } else {
                        MessageOutput.println(requiredArgument ?
                                              "Connector required argument default" :
                                              "Connector argument default",
                                              new Object [] {aa.name(), aa.value()});
                    }
                    MessageOutput.println("Connector description", aa.description());

                }
            }

    }

    void commandClasses() {
        StringBuilder classList = new StringBuilder();
        for (ReferenceType refType : Env.vm().allClasses()) {
            classList.append(refType.name());
            classList.append("\n");
        }
        MessageOutput.print("** classes list **", classList.toString());
    }

    void commandClass(StringTokenizer t) {

        if (!t.hasMoreTokens()) {
            MessageOutput.println("No class specified.");
            return;
        }

        String idClass = t.nextToken();
        boolean showAll = false;

        if (t.hasMoreTokens()) {
            if (t.nextToken().toLowerCase().equals("all")) {
                showAll = true;
            } else {
                MessageOutput.println("Invalid option on class command");
                return;
            }
        }
        ReferenceType type = Env.getReferenceTypeFromToken(idClass);
        if (type == null) {
            MessageOutput.println("is not a valid id or class name", idClass);
            return;
        }
        if (type instanceof ClassType) {
            ClassType clazz = (ClassType)type;
            MessageOutput.println("Class:", clazz.name());

            ClassType superclass = clazz.superclass();
            while (superclass != null) {
                MessageOutput.println("extends:", superclass.name());
                superclass = showAll ? superclass.superclass() : null;
            }

            List<InterfaceType> interfaces =
                showAll ? clazz.allInterfaces() : clazz.interfaces();
            for (InterfaceType interfaze : interfaces) {
                MessageOutput.println("implements:", interfaze.name());
            }

            for (ClassType sub : clazz.subclasses()) {
                MessageOutput.println("subclass:", sub.name());
            }
            for (ReferenceType nest : clazz.nestedTypes()) {
                MessageOutput.println("nested:", nest.name());
            }
        } else if (type instanceof InterfaceType) {
            InterfaceType interfaze = (InterfaceType)type;
            MessageOutput.println("Interface:", interfaze.name());
            for (InterfaceType superinterface : interfaze.superinterfaces()) {
                MessageOutput.println("extends:", superinterface.name());
            }
            for (InterfaceType sub : interfaze.subinterfaces()) {
                MessageOutput.println("subinterface:", sub.name());
            }
            for (ClassType implementor : interfaze.implementors()) {
                MessageOutput.println("implementor:", implementor.name());
            }
            for (ReferenceType nest : interfaze.nestedTypes()) {
                MessageOutput.println("nested:", nest.name());
            }
        } else {  // array type
            ArrayType array = (ArrayType)type;
            MessageOutput.println("Array:", array.name());
        }
    }

    void commandMethods(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No class specified.");
            return;
        }

        String idClass = t.nextToken();
        ReferenceType cls = Env.getReferenceTypeFromToken(idClass);
        if (cls != null) {
            StringBuilder methodsList = new StringBuilder();
            for (Method method : cls.allMethods()) {
                methodsList.append(method.declaringType().name());
                methodsList.append(" ");
                methodsList.append(typedName(method));
                methodsList.append('\n');
            }
            MessageOutput.print("** methods list **", methodsList.toString());
        } else {
            MessageOutput.println("is not a valid id or class name", idClass);
        }
    }

    void commandFields(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No class specified.");
            return;
        }

        String idClass = t.nextToken();
        ReferenceType cls = Env.getReferenceTypeFromToken(idClass);
        if (cls != null) {
            List<Field> fields = cls.allFields();
            List<Field> visible = cls.visibleFields();
            StringBuilder fieldsList = new StringBuilder();
            for (Field field : fields) {
                String s;
                if (!visible.contains(field)) {
                    s = MessageOutput.format("list field typename and name hidden",
                                             new Object [] {field.typeName(),
                                                            field.name()});
                } else if (!field.declaringType().equals(cls)) {
                    s = MessageOutput.format("list field typename and name inherited",
                                             new Object [] {field.typeName(),
                                                            field.name(),
                                                            field.declaringType().name()});
                } else {
                    s = MessageOutput.format("list field typename and name",
                                             new Object [] {field.typeName(),
                                                            field.name()});
                }
                fieldsList.append(s);
            }
            MessageOutput.print("** fields list **", fieldsList.toString());
        } else {
            MessageOutput.println("is not a valid id or class name", idClass);
        }
    }

    private void printThreadGroup(ThreadGroupReference tg) {
        ThreadIterator threadIter = new ThreadIterator(tg);

        MessageOutput.println("Thread Group:", tg.name());
        int maxIdLength = 0;
        int maxNameLength = 0;
        while (threadIter.hasNext()) {
            ThreadReference thr = threadIter.next();
            maxIdLength = Math.max(maxIdLength,
                                   Env.description(thr).length());
            maxNameLength = Math.max(maxNameLength,
                                     thr.name().length());
        }

        threadIter = new ThreadIterator(tg);
        while (threadIter.hasNext()) {
            ThreadReference thr = threadIter.next();
            if (thr.threadGroup() == null) {
                continue;
            }
            // Note any thread group changes
            if (!thr.threadGroup().equals(tg)) {
                tg = thr.threadGroup();
                MessageOutput.println("Thread Group:", tg.name());
            }

            /*
             * Do a bit of filling with whitespace to get thread ID
             * and thread names to line up in the listing, and also
             * allow for proper localization.  This also works for
             * very long thread names, at the possible cost of lines
             * being wrapped by the display device.
             */
            StringBuilder idBuffer = new StringBuilder(Env.description(thr));
            for (int i = idBuffer.length(); i < maxIdLength; i++) {
                idBuffer.append(" ");
            }
            StringBuilder nameBuffer = new StringBuilder(thr.name());
            for (int i = nameBuffer.length(); i < maxNameLength; i++) {
                nameBuffer.append(" ");
            }

            /*
             * Select the output format to use based on thread status
             * and breakpoint.
             */
            String statusFormat;
            switch (thr.status()) {
            case ThreadReference.THREAD_STATUS_UNKNOWN:
                if (thr.isAtBreakpoint()) {
                    statusFormat = "Thread description name unknownStatus BP";
                } else {
                    statusFormat = "Thread description name unknownStatus";
                }
                break;
            case ThreadReference.THREAD_STATUS_ZOMBIE:
                if (thr.isAtBreakpoint()) {
                    statusFormat = "Thread description name zombieStatus BP";
                } else {
                    statusFormat = "Thread description name zombieStatus";
                }
                break;
            case ThreadReference.THREAD_STATUS_RUNNING:
                if (thr.isAtBreakpoint()) {
                    statusFormat = "Thread description name runningStatus BP";
                } else {
                    statusFormat = "Thread description name runningStatus";
                }
                break;
            case ThreadReference.THREAD_STATUS_SLEEPING:
                if (thr.isAtBreakpoint()) {
                    statusFormat = "Thread description name sleepingStatus BP";
                } else {
                    statusFormat = "Thread description name sleepingStatus";
                }
                break;
            case ThreadReference.THREAD_STATUS_MONITOR:
                if (thr.isAtBreakpoint()) {
                    statusFormat = "Thread description name waitingStatus BP";
                } else {
                    statusFormat = "Thread description name waitingStatus";
                }
                break;
            case ThreadReference.THREAD_STATUS_WAIT:
                if (thr.isAtBreakpoint()) {
                    statusFormat = "Thread description name condWaitstatus BP";
                } else {
                    statusFormat = "Thread description name condWaitstatus";
                }
                break;
            default:
                throw new InternalError(MessageOutput.format("Invalid thread status."));
            }
            MessageOutput.println(statusFormat,
                                  new Object [] {idBuffer.toString(),
                                                 nameBuffer.toString()});
        }
    }

    void commandThreads(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            printThreadGroup(ThreadInfo.group());
            return;
        }
        String name = t.nextToken();
        ThreadGroupReference tg = ThreadGroupIterator.find(name);
        if (tg == null) {
            MessageOutput.println("is not a valid threadgroup name", name);
        } else {
            printThreadGroup(tg);
        }
    }

    void commandThreadGroups() {
        ThreadGroupIterator it = new ThreadGroupIterator();
        int cnt = 0;
        while (it.hasNext()) {
            ThreadGroupReference tg = it.nextThreadGroup();
            ++cnt;
            MessageOutput.println("thread group number description name",
                                  new Object [] { Integer.valueOf(cnt),
                                                  Env.description(tg),
                                                  tg.name()});
        }
    }

    void commandThread(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Thread number not specified.");
            return;
        }
        ThreadInfo threadInfo = doGetThread(t.nextToken());
        if (threadInfo != null) {
            ThreadInfo.setCurrentThreadInfo(threadInfo);
        }
    }

    void commandThreadGroup(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Threadgroup name not specified.");
            return;
        }
        String name = t.nextToken();
        ThreadGroupReference tg = ThreadGroupIterator.find(name);
        if (tg == null) {
            MessageOutput.println("is not a valid threadgroup name", name);
        } else {
            ThreadInfo.setThreadGroup(tg);
        }
    }

    void commandRun(StringTokenizer t) {
        /*
         * The 'run' command makes little sense in a
         * that doesn't support restarts or multiple VMs. However,
         * this is an attempt to emulate the behavior of the old
         * JDB as much as possible. For new users and implementations
         * it is much more straightforward to launch immedidately
         * with the -launch option.
         */
        VMConnection connection = Env.connection();
        if (!connection.isLaunch()) {
            if (!t.hasMoreTokens()) {
                commandCont();
            } else {
                MessageOutput.println("run <args> command is valid only with launched VMs");
            }
            return;
        }
        if (connection.isOpen()) {
            MessageOutput.println("VM already running. use cont to continue after events.");
            return;
        }

        /*
         * Set the main class and any arguments. Note that this will work
         * only with the standard launcher, "com.sun.jdi.CommandLineLauncher"
         */
        String args;
        if (t.hasMoreTokens()) {
            args = t.nextToken("");
            boolean argsSet = connection.setConnectorArg("main", args);
            if (!argsSet) {
                MessageOutput.println("Unable to set main class and arguments");
                return;
            }
        } else {
            args = connection.connectorArg("main");
            if (args.length() == 0) {
                MessageOutput.println("Main class and arguments must be specified");
                return;
            }
        }
        MessageOutput.println("run", args);

        /*
         * Launch the VM.
         */
        connection.open();

    }

    void commandLoad(StringTokenizer t) {
        MessageOutput.println("The load command is no longer supported.");
    }

    private List<ThreadReference> allThreads(ThreadGroupReference group) {
        List<ThreadReference> list = new ArrayList<ThreadReference>();
        list.addAll(group.threads());
        for (ThreadGroupReference child : group.threadGroups()) {
            list.addAll(allThreads(child));
        }
        return list;
    }

    void commandSuspend(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            Env.vm().suspend();
            MessageOutput.println("All threads suspended.");
        } else {
            while (t.hasMoreTokens()) {
                ThreadInfo threadInfo = doGetThread(t.nextToken());
                if (threadInfo != null) {
                    threadInfo.getThread().suspend();
                }
            }
        }
    }

    void commandResume(StringTokenizer t) {
         if (!t.hasMoreTokens()) {
             ThreadInfo.invalidateAll();
             Env.vm().resume();
             MessageOutput.println("All threads resumed.");
         } else {
             while (t.hasMoreTokens()) {
                ThreadInfo threadInfo = doGetThread(t.nextToken());
                if (threadInfo != null) {
                    threadInfo.invalidate();
                    threadInfo.getThread().resume();
                }
            }
        }
    }

    void commandCont() {
        if (ThreadInfo.getCurrentThreadInfo() == null) {
            MessageOutput.println("Nothing suspended.");
            return;
        }
        ThreadInfo.invalidateAll();
        Env.vm().resume();
    }

    void clearPreviousStep(ThreadReference thread) {
        /*
         * A previous step may not have completed on this thread;
         * if so, it gets removed here.
         */
         EventRequestManager mgr = Env.vm().eventRequestManager();
         for (StepRequest request : mgr.stepRequests()) {
             if (request.thread().equals(thread)) {
                 mgr.deleteEventRequest(request);
                 break;
             }
         }
    }
    /* step
     *
     */
    void commandStep(StringTokenizer t) {
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("Nothing suspended.");
            return;
        }
        int depth;
        if (t.hasMoreTokens() &&
                  t.nextToken().toLowerCase().equals("up")) {
            depth = StepRequest.STEP_OUT;
        } else {
            depth = StepRequest.STEP_INTO;
        }

        clearPreviousStep(threadInfo.getThread());
        EventRequestManager reqMgr = Env.vm().eventRequestManager();
        StepRequest request = reqMgr.createStepRequest(threadInfo.getThread(),
                                                       StepRequest.STEP_LINE, depth);
        if (depth == StepRequest.STEP_INTO) {
            Env.addExcludes(request);
        }
        // We want just the next step event and no others
        request.addCountFilter(1);
        request.enable();
        ThreadInfo.invalidateAll();
        Env.vm().resume();
    }

    /* stepi
     * step instruction.
     */
    void commandStepi() {
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("Nothing suspended.");
            return;
        }
        clearPreviousStep(threadInfo.getThread());
        EventRequestManager reqMgr = Env.vm().eventRequestManager();
        StepRequest request = reqMgr.createStepRequest(threadInfo.getThread(),
                                                       StepRequest.STEP_MIN,
                                                       StepRequest.STEP_INTO);
        Env.addExcludes(request);
        // We want just the next step event and no others
        request.addCountFilter(1);
        request.enable();
        ThreadInfo.invalidateAll();
        Env.vm().resume();
    }

    void commandNext() {
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("Nothing suspended.");
            return;
        }
        clearPreviousStep(threadInfo.getThread());
        EventRequestManager reqMgr = Env.vm().eventRequestManager();
        StepRequest request = reqMgr.createStepRequest(threadInfo.getThread(),
                                                       StepRequest.STEP_LINE,
                                                       StepRequest.STEP_OVER);
        Env.addExcludes(request);
        // We want just the next step event and no others
        request.addCountFilter(1);
        request.enable();
        ThreadInfo.invalidateAll();
        Env.vm().resume();
    }

    void doKill(ThreadReference thread, StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No exception object specified.");
            return;
        }
        String expr = t.nextToken("");
        Value val = evaluate(expr);
        if ((val != null) && (val instanceof ObjectReference)) {
            try {
                thread.stop((ObjectReference)val);
                MessageOutput.println("killed", thread.toString());
            } catch (InvalidTypeException e) {
                MessageOutput.println("Invalid exception object");
            }
        } else {
            MessageOutput.println("Expression must evaluate to an object");
        }
    }

    void doKillThread(final ThreadReference threadToKill,
                      final StringTokenizer tokenizer) {
        new AsyncExecution() {
                @Override
                void action() {
                    doKill(threadToKill, tokenizer);
                }
            };
    }

    void commandKill(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Usage: kill <thread id> <throwable>");
            return;
        }
        ThreadInfo threadInfo = doGetThread(t.nextToken());
        if (threadInfo != null) {
            MessageOutput.println("killing thread:", threadInfo.getThread().name());
            doKillThread(threadInfo.getThread(), t);
            return;
        }
    }

    void listCaughtExceptions() {
        boolean noExceptions = true;

        // Print a listing of the catch patterns currently in place
        for (EventRequestSpec spec : Env.specList.eventRequestSpecs()) {
            if (spec instanceof ExceptionSpec) {
                if (noExceptions) {
                    noExceptions = false;
                    MessageOutput.println("Exceptions caught:");
                }
                MessageOutput.println("tab", spec.toString());
            }
        }
        if (noExceptions) {
            MessageOutput.println("No exceptions caught.");
        }
    }

    private EventRequestSpec parseExceptionSpec(StringTokenizer t) {
        String notification = t.nextToken();
        boolean notifyCaught = false;
        boolean notifyUncaught = false;
        EventRequestSpec spec = null;
        String classPattern = null;

        if (notification.equals("uncaught")) {
            notifyCaught = false;
            notifyUncaught = true;
        } else if (notification.equals("caught")) {
            notifyCaught = true;
            notifyUncaught = false;
        } else if (notification.equals("all")) {
            notifyCaught = true;
            notifyUncaught = true;
        } else {
            /*
             * Handle the same as "all" for backward
             * compatibility with existing .jdbrc files.
             *
             * Insert an "all" and take the current token as the
             * intended classPattern
             *
             */
            notifyCaught = true;
            notifyUncaught = true;
            classPattern = notification;
        }
        if (classPattern == null && t.hasMoreTokens()) {
            classPattern = t.nextToken();
        }
        if ((classPattern != null) && (notifyCaught || notifyUncaught)) {
            try {
                spec = Env.specList.createExceptionCatch(classPattern,
                                                         notifyCaught,
                                                         notifyUncaught);
            } catch (ClassNotFoundException exc) {
                MessageOutput.println("is not a valid class name", classPattern);
            }
        }
        return spec;
    }

    void commandCatchException(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            listCaughtExceptions();
        } else {
            EventRequestSpec spec = parseExceptionSpec(t);
            if (spec != null) {
                resolveNow(spec);
            } else {
                MessageOutput.println("Usage: catch exception");
            }
        }
    }

    void commandIgnoreException(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            listCaughtExceptions();
        } else {
            EventRequestSpec spec = parseExceptionSpec(t);
            if (Env.specList.delete(spec)) {
                MessageOutput.println("Removed:", spec.toString());
            } else {
                if (spec != null) {
                    MessageOutput.println("Not found:", spec.toString());
                }
                MessageOutput.println("Usage: ignore exception");
            }
        }
    }

    void commandUp(StringTokenizer t) {
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("Current thread not set.");
            return;
        }

        int nLevels = 1;
        if (t.hasMoreTokens()) {
            String idToken = t.nextToken();
            int i;
            try {
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setParseIntegerOnly(true);
                Number n = nf.parse(idToken);
                i = n.intValue();
            } catch (java.text.ParseException jtpe) {
                i = 0;
            }
            if (i <= 0) {
                MessageOutput.println("Usage: up [n frames]");
                return;
            }
            nLevels = i;
        }

        try {
            threadInfo.up(nLevels);
        } catch (IncompatibleThreadStateException e) {
            MessageOutput.println("Current thread isnt suspended.");
        } catch (ArrayIndexOutOfBoundsException e) {
            MessageOutput.println("End of stack.");
        }
    }

    void commandDown(StringTokenizer t) {
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("Current thread not set.");
            return;
        }

        int nLevels = 1;
        if (t.hasMoreTokens()) {
            String idToken = t.nextToken();
            int i;
            try {
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setParseIntegerOnly(true);
                Number n = nf.parse(idToken);
                i = n.intValue();
            } catch (java.text.ParseException jtpe) {
                i = 0;
            }
            if (i <= 0) {
                MessageOutput.println("Usage: down [n frames]");
                return;
            }
            nLevels = i;
        }

        try {
            threadInfo.down(nLevels);
        } catch (IncompatibleThreadStateException e) {
            MessageOutput.println("Current thread isnt suspended.");
        } catch (ArrayIndexOutOfBoundsException e) {
            MessageOutput.println("End of stack.");
        }
    }

    private void dumpStack(ThreadInfo threadInfo, boolean showPC) {
        List<StackFrame> stack = null;
        try {
            stack = threadInfo.getStack();
        } catch (IncompatibleThreadStateException e) {
            MessageOutput.println("Current thread isnt suspended.");
            return;
        }
        if (stack == null) {
            MessageOutput.println("Thread is not running (no stack).");
        } else {
            int nFrames = stack.size();
            for (int i = threadInfo.getCurrentFrameIndex(); i < nFrames; i++) {
                StackFrame frame = stack.get(i);
                dumpFrame (i, showPC, frame);
            }
        }
    }

    private void dumpFrame (int frameNumber, boolean showPC, StackFrame frame) {
        Location loc = frame.location();
        long pc = -1;
        if (showPC) {
            pc = loc.codeIndex();
        }
        Method meth = loc.method();

        long lineNumber = loc.lineNumber();
        String methodInfo = null;
        if (meth.isNative()) {
            methodInfo = MessageOutput.format("native method");
        } else if (lineNumber != -1) {
            try {
                methodInfo = loc.sourceName() +
                    MessageOutput.format("line number",
                                         new Object [] {Long.valueOf(lineNumber)});
            } catch (AbsentInformationException e) {
                methodInfo = MessageOutput.format("unknown");
            }
        }
        if (pc != -1) {
            MessageOutput.println("stack frame dump with pc",
                                  new Object [] {(frameNumber + 1),
                                                 meth.declaringType().name(),
                                                 meth.name(),
                                                 methodInfo,
                                                 Long.valueOf(pc)});
        } else {
            MessageOutput.println("stack frame dump",
                                  new Object [] {(frameNumber + 1),
                                                 meth.declaringType().name(),
                                                 meth.name(),
                                                 methodInfo});
        }
    }

    void commandWhere(StringTokenizer t, boolean showPC) {
        if (!t.hasMoreTokens()) {
            ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
            if (threadInfo == null) {
                MessageOutput.println("No thread specified.");
                return;
            }
            dumpStack(threadInfo, showPC);
        } else {
            String token = t.nextToken();
            if (token.toLowerCase().equals("all")) {
                for (ThreadInfo threadInfo : ThreadInfo.threads()) {
                    MessageOutput.println("Thread:",
                                          threadInfo.getThread().name());
                    dumpStack(threadInfo, showPC);
                }
            } else {
                ThreadInfo threadInfo = doGetThread(token);
                if (threadInfo != null) {
                    ThreadInfo.setCurrentThreadInfo(threadInfo);
                    dumpStack(threadInfo, showPC);
                }
            }
        }
    }

    void commandInterrupt(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
            if (threadInfo == null) {
                MessageOutput.println("No thread specified.");
                return;
            }
            threadInfo.getThread().interrupt();
        } else {
            ThreadInfo threadInfo = doGetThread(t.nextToken());
            if (threadInfo != null) {
                threadInfo.getThread().interrupt();
            }
        }
    }

    void commandMemory() {
        MessageOutput.println("The memory command is no longer supported.");
    }

    void commandGC() {
        MessageOutput.println("The gc command is no longer necessary.");
    }

    /*
     * The next two methods are used by this class and by EventHandler
     * to print consistent locations and error messages.
     */
    static String locationString(Location loc) {
        return MessageOutput.format("locationString",
                                    new Object [] {loc.declaringType().name(),
                                                   loc.method().name(),
                                                   Integer.valueOf(loc.lineNumber()),
                                                   Long.valueOf(loc.codeIndex())});
    }

    void listBreakpoints() {
        boolean noBreakpoints = true;

        // Print set breakpoints
        for (EventRequestSpec spec : Env.specList.eventRequestSpecs()) {
            if (spec instanceof BreakpointSpec) {
                if (noBreakpoints) {
                    noBreakpoints = false;
                    MessageOutput.println("Breakpoints set:");
                }
                MessageOutput.println("tab", spec.toString());
            }
        }
        if (noBreakpoints) {
            MessageOutput.println("No breakpoints set.");
        }
    }


    private void printBreakpointCommandUsage(String usageMessage) {
        MessageOutput.println(usageMessage);
    }

    protected BreakpointSpec parseBreakpointSpec(StringTokenizer t, String next_token,
                                                 ThreadReference threadFilter,
                                                 String usageMessage) {
        BreakpointSpec breakpoint = null;
        try {
            String token = next_token;

            // We can't use hasMoreTokens here because it will cause any leading
            // paren to be lost.
            String rest;
            try {
                rest = t.nextToken("").trim();
            } catch (NoSuchElementException e) {
                rest = null;
            }

            if ((rest != null) && rest.startsWith(":")) {
                t = new StringTokenizer(rest.substring(1));
                String classId = token;
                String lineToken = t.nextToken();

                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setParseIntegerOnly(true);
                Number n;
                try {
                    n = nf.parse(lineToken);
                } catch (java.text.ParseException pe) {
                    MessageOutput.println("Invalid line number specified");
                    printBreakpointCommandUsage(usageMessage);
                    return null;
                }
                int lineNumber = n.intValue();

                if (t.hasMoreTokens()) {
                    MessageOutput.println("Extra tokens after breakpoint location");
                    printBreakpointCommandUsage(usageMessage);
                    return null;
                }
                try {
                    breakpoint = Env.specList.createBreakpoint(classId,
                                                               lineNumber, threadFilter);
                } catch (ClassNotFoundException exc) {
                    MessageOutput.println("is not a valid class name", classId);
                }
            } else {
                // Try stripping method from class.method token.
                int idot = token.lastIndexOf('.');
                if ( (idot <= 0) ||                     /* No dot or dot in first char */
                     (idot >= token.length() - 1) ) { /* dot in last char */
                    MessageOutput.println("Invalid <class>.<method_name> specification");
                    printBreakpointCommandUsage(usageMessage);
                    return null;
                }
                String methodName = token.substring(idot + 1);
                String classId = token.substring(0, idot);
                List<String> argumentList = null;
                if (rest != null) {
                    if (!rest.startsWith("(") || !rest.endsWith(")")) {
                        MessageOutput.println("Invalid <method_name> specification:",
                                              methodName + rest);
                        printBreakpointCommandUsage(usageMessage);
                        return null;
                    }
                    // Trim the parens
                    rest = rest.substring(1, rest.length() - 1);

                    argumentList = new ArrayList<String>();
                    t = new StringTokenizer(rest, ",");
                    while (t.hasMoreTokens()) {
                        argumentList.add(t.nextToken());
                    }
                }
                try {
                    breakpoint = Env.specList.createBreakpoint(classId,
                                                               methodName,
                                                               threadFilter,
                                                               argumentList);
                } catch (MalformedMemberNameException exc) {
                    MessageOutput.println("is not a valid method name", methodName);
                } catch (ClassNotFoundException exc) {
                    MessageOutput.println("is not a valid class name", classId);
                }
            }
        } catch (Exception e) {
            printBreakpointCommandUsage(usageMessage);
            return null;
        }
        return breakpoint;
    }

    private void resolveNow(EventRequestSpec spec) {
        boolean success = Env.specList.addEagerlyResolve(spec);
        if (success && !spec.isResolved()) {
            MessageOutput.println("Deferring.", spec.toString());
        }
    }

    void commandDbgTrace(StringTokenizer t) {
        int traceFlags;
        if (t.hasMoreTokens()) {
            String flagStr = t.nextToken();
            try {
                traceFlags = Integer.decode(flagStr).intValue();
            } catch (NumberFormatException nfe) {
                MessageOutput.println("dbgtrace command value must be an integer:", flagStr);
                return;
            }
        } else {
            traceFlags = VirtualMachine.TRACE_ALL;
        }
        Env.setTraceFlags(traceFlags);
    }

    void commandStop(StringTokenizer t) {
        byte suspendPolicy = EventRequest.SUSPEND_ALL;
        ThreadReference threadFilter = null;

        /*
         * Allowed syntax:
         *    stop [go|thread] [<thread_id>] <at|in> <location>
         * If no options are given, the current list of breakpoints is printed.
         * If "go" is specified, then immediately resume after stopping. No threads are suspended.
         * If "thread" is specified, then only suspend the thread we stop in.
         * If neither "go" nor "thread" are specified, then suspend all threads.
         * If an integer <thread_id> is specified, then only stop in the specified thread.
         * <location> can either be a line number or a method:
         *    - <class id>:<line>
         *    - <class id>.<method>[(argument_type,...)]
         */

        if (!t.hasMoreTokens()) {
            listBreakpoints();
            return;
        }

        String token = t.nextToken();

        /* Check for "go" or "thread" modifiers. */
        if (token.equals("go") && t.hasMoreTokens()) {
            suspendPolicy = EventRequest.SUSPEND_NONE;
            token = t.nextToken();
        } else if (token.equals("thread") && t.hasMoreTokens()) {
            suspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
            token = t.nextToken();
        }

        /* Handle <thread_id> modifier. */
        if (!token.equals("at") && !token.equals("in")) {
            Long threadid;
            try {
                threadid = Long.decode(token);
            } catch (NumberFormatException nfe) {
                MessageOutput.println("Expected at, in, or an integer <thread_id>:", token);
                printBreakpointCommandUsage("printstopcommandusage");
                return;
            }
            try {
                ThreadInfo threadInfo = ThreadInfo.getThreadInfo(token);
                if (threadInfo == null) {
                    MessageOutput.println("Invalid <thread_id>:", token);
                    return;
                }
                threadFilter = threadInfo.getThread();
                token = t.nextToken(BreakpointSpec.locationTokenDelimiter);
            } catch (VMNotConnectedException vmnce) {
                MessageOutput.println("<thread_id> option not valid until the VM is started with the run command");
                return;
            }

        }

        /* Make sure "at" or "in" comes next. */
        if (!token.equals("at") && !token.equals("in")) {
            MessageOutput.println("Missing at or in");
            printBreakpointCommandUsage("printstopcommandusage");
            return;
        }

        token = t.nextToken(BreakpointSpec.locationTokenDelimiter);

        BreakpointSpec spec = parseBreakpointSpec(t, token, threadFilter, "printstopcommandusage");
        if (spec != null) {
            spec.suspendPolicy = suspendPolicy;
            resolveNow(spec);
        }
    }

    void commandClear(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            listBreakpoints();
            return;
        }

        String token = t.nextToken(BreakpointSpec.locationTokenDelimiter);
        BreakpointSpec spec = parseBreakpointSpec(t, token, null, "printclearcommandusage");
        if (spec != null) {
            if (Env.specList.delete(spec)) {
                MessageOutput.println("Removed:", spec.toString());
            } else {
                MessageOutput.println("Not found:", spec.toString());
            }
        }
    }

    private List<WatchpointSpec> parseWatchpointSpec(StringTokenizer t) {
        List<WatchpointSpec> list = new ArrayList<WatchpointSpec>();
        boolean access = false;
        boolean modification = false;
        int suspendPolicy = EventRequest.SUSPEND_ALL;

        String fieldName = t.nextToken();
        if (fieldName.equals("go")) {
            suspendPolicy = EventRequest.SUSPEND_NONE;
            fieldName = t.nextToken();
        } else if (fieldName.equals("thread")) {
            suspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
            fieldName = t.nextToken();
        }
        if (fieldName.equals("access")) {
            access = true;
            fieldName = t.nextToken();
        } else if (fieldName.equals("all")) {
            access = true;
            modification = true;
            fieldName = t.nextToken();
        } else {
            modification = true;
        }
        int dot = fieldName.lastIndexOf('.');
        if (dot < 0) {
            MessageOutput.println("Class containing field must be specified.");
            return list;
        }
        String className = fieldName.substring(0, dot);
        fieldName = fieldName.substring(dot+1);

        try {
            WatchpointSpec spec;
            if (access) {
                spec = Env.specList.createAccessWatchpoint(className,
                                                           fieldName);
                spec.suspendPolicy = suspendPolicy;
                list.add(spec);
            }
            if (modification) {
                spec = Env.specList.createModificationWatchpoint(className,
                                                                 fieldName);
                spec.suspendPolicy = suspendPolicy;
                list.add(spec);
            }
        } catch (MalformedMemberNameException exc) {
            MessageOutput.println("is not a valid field name", fieldName);
        } catch (ClassNotFoundException exc) {
            MessageOutput.println("is not a valid class name", className);
        }
        return list;
    }

    void commandWatch(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Field to watch not specified");
            return;
        }

        for (WatchpointSpec spec : parseWatchpointSpec(t)) {
            resolveNow(spec);
        }
    }

    void commandUnwatch(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Field to unwatch not specified");
            return;
        }

        for (WatchpointSpec spec : parseWatchpointSpec(t)) {
            if (Env.specList.delete(spec)) {
                MessageOutput.println("Removed:", spec.toString());
            } else {
                MessageOutput.println("Not found:", spec.toString());
            }
        }
    }

    void turnOnExitTrace(ThreadInfo threadInfo, int suspendPolicy) {
        EventRequestManager erm = Env.vm().eventRequestManager();
        MethodExitRequest exit = erm.createMethodExitRequest();
        if (threadInfo != null) {
            exit.addThreadFilter(threadInfo.getThread());
        }
        Env.addExcludes(exit);
        exit.setSuspendPolicy(suspendPolicy);
        exit.enable();

    }

    static String methodTraceCommand = null;

    void commandTrace(StringTokenizer t) {
        String modif;
        int suspendPolicy = EventRequest.SUSPEND_ALL;
        ThreadInfo threadInfo = null;
        String goStr = " ";

        /*
         * trace [go] methods [thread]
         * trace [go] method exit | exits [thread]
         */
        if (t.hasMoreTokens()) {
            modif = t.nextToken();
            if (modif.equals("go")) {
                suspendPolicy = EventRequest.SUSPEND_NONE;
                goStr = " go ";
                if (t.hasMoreTokens()) {
                    modif = t.nextToken();
                }
            } else if (modif.equals("thread")) {
                // this is undocumented as it doesn't work right.
                suspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
                if (t.hasMoreTokens()) {
                    modif = t.nextToken();
                }
            }

            if  (modif.equals("method")) {
                String traceCmd = null;

                if (t.hasMoreTokens()) {
                    String modif1 = t.nextToken();
                    if (modif1.equals("exits") || modif1.equals("exit")) {
                        if (t.hasMoreTokens()) {
                            threadInfo = doGetThread(t.nextToken());
                        }
                        if (modif1.equals("exit")) {
                            StackFrame frame;
                            try {
                                frame = ThreadInfo.getCurrentThreadInfo().getCurrentFrame();
                            } catch (IncompatibleThreadStateException ee) {
                                MessageOutput.println("Current thread isnt suspended.");
                                return;
                            }
                            Env.setAtExitMethod(frame.location().method());
                            traceCmd = MessageOutput.format("trace" +
                                                    goStr + "method exit " +
                                                    "in effect for",
                                                    Env.atExitMethod().toString());
                        } else {
                            traceCmd = MessageOutput.format("trace" +
                                                   goStr + "method exits " +
                                                   "in effect");
                        }
                        commandUntrace(new StringTokenizer("methods"));
                        turnOnExitTrace(threadInfo, suspendPolicy);
                        methodTraceCommand = traceCmd;
                        return;
                    }
                } else {
                   MessageOutput.println("Can only trace");
                   return;
                }
            }
            if (modif.equals("methods")) {
                // Turn on method entry trace
                MethodEntryRequest entry;
                EventRequestManager erm = Env.vm().eventRequestManager();
                if (t.hasMoreTokens()) {
                    threadInfo = doGetThread(t.nextToken());
                }
                if (threadInfo != null) {
                    /*
                     * To keep things simple we want each 'trace' to cancel
                     * previous traces.  However in this case, we don't do that
                     * to preserve backward compatibility with pre JDK 6.0.
                     * IE, you can currently do
                     *   trace   methods 0x21
                     *   trace   methods 0x22
                     * and you will get xxx traced just on those two threads
                     * But this feature is kind of broken because if you then do
                     *   untrace  0x21
                     * it turns off both traces instead of just the one.
                     * Another bogosity is that if you do
                     *   trace methods
                     *   trace methods
                     * and you will get two traces.
                     */

                    entry = erm.createMethodEntryRequest();
                    entry.addThreadFilter(threadInfo.getThread());
                } else {
                    commandUntrace(new StringTokenizer("methods"));
                    entry = erm.createMethodEntryRequest();
                }
                Env.addExcludes(entry);
                entry.setSuspendPolicy(suspendPolicy);
                entry.enable();
                turnOnExitTrace(threadInfo, suspendPolicy);
                methodTraceCommand = MessageOutput.format("trace" + goStr +
                                                          "methods in effect");

                return;
            }

            MessageOutput.println("Can only trace");
            return;
        }

        // trace all by itself.
        if (methodTraceCommand != null) {
            MessageOutput.printDirectln(methodTraceCommand);
        }

        // More trace lines can be added here.
    }

    void commandUntrace(StringTokenizer t) {
        // untrace
        // untrace methods

        String modif = null;
        EventRequestManager erm = Env.vm().eventRequestManager();
        if (t.hasMoreTokens()) {
            modif = t.nextToken();
        }
        if (modif == null || modif.equals("methods")) {
            erm.deleteEventRequests(erm.methodEntryRequests());
            erm.deleteEventRequests(erm.methodExitRequests());
            Env.setAtExitMethod(null);
            methodTraceCommand = null;
        }
    }

    void commandList(StringTokenizer t) {
        StackFrame frame = null;
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("No thread specified.");
            return;
        }
        try {
            frame = threadInfo.getCurrentFrame();
        } catch (IncompatibleThreadStateException e) {
            MessageOutput.println("Current thread isnt suspended.");
            return;
        }

        if (frame == null) {
            MessageOutput.println("No frames on the current call stack");
            return;
        }

        Location loc = frame.location();
        if (loc.method().isNative()) {
            MessageOutput.println("Current method is native");
            return;
        }

        String sourceFileName = null;
        try {
            sourceFileName = loc.sourceName();

            ReferenceType refType = loc.declaringType();
            int lineno = loc.lineNumber();

            if (t.hasMoreTokens()) {
                String id = t.nextToken();

                // See if token is a line number.
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setParseIntegerOnly(true);
                    Number n = nf.parse(id);
                    lineno = n.intValue();
                } catch (java.text.ParseException jtpe) {
                    // It isn't -- see if it's a method name.
                        List<Method> meths = refType.methodsByName(id);
                        if (meths == null || meths.size() == 0) {
                            MessageOutput.println("is not a valid line number or method name for",
                                                  new Object [] {id, refType.name()});
                            return;
                        } else if (meths.size() > 1) {
                            MessageOutput.println("is an ambiguous method name in",
                                                  new Object [] {id, refType.name()});
                            return;
                        }
                        loc = meths.get(0).location();
                        lineno = loc.lineNumber();
                }
            }
            int startLine = Math.max(lineno - 4, 1);
            int endLine = startLine + 9;
            if (lineno < 0) {
                MessageOutput.println("Line number information not available for");
            } else if (Env.sourceLine(loc, lineno) == null) {
                MessageOutput.println("is an invalid line number for",
                                      new Object [] {Integer.valueOf(lineno),
                                                     refType.name()});
            } else {
                for (int i = startLine; i <= endLine; i++) {
                    String sourceLine = Env.sourceLine(loc, i);
                    if (sourceLine == null) {
                        break;
                    }
                    if (i == lineno) {
                        MessageOutput.println("source line number current line and line",
                                              new Object [] {Integer.valueOf(i),
                                                             sourceLine});
                    } else {
                        MessageOutput.println("source line number and line",
                                              new Object [] {Integer.valueOf(i),
                                                             sourceLine});
                    }
                }
            }
        } catch (AbsentInformationException e) {
            MessageOutput.println("No source information available for:", loc.toString());
        } catch(FileNotFoundException exc) {
            MessageOutput.println("Source file not found:", sourceFileName);
        } catch(IOException exc) {
            MessageOutput.println("I/O exception occurred:", exc.toString());
        }
    }

    void commandLines(StringTokenizer t) { // Undocumented command: useful for testing
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Specify class and method");
        } else {
            String idClass = t.nextToken();
            String idMethod = t.hasMoreTokens() ? t.nextToken() : null;
            try {
                ReferenceType refType = Env.getReferenceTypeFromToken(idClass);
                if (refType != null) {
                    List<Location> lines = null;
                    if (idMethod == null) {
                        lines = refType.allLineLocations();
                    } else {
                        for (Method method : refType.allMethods()) {
                            if (method.name().equals(idMethod)) {
                                lines = method.allLineLocations();
                            }
                        }
                        if (lines == null) {
                            MessageOutput.println("is not a valid method name", idMethod);
                        }
                    }
                    for (Location line : lines) {
                        MessageOutput.printDirectln(line.toString());// Special case: use printDirectln()
                    }
                } else {
                    MessageOutput.println("is not a valid id or class name", idClass);
                }
            } catch (AbsentInformationException e) {
                MessageOutput.println("Line number information not available for", idClass);
            }
        }
    }

    void commandClasspath(StringTokenizer t) {
        if (Env.vm() instanceof PathSearchingVirtualMachine) {
            PathSearchingVirtualMachine vm = (PathSearchingVirtualMachine)Env.vm();
            MessageOutput.println("base directory:", vm.baseDirectory());
            MessageOutput.println("classpath:", vm.classPath().toString());
        } else {
            MessageOutput.println("The VM does not use paths");
        }
    }

    /* Get or set the source file path list. */
    void commandUse(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.printDirectln(Env.getSourcePath());// Special case: use printDirectln()
        } else {
            /*
             * Take the remainder of the command line, minus
             * leading or trailing whitespace.  Embedded
             * whitespace is fine.
             */
            Env.setSourcePath(t.nextToken("").trim());
        }
    }

    /* Print a stack variable */
    private void printVar(LocalVariable var, Value value) {
        MessageOutput.println("expr is value",
                              new Object [] {var.name(),
                                             value == null ? "null" : value.toString()});
    }

    /* Print all local variables in current stack frame. */
    void commandLocals() {
        StackFrame frame;
        ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
        if (threadInfo == null) {
            MessageOutput.println("No default thread specified:");
            return;
        }
        try {
            frame = threadInfo.getCurrentFrame();
            if (frame == null) {
                throw new AbsentInformationException();
            }
            List<LocalVariable> vars = frame.visibleVariables();

            if (vars.size() == 0) {
                MessageOutput.println("No local variables");
                return;
            }
            Map<LocalVariable, Value> values = frame.getValues(vars);

            MessageOutput.println("Method arguments:");
            for (LocalVariable var : vars) {
                if (var.isArgument()) {
                    Value val = values.get(var);
                    printVar(var, val);
                }
            }
            MessageOutput.println("Local variables:");
            for (LocalVariable var : vars) {
                if (!var.isArgument()) {
                    Value val = values.get(var);
                    printVar(var, val);
                }
            }
        } catch (AbsentInformationException aie) {
            MessageOutput.println("Local variable information not available.");
        } catch (IncompatibleThreadStateException exc) {
            MessageOutput.println("Current thread isnt suspended.");
        }
    }

    private void dump(ObjectReference obj, ReferenceType refType,
                      ReferenceType refTypeBase) {
        for (Field field : refType.fields()) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            if (!refType.equals(refTypeBase)) {
                sb.append(refType.name());
                sb.append(".");
            }
            sb.append(field.name());
            sb.append(MessageOutput.format("colon space"));
            sb.append(obj.getValue(field));
            MessageOutput.printDirectln(sb.toString()); // Special case: use printDirectln()
        }
        if (refType instanceof ClassType) {
            ClassType sup = ((ClassType)refType).superclass();
            if (sup != null) {
                dump(obj, sup, refTypeBase);
            }
        } else if (refType instanceof InterfaceType) {
            for (InterfaceType sup : ((InterfaceType)refType).superinterfaces()) {
                dump(obj, sup, refTypeBase);
            }
        } else {
            /* else refType is an instanceof ArrayType */
            if (obj instanceof ArrayReference) {
                for (Iterator<Value> it = ((ArrayReference)obj).getValues().iterator();
                     it.hasNext(); ) {
                    MessageOutput.printDirect(it.next().toString());// Special case: use printDirect()
                    if (it.hasNext()) {
                        MessageOutput.printDirect(", ");// Special case: use printDirect()
                    }
                }
                MessageOutput.println();
            }
        }
    }

    /* Print a specified reference.
     */
    void doPrint(StringTokenizer t, boolean dumpObject) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No objects specified.");
            return;
        }

        while (t.hasMoreTokens()) {
            String expr = t.nextToken("");
            Value val = evaluate(expr);
            if (val == null) {
                MessageOutput.println("expr is null", expr.toString());
            } else if (dumpObject && (val instanceof ObjectReference) &&
                       !(val instanceof StringReference)) {
                ObjectReference obj = (ObjectReference)val;
                ReferenceType refType = obj.referenceType();
                MessageOutput.println("expr is value",
                                      new Object [] {expr.toString(),
                                                     MessageOutput.format("grouping begin character")});
                dump(obj, refType, refType);
                MessageOutput.println("grouping end character");
            } else {
                  String strVal = getStringValue();
                  if (strVal != null) {
                     MessageOutput.println("expr is value", new Object [] {expr.toString(),
                                                                      strVal});
                   }
            }
        }
    }

    void commandPrint(final StringTokenizer t, final boolean dumpObject) {
        new AsyncExecution() {
                @Override
                void action() {
                    doPrint(t, dumpObject);
                }
            };
    }

    void commandSet(final StringTokenizer t) {
        String all = t.nextToken("");

        /*
         * Bare bones error checking.
         */
        if (all.indexOf('=') == -1) {
            MessageOutput.println("Invalid assignment syntax");
            MessageOutput.printPrompt();
            return;
        }

        /*
         * The set command is really just syntactic sugar. Pass it on to the
         * print command.
         */
        commandPrint(new StringTokenizer(all), false);
    }

    void doLock(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No object specified.");
            return;
        }

        String expr = t.nextToken("");
        Value val = evaluate(expr);

        try {
            if ((val != null) && (val instanceof ObjectReference)) {
                ObjectReference object = (ObjectReference)val;
                String strVal = getStringValue();
                if (strVal != null) {
                    MessageOutput.println("Monitor information for expr",
                                      new Object [] {expr.trim(),
                                                     strVal});
                }
                ThreadReference owner = object.owningThread();
                if (owner == null) {
                    MessageOutput.println("Not owned");
                } else {
                    MessageOutput.println("Owned by:",
                                          new Object [] {owner.name(),
                                                         Integer.valueOf(object.entryCount())});
                }
                List<ThreadReference> waiters = object.waitingThreads();
                if (waiters.size() == 0) {
                    MessageOutput.println("No waiters");
                } else {
                    for (ThreadReference waiter : waiters) {
                        MessageOutput.println("Waiting thread:", waiter.name());
                    }
                }
            } else {
                MessageOutput.println("Expression must evaluate to an object");
            }
        } catch (IncompatibleThreadStateException e) {
            MessageOutput.println("Threads must be suspended");
        }
    }

    void commandLock(final StringTokenizer t) {
        new AsyncExecution() {
                @Override
                void action() {
                    doLock(t);
                }
            };
    }

    private void printThreadLockInfo(ThreadInfo threadInfo) {
        ThreadReference thread = threadInfo.getThread();
        try {
            MessageOutput.println("Monitor information for thread", thread.name());
            List<ObjectReference> owned = thread.ownedMonitors();
            if (owned.size() == 0) {
                MessageOutput.println("No monitors owned");
            } else {
                for (ObjectReference monitor : owned) {
                    MessageOutput.println("Owned monitor:", monitor.toString());
                }
            }
            ObjectReference waiting = thread.currentContendedMonitor();
            if (waiting == null) {
                MessageOutput.println("Not waiting for a monitor");
            } else {
                MessageOutput.println("Waiting for monitor:", waiting.toString());
            }
        } catch (IncompatibleThreadStateException e) {
            MessageOutput.println("Threads must be suspended");
        }
    }

    void commandThreadlocks(final StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
            if (threadInfo == null) {
                MessageOutput.println("Current thread not set.");
            } else {
                printThreadLockInfo(threadInfo);
            }
            return;
        }
        String token = t.nextToken();
        if (token.toLowerCase().equals("all")) {
            for (ThreadInfo threadInfo : ThreadInfo.threads()) {
                printThreadLockInfo(threadInfo);
            }
        } else {
            ThreadInfo threadInfo = doGetThread(token);
            if (threadInfo != null) {
                ThreadInfo.setCurrentThreadInfo(threadInfo);
                printThreadLockInfo(threadInfo);
            }
        }
    }

    void doDisableGC(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No object specified.");
            return;
        }

        String expr = t.nextToken("");
        Value val = evaluate(expr);
        if ((val != null) && (val instanceof ObjectReference)) {
            ObjectReference object = (ObjectReference)val;
            object.disableCollection();
            String strVal = getStringValue();
            if (strVal != null) {
                 MessageOutput.println("GC Disabled for", strVal);
            }
        } else {
            MessageOutput.println("Expression must evaluate to an object");
        }
    }

    void commandDisableGC(final StringTokenizer t) {
        new AsyncExecution() {
                @Override
                void action() {
                    doDisableGC(t);
                }
            };
    }

    void doEnableGC(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No object specified.");
            return;
        }

        String expr = t.nextToken("");
        Value val = evaluate(expr);
        if ((val != null) && (val instanceof ObjectReference)) {
            ObjectReference object = (ObjectReference)val;
            object.enableCollection();
            String strVal = getStringValue();
            if (strVal != null) {
                 MessageOutput.println("GC Enabled for", strVal);
            }
        } else {
            MessageOutput.println("Expression must evaluate to an object");
        }
    }

    void commandEnableGC(final StringTokenizer t) {
        new AsyncExecution() {
                @Override
                void action() {
                    doEnableGC(t);
                }
            };
    }

    void doSave(StringTokenizer t) {// Undocumented command: useful for testing.
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No save index specified.");
            return;
        }

        String key = t.nextToken();

        if (!t.hasMoreTokens()) {
            MessageOutput.println("No expression specified.");
            return;
        }
        String expr = t.nextToken("");
        Value val = evaluate(expr);
        if (val != null) {
            Env.setSavedValue(key, val);
            String strVal = getStringValue();
            if (strVal != null) {
                 MessageOutput.println("saved", strVal);
            }
        } else {
            MessageOutput.println("Expression cannot be void");
        }
    }

    void commandSave(final StringTokenizer t) { // Undocumented command: useful for testing.
        if (!t.hasMoreTokens()) {
            Set<String> keys = Env.getSaveKeys();
            if (keys.isEmpty()) {
                MessageOutput.println("No saved values");
                return;
            }
            for (String key : keys) {
                Value value = Env.getSavedValue(key);
                if ((value instanceof ObjectReference) &&
                    ((ObjectReference)value).isCollected()) {
                    MessageOutput.println("expr is value <collected>",
                                          new Object [] {key, value.toString()});
                } else {
                    if (value == null){
                        MessageOutput.println("expr is null", key);
                    } else {
                        MessageOutput.println("expr is value",
                                              new Object [] {key, value.toString()});
                    }
                }
            }
        } else {
            new AsyncExecution() {
                    @Override
                    void action() {
                        doSave(t);
                    }
                };
        }

    }

   void commandBytecodes(final StringTokenizer t) { // Undocumented command: useful for testing.
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No class specified.");
            return;
        }
        String className = t.nextToken();

        if (!t.hasMoreTokens()) {
            MessageOutput.println("No method specified.");
            return;
        }
        // Overloading is not handled here.
        String methodName = t.nextToken();

        List<ReferenceType> classes = Env.vm().classesByName(className);
        // TO DO: handle multiple classes found
        if (classes.size() == 0) {
            if (className.indexOf('.') < 0) {
                MessageOutput.println("not found (try the full name)", className);
            } else {
                MessageOutput.println("not found", className);
            }
            return;
        }

        ReferenceType rt = classes.get(0);
        if (!(rt instanceof ClassType)) {
            MessageOutput.println("not a class", className);
            return;
        }

        byte[] bytecodes = null;
        for (Method method : rt.methodsByName(methodName)) {
            if (!method.isAbstract()) {
                bytecodes = method.bytecodes();
                break;
            }
        }

        StringBuilder line = new StringBuilder(80);
        line.append("0000: ");
        for (int i = 0; i < bytecodes.length; i++) {
            if ((i > 0) && (i % 16 == 0)) {
                MessageOutput.printDirectln(line.toString());// Special case: use printDirectln()
                line.setLength(0);
                line.append(String.valueOf(i));
                line.append(": ");
                int len = line.length();
                for (int j = 0; j < 6 - len; j++) {
                    line.insert(0, '0');
                }
            }
            int val = 0xff & bytecodes[i];
            String str = Integer.toHexString(val);
            if (str.length() == 1) {
                line.append('0');
            }
            line.append(str);
            line.append(' ');
        }
        if (line.length() > 6) {
            MessageOutput.printDirectln(line.toString());// Special case: use printDirectln()
        }
    }

    void commandExclude(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.printDirectln(Env.excludesString());// Special case: use printDirectln()
        } else {
            String rest = t.nextToken("");
            if (rest.equals("none")) {
                rest = "";
            }
            Env.setExcludes(rest);
        }
    }

    void commandRedefine(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("Specify classes to redefine");
        } else {
            String className = t.nextToken();
            List<ReferenceType> classes = Env.vm().classesByName(className);
            if (classes.size() == 0) {
                MessageOutput.println("No class named", className);
                return;
            }
            if (classes.size() > 1) {
                MessageOutput.println("More than one class named", className);
                return;
            }
            Env.setSourcePath(Env.getSourcePath());
            ReferenceType refType = classes.get(0);
            if (!t.hasMoreTokens()) {
                MessageOutput.println("Specify file name for class", className);
                return;
            }
            String fileName = t.nextToken();
            File phyl = new File(fileName);
            byte[] bytes = new byte[(int)phyl.length()];
            try {
                InputStream in = new FileInputStream(phyl);
                in.read(bytes);
                in.close();
            } catch (Exception exc) {
                MessageOutput.println("Error reading file",
                             new Object [] {fileName, exc.toString()});
                return;
            }
            Map<ReferenceType, byte[]> map
                = new HashMap<ReferenceType, byte[]>();
            map.put(refType, bytes);
            try {
                Env.vm().redefineClasses(map);
            } catch (Throwable exc) {
                MessageOutput.println("Error redefining class to file",
                             new Object [] {className,
                                            fileName,
                                            exc});
            }
        }
    }

    void commandPopFrames(StringTokenizer t, boolean reenter) {
        ThreadInfo threadInfo;

        if (t.hasMoreTokens()) {
            String token = t.nextToken();
            threadInfo = doGetThread(token);
            if (threadInfo == null) {
                return;
            }
        } else {
            threadInfo = ThreadInfo.getCurrentThreadInfo();
            if (threadInfo == null) {
                MessageOutput.println("No thread specified.");
                return;
            }
        }

        try {
            StackFrame frame = threadInfo.getCurrentFrame();
            threadInfo.getThread().popFrames(frame);
            threadInfo = ThreadInfo.getCurrentThreadInfo();
            ThreadInfo.setCurrentThreadInfo(threadInfo);
            if (reenter) {
                commandStepi();
            }
        } catch (Throwable exc) {
            MessageOutput.println("Error popping frame", exc.toString());
        }
    }

    void commandExtension(StringTokenizer t) {
        if (!t.hasMoreTokens()) {
            MessageOutput.println("No class specified.");
            return;
        }

        String idClass = t.nextToken();
        ReferenceType cls = Env.getReferenceTypeFromToken(idClass);
        String extension = null;
        if (cls != null) {
            try {
                extension = cls.sourceDebugExtension();
                MessageOutput.println("sourcedebugextension", extension);
            } catch (AbsentInformationException e) {
                MessageOutput.println("No sourcedebugextension specified");
            }
        } else {
            MessageOutput.println("is not a valid id or class name", idClass);
        }
    }

    void commandVersion(String debuggerName,
                        VirtualMachineManager vmm) {
        MessageOutput.println("minus version",
                              new Object [] { debuggerName,
                                              vmm.majorInterfaceVersion(),
                                              vmm.minorInterfaceVersion(),
                                                  System.getProperty("java.version")});
        if (Env.connection() != null) {
            try {
                MessageOutput.printDirectln(Env.vm().description());// Special case: use printDirectln()
            } catch (VMNotConnectedException e) {
                MessageOutput.println("No VM connected");
            }
        }
    }
}
