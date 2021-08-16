/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.jdi.connect.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ThreadStartRequest;
import com.sun.jdi.request.ThreadDeathRequest;

import java.util.*;
import java.util.regex.*;
import java.io.*;

class VMConnection {

    private VirtualMachine vm;
    private Process process = null;
    private int outputCompleteCount = 0;

    private final Connector connector;
    private final Map<String, com.sun.jdi.connect.Connector.Argument> connectorArgs;
    private int traceFlags;

    synchronized void notifyOutputComplete() {
        outputCompleteCount++;
        notifyAll();
    }

    synchronized void waitOutputComplete() {
        // Wait for stderr and stdout
        if (process != null) {
            while (outputCompleteCount < 2) {
                try {wait();} catch (InterruptedException e) {}
            }
        }
    }

    private Connector findConnector(String name) {
        for (Connector connector :
                 Bootstrap.virtualMachineManager().allConnectors()) {
            if (connector.name().equals(name)) {
                return connector;
            }
        }
        return null;
    }

    private Map <String, com.sun.jdi.connect.Connector.Argument>
            parseConnectorArgs(Connector connector, String argString, String extraOptions) {
        Map<String, com.sun.jdi.connect.Connector.Argument> arguments = connector.defaultArguments();

        /*
         * We are parsing strings of the form:
         *    name1=value1,[name2=value2,...]
         * However, the value1...valuen substrings may contain
         * embedded comma(s), so make provision for quoting inside
         * the value substrings. (Bug ID 4285874)
         */
        String regexPattern =
            "(quote=[^,]+,)|" +           // special case for quote=.,
            "(\\w+=)" +                   // name=
            "(((\"[^\"]*\")|" +           //   ( "l , ue"
            "('[^']*')|" +                //     'l , ue'
            "([^,'\"]+))+,)";             //     v a l u e )+ ,
        Pattern p = Pattern.compile(regexPattern);
        Matcher m = p.matcher(argString);
        while (m.find()) {
            int startPosition = m.start();
            int endPosition = m.end();
            if (startPosition > 0) {
                /*
                 * It is an error if parsing skips over any part of argString.
                 */
                throw new IllegalArgumentException
                    (MessageOutput.format("Illegal connector argument",
                                          argString));
            }

            String token = argString.substring(startPosition, endPosition);
            int index = token.indexOf('=');
            String name = token.substring(0, index);
            String value = token.substring(index + 1,
                                           token.length() - 1); // Remove comma delimiter

            /*
             * for values enclosed in quotes (single and/or double quotes)
             * strip off enclosing quote chars
             * needed for quote enclosed delimited substrings
             */
            if (name.equals("options")) {
                StringBuilder sb = new StringBuilder();
                if (extraOptions != null) {
                    sb.append(extraOptions).append(" ");
                    // set extraOptions to null to avoid appending it again
                    extraOptions = null;
                }
                for (String s : splitStringAtNonEnclosedWhiteSpace(value)) {
                    boolean wasEnclosed = false;
                    while (isEnclosed(s, "\"") || isEnclosed(s, "'")) {
                        wasEnclosed = true;
                        s = s.substring(1, s.length() - 1);
                    }
                    if (wasEnclosed && hasWhitespace(s)) {
                        s = "\"" + s + "\"";
                    }
                    sb.append(s);
                    sb.append(" ");
                }
                value = sb.toString();
            }

            Connector.Argument argument = arguments.get(name);
            if (argument == null) {
                throw new IllegalArgumentException
                    (MessageOutput.format("Argument is not defined for connector:",
                                          new Object [] {name, connector.name()}));
            }
            argument.setValue(value);

            argString = argString.substring(endPosition); // Remove what was just parsed...
            m = p.matcher(argString);                     //    and parse again on what is left.
        }
        if ((! argString.equals(",")) && (argString.length() > 0)) {
            /*
             * It is an error if any part of argString is left over,
             * unless it was empty to begin with.
             */
            throw new IllegalArgumentException
                (MessageOutput.format("Illegal connector argument", argString));
        }
        if (extraOptions != null) {
            // there was no "options" specified in argString
            Connector.Argument argument = arguments.get("options");
            if (argument != null) {
                argument.setValue(extraOptions);
            }
        }
        return arguments;
    }

    private static boolean hasWhitespace(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnclosed(String value, String enclosingChar) {
        if (value.indexOf(enclosingChar) == 0) {
            int lastIndex = value.lastIndexOf(enclosingChar);
            if (lastIndex > 0 && lastIndex  == value.length() - 1) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitStringAtNonEnclosedWhiteSpace(String value) throws IllegalArgumentException {
        List<String> al = new ArrayList<String>();
        char[] arr;
        int startPosition = 0;
        int endPosition = 0;
        final char SPACE = ' ';
        final char DOUBLEQ = '"';
        final char SINGLEQ = '\'';

        /*
         * An "open" or "active" enclosing state is where
         * the first valid start quote qualifier is found,
         * and there is a search in progress for the
         * relevant end matching quote
         *
         * enclosingTargetChar set to SPACE
         * is used to signal a non open enclosing state
         */
        char enclosingTargetChar = SPACE;

        if (value == null) {
            throw new IllegalArgumentException
                (MessageOutput.format("value string is null"));
        }

        // split parameter string into individual chars
        arr = value.toCharArray();

        for (int i = 0; i < arr.length; i++) {
            switch (arr[i]) {
                case SPACE: {
                    // do nothing for spaces
                    // unless last in array
                    if (isLastChar(arr, i)) {
                        endPosition = i;
                        // break for substring creation
                        break;
                    }
                    continue;
                }
                case DOUBLEQ:
                case SINGLEQ: {
                    if (enclosingTargetChar == arr[i]) {
                        // potential match to close open enclosing
                        if (isNextCharWhitespace(arr, i)) {
                            // if peek next is whitespace
                            // then enclosing is a valid substring
                            endPosition = i;
                            // reset enclosing target char
                            enclosingTargetChar = SPACE;
                            // break for substring creation
                            break;
                        }
                    }
                    if (enclosingTargetChar == SPACE) {
                        // no open enclosing state
                        // handle as normal char
                        if (isPreviousCharWhitespace(arr, i)) {
                            startPosition = i;
                            // peek forward for end candidates
                            if (value.indexOf(arr[i], i + 1) >= 0) {
                                // set open enclosing state by
                                // setting up the target char
                                enclosingTargetChar = arr[i];
                            } else {
                                // no more target chars left to match
                                // end enclosing, handle as normal char
                                if (isNextCharWhitespace(arr, i)) {
                                    endPosition = i;
                                    // break for substring creation
                                    break;
                                }
                            }
                        }
                    }
                    continue;
                }
                default: {
                    // normal non-space, non-" and non-' chars
                    if (enclosingTargetChar == SPACE) {
                        // no open enclosing state
                        if (isPreviousCharWhitespace(arr, i)) {
                            // start of space delim substring
                            startPosition = i;
                        }
                        if (isNextCharWhitespace(arr, i)) {
                            // end of space delim substring
                            endPosition = i;
                            // break for substring creation
                            break;
                        }
                    }
                    continue;
                }
            }

            // break's end up here
            if (startPosition > endPosition) {
                throw new IllegalArgumentException
                    (MessageOutput.format("Illegal option values"));
            }

            // extract substring and add to List<String>
            al.add(value.substring(startPosition, ++endPosition));

            // set new start position
            i = startPosition = endPosition;

        } // for loop

        return al;
    }

    static private boolean isPreviousCharWhitespace(char[] arr, int curr_pos) {
        return isCharWhitespace(arr, curr_pos - 1);
    }

    static private boolean isNextCharWhitespace(char[] arr, int curr_pos) {
        return isCharWhitespace(arr, curr_pos + 1);
    }

    static private boolean isCharWhitespace(char[] arr, int pos) {
        if (pos < 0 || pos >= arr.length) {
            // outside arraybounds is considered an implicit space
            return true;
        }
        if (arr[pos] == ' ') {
            return true;
        }
        return false;
    }

    static private boolean isLastChar(char[] arr, int pos) {
        return (pos + 1 == arr.length);
    }

    VMConnection(String connectSpec, int traceFlags, String extraOptions) {
        String nameString;
        String argString;
        int index = connectSpec.indexOf(':');
        if (index == -1) {
            nameString = connectSpec;
            argString = "";
        } else {
            nameString = connectSpec.substring(0, index);
            argString = connectSpec.substring(index + 1);
        }

        connector = findConnector(nameString);
        if (connector == null) {
            throw new IllegalArgumentException
                (MessageOutput.format("No connector named:", nameString));
        }

        connectorArgs = parseConnectorArgs(connector, argString, extraOptions);
        this.traceFlags = traceFlags;
    }

    public void setTraceFlags(int flags) {
        this.traceFlags = flags;
        /*
         * If vm is not connected now, then vm.setDebugTraceMode() will
         * be called when it is connected.
         */
        if (vm != null) {
            vm.setDebugTraceMode(flags);
        }
    }

    synchronized VirtualMachine open() {
        if (connector instanceof LaunchingConnector) {
            vm = launchTarget();
        } else if (connector instanceof AttachingConnector) {
            vm = attachTarget();
        } else if (connector instanceof ListeningConnector) {
            vm = listenTarget();
        } else {
            throw new InternalError
                (MessageOutput.format("Invalid connect type"));
        }
        vm.setDebugTraceMode(traceFlags);
        if (vm.canBeModified()){
            setEventRequests(vm);
            resolveEventRequests();
        }
        /*
         * Now that the vm connection is open, fetch the debugee
         * classpath and set up a default sourcepath.
         * (Unless user supplied a sourcepath on the command line)
         * (Bug ID 4186582)
         */
        if (Env.getSourcePath().length() == 0) {
            if (vm instanceof PathSearchingVirtualMachine) {
                PathSearchingVirtualMachine psvm =
                    (PathSearchingVirtualMachine) vm;
                Env.setSourcePath(psvm.classPath());
            } else {
                Env.setSourcePath(".");
            }
        }

        return vm;
    }

    boolean setConnectorArg(String name, String value) {
        /*
         * Too late if the connection already made
         */
        if (vm != null) {
            return false;
        }

        Connector.Argument argument = connectorArgs.get(name);
        if (argument == null) {
            return false;
        }
        argument.setValue(value);
        return true;
    }

    String connectorArg(String name) {
        Connector.Argument argument = connectorArgs.get(name);
        if (argument == null) {
            return "";
        }
        return argument.value();
    }

    public synchronized VirtualMachine vm() {
        if (vm == null) {
            throw new VMNotConnectedException();
        } else {
            return vm;
        }
    }

    boolean isOpen() {
        return (vm != null);
    }

    boolean isLaunch() {
        return (connector instanceof LaunchingConnector);
    }

    public void disposeVM() {
        try {
            if (vm != null) {
                vm.dispose();
                vm = null;
            }
        } finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
            waitOutputComplete();
        }
    }

    private void setEventRequests(VirtualMachine vm) {
        EventRequestManager erm = vm.eventRequestManager();

        // Normally, we want all uncaught exceptions.  We request them
        // via the same mechanism as Commands.commandCatchException()
        // so the user can ignore them later if they are not
        // interested.
        // FIXME: this works but generates spurious messages on stdout
        //        during startup:
        //          Set uncaught java.lang.Throwable
        //          Set deferred uncaught java.lang.Throwable
        Commands evaluator = new Commands();
        evaluator.commandCatchException
            (new StringTokenizer("uncaught java.lang.Throwable"));

        ThreadStartRequest tsr = erm.createThreadStartRequest();
        tsr.enable();
        ThreadDeathRequest tdr = erm.createThreadDeathRequest();
        tdr.enable();
    }

    private void resolveEventRequests() {
        Env.specList.resolveAll();
    }

    private void dumpStream(InputStream stream) throws IOException {
        BufferedReader in =
            new BufferedReader(new InputStreamReader(stream));
        int i;
        try {
            while ((i = in.read()) != -1) {
                   MessageOutput.printDirect((char)i);// Special case: use
                                                      //   printDirect()
            }
        } catch (IOException ex) {
            if (!ex.getMessage().equalsIgnoreCase("stream closed")) {
                  throw ex;
            }
            // else we got a "Stream closed" IOException which just means
            // that the debuggee has gone away.  We'll just treat it the
            // same as if we got an EOF.
        }
    }

    /**
     *  Create a Thread that will retrieve and display any output.
     *  Needs to be high priority, else debugger may exit before
     *  it can be displayed.
     */
    private void displayRemoteOutput(final InputStream stream) {
        Thread thr = new Thread("output reader") {
            @Override
            public void run() {
                try {
                    dumpStream(stream);
                } catch (IOException ex) {
                    MessageOutput.fatalError("Failed reading output");
                } finally {
                    notifyOutputComplete();
                }
            }
        };
        thr.setPriority(Thread.MAX_PRIORITY-1);
        thr.start();
    }

    private void dumpFailedLaunchInfo(Process process) {
        try {
            dumpStream(process.getErrorStream());
            dumpStream(process.getInputStream());
        } catch (IOException e) {
            MessageOutput.println("Unable to display process output:",
                                  e.getMessage());
        }
    }

    /* launch child target vm */
    private VirtualMachine launchTarget() {
        LaunchingConnector launcher = (LaunchingConnector)connector;
        try {
            VirtualMachine vm = launcher.launch(connectorArgs);
            process = vm.process();
            displayRemoteOutput(process.getErrorStream());
            displayRemoteOutput(process.getInputStream());
            return vm;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            MessageOutput.fatalError("Unable to launch target VM.");
        } catch (IllegalConnectorArgumentsException icae) {
            icae.printStackTrace();
            MessageOutput.fatalError("Internal debugger error.");
        } catch (VMStartException vmse) {
            MessageOutput.println("vmstartexception", vmse.getMessage());
            MessageOutput.println();
            dumpFailedLaunchInfo(vmse.process());
            MessageOutput.fatalError("Target VM failed to initialize.");
        }
        return null; // Shuts up the compiler
    }

    /* attach to running target vm */
    private VirtualMachine attachTarget() {
        AttachingConnector attacher = (AttachingConnector)connector;
        try {
            return attacher.attach(connectorArgs);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            MessageOutput.fatalError("Unable to attach to target VM.");
        } catch (IllegalConnectorArgumentsException icae) {
            icae.printStackTrace();
            MessageOutput.fatalError("Internal debugger error.");
        }
        return null; // Shuts up the compiler
    }

    /* listen for connection from target vm */
    private VirtualMachine listenTarget() {
        ListeningConnector listener = (ListeningConnector)connector;
        try {
            String retAddress = listener.startListening(connectorArgs);
            MessageOutput.println("Listening at address:", retAddress);
            vm = listener.accept(connectorArgs);
            listener.stopListening(connectorArgs);
            return vm;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            MessageOutput.fatalError("Unable to attach to target VM.");
        } catch (IllegalConnectorArgumentsException icae) {
            icae.printStackTrace();
            MessageOutput.fatalError("Internal debugger error.");
        }
        return null; // Shuts up the compiler
    }
}
