/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdb;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.ArgumentHandler;

import java.io.*;
import java.util.*;

/**
 * This class provides launching of <code>jdb</code> and debuggee in local
 * or remote mode according to test command line options.
 */

public class Launcher extends DebugeeBinder {

    /* Delay in milliseconds after launching jdb.*/
    static final long DEBUGGEE_START_DELAY = 5 * 1000;

    protected static Jdb jdb;

    protected static Debuggee debuggee;

    /** Pattern for message of jdb has started. */
    protected static String JDB_STARTED = "Initializing jdb";

    /**
     * Get version string.
     */
    public static String getVersion () {
        return "@(#)Launcher.java %I% %E%";
    }

    // -------------------------------------------------- //

    /**
     * Handler of command line arguments.
     */
    protected static JdbArgumentHandler argumentHandler = null;

    /**
     * Return <code>argumentHandler</code> of this binder.
     */
    public static JdbArgumentHandler getJdbArgumentHandler() {
        return argumentHandler;
    }

    /**
     * Return <code>jdb</code> mirror of this binder.
     */
    public static Jdb getJdb() {
        return jdb;
    }

    /**
     * Return debuggee mirror of this binder.
     */
    public static Debuggee getDebuggee() {
        return debuggee;
    }

    /**
     * Incarnate new Launcher obeying the given
     * <code>argumentHandler</code>; and assign the given
     * <code>log</code>.
     */
    public Launcher (JdbArgumentHandler argumentHandler, Log log) {
        super(argumentHandler, log);
        setLogPrefix("launcher > ");
        this.argumentHandler = argumentHandler;
    }

    /**
     * Defines mode (local or remote) and type of connector (default, launching,
     * raw launching, attaching or listening) according to options
     * parsed by <code>JdbArgumentHandler</code>. And then launches <code>jdb</code>
     * and debuggee in defined mode.
     */
    public void launchJdbAndDebuggee (String classToExecute) throws IOException {

        String[] jdbCmdArgs = makeJdbCmdLine(classToExecute);

        if (argumentHandler.isLaunchedLocally()) {

            if (argumentHandler.isDefaultConnector()) {

                localDefaultLaunch(jdbCmdArgs, classToExecute);

            } else if (argumentHandler.isRawLaunchingConnector()) {

                localRawLaunch(jdbCmdArgs, classToExecute);

            } else if (argumentHandler.isLaunchingConnector()) {

                localLaunch(jdbCmdArgs, classToExecute);

            } else if (argumentHandler.isAttachingConnector()) {

                localLaunchAndAttach(jdbCmdArgs, classToExecute);

            } else if (argumentHandler.isListeningConnector()) {

                localLaunchAndListen(jdbCmdArgs, classToExecute);

            } else {
                throw new TestBug("Unexpected connector type for local launch mode"
                                  + argumentHandler.getConnectorType());
            }

        } else if (argumentHandler.isLaunchedRemotely()) {

            connectToBindServer(classToExecute);

            if (argumentHandler.isAttachingConnector()) {

                remoteLaunchAndAttach(jdbCmdArgs, classToExecute);

            } else if (argumentHandler.isListeningConnector()) {

                remoteLaunchAndListen(jdbCmdArgs, classToExecute);

            } else {
                throw new TestBug("Unexpected connector type for remote launch mode"
                                  + argumentHandler.getConnectorType());
            }
        } else {
            throw new Failure("Unexpected launching mode: " + argumentHandler.getLaunchMode());
        }
    }

    /**
     * Creates String array to launch <code>jdb</code> according to options
     * parsed by <code>JdbArgumentHandler</code>.
     */
    private String[] makeJdbCmdLine (String classToExecute) {

        Vector<String> args = new Vector<String>();

        String jdbExecPath = argumentHandler.getJdbExecPath();
        args.add(jdbExecPath.trim());
        args.addAll(argumentHandler.enwrapJavaOptions(argumentHandler.getJavaOptions()));

        String jdbOptions = argumentHandler.getJdbOptions();
        if (jdbOptions.trim().length() > 0) {
            StringTokenizer tokenizer = new StringTokenizer(jdbOptions);
            while (tokenizer.hasMoreTokens()) {
                String option = tokenizer.nextToken();
                args.add(option);
            }
        }
        if (classToExecute == null)
            return args.toArray(new String[args.size()]);
        args.add("-connect");
        StringBuffer connect = new StringBuffer();

        if (argumentHandler.isLaunchingConnector()) {

// Do not need to use quote symbol.
//            String quote = '\"';
//            connect.append(quote + argumentHandler.getConnectorName() + ":");
            connect.append(argumentHandler.getConnectorName() + ":");

            String connectorAddress;
            String vmAddress = makeTransportAddress();;

            if (argumentHandler.isRawLaunchingConnector()) {

                if (argumentHandler.isSocketTransport()) {
                    if (argumentHandler.isLaunchedLocally()) {
                        connectorAddress = argumentHandler.getTransportPort();
                    } else {
                        connectorAddress = argumentHandler.getDebugeeHost() + ":" + argumentHandler.getTransportPort();
                    }
                } else if (argumentHandler.isShmemTransport() ) {
                    connectorAddress = argumentHandler.getTransportSharedName();
                } else {
                    throw new TestBug("Launcher: Undefined transport type for RawLaunchingConnector");
                }

                connect.append("address=" + connectorAddress.trim());
                connect.append(",command=" + makeCommandLineString(classToExecute, vmAddress, " ").trim());

            } else /* LaunchingConnector or DefaultConnector */ {

                connect.append("vmexec=" + argumentHandler.getLaunchExecName().trim());
                String debuggeeOpts = argumentHandler.getDebuggeeOptions();
                if (debuggeeOpts.trim().length() > 0) {
                    //connect.append(",options=" + debuggeeOpts.trim());
                    connect.append(",options=");
                    for (String arg : debuggeeOpts.split("\\s+")) {
                       connect.append(" \"");
                       connect.append(arg);
                       connect.append("\"");
                    }
                }
                String cmdline = classToExecute + " " + ArgumentHandler.joinArguments(argumentHandler.getArguments(), " ");
                connect.append(",main=" + cmdline.trim());

            }

//            connect.append(quote);

        } else {

            connect.append(argumentHandler.getConnectorName() + ":");

            if (argumentHandler.isAttachingConnector()) {

                if (argumentHandler.isSocketTransport()) {
                    connect.append("port=" + argumentHandler.getTransportPort().trim());
                    if (argumentHandler.isLaunchedRemotely())
                        connect.append(",hostname=" + argumentHandler.getDebugeeHost().trim());
                } else if (argumentHandler.isShmemTransport()) {
                    connect.append("name=" + argumentHandler.getTransportSharedName().trim());
                } else {
                    throw new TestBug("Launcher: Undefined transport type for AttachingConnector");
                }


            } else if (argumentHandler.isListeningConnector()) {

                if (!argumentHandler.isTransportAddressDynamic()) {
                    if (argumentHandler.isSocketTransport()) {
                        connect.append("port=" + argumentHandler.getTransportPort().trim());
                    } else if (argumentHandler.isShmemTransport()) {
                        connect.append("name=" + argumentHandler.getTransportSharedName().trim());
                    } else {
                        throw new TestBug("Launcher: Undefined transport type for AttachingConnector");
                    }
                }

            } else {
                throw new TestBug("Launcher: Undefined connector type");
            }

        }

        args.add(connect.toString().trim());

        String[] argsArray = new String[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argsArray[i] = (String) args.elementAt(i);
        }

        return argsArray;
    }

    // ---------------------------------------------- //

    /**
     * Run test in local mode using default connector.
     */
    private void localDefaultLaunch
       (String[] jdbCmdArgs, String classToExecute) throws IOException {
        localLaunch(jdbCmdArgs, classToExecute);
    }

    /**
     * Run test in local mode using raw launching connector.
     */
    private void localRawLaunch
       (String[] jdbCmdArgs, String classToExecute) throws IOException {
        localLaunch(jdbCmdArgs, classToExecute);
    }

    /**
     * Run test in local mode using launching connector.
     */
    private void localLaunch
       (String[] jdbCmdArgs, String classToExecute) throws IOException {

        jdb = new Jdb(this);
        display("Starting jdb launching local debuggee");
        jdb.launch(jdbCmdArgs);

        if (classToExecute != null)
            jdb.waitForMessage(0, JDB_STARTED);
//        jdb.waitForPrompt(0, false);

    }

    /**
     * Run test in local mode using attaching connector.
     */
    private void localLaunchAndAttach
       (String[] jdbCmdArgs, String classToExecute) throws IOException {

        debuggee = new LocalLaunchedDebuggee(this);
        String address = makeTransportAddress();
        String[] javaCmdArgs = makeCommandLineArgs(classToExecute, address);
        debuggee.launch(javaCmdArgs);

        display("Start jdb attaching to local debuggee");
        jdb = Jdb.startAttachingJdb (this, jdbCmdArgs, JDB_STARTED);
//        jdb.waitForPrompt(0, false);
    }

    /**
     * Run test in local mode using listening connector.
     */
    private void localLaunchAndListen
       (String[] jdbCmdArgs, String classToExecute) throws IOException {

        jdb = new Jdb(this);
        display("Starting jdb listening to local debuggee");
        jdb.launch(jdbCmdArgs);
        String address = jdb.waitForListeningJdb();
        display("Listening address found: " + address);

        debuggee = new LocalLaunchedDebuggee(this);
        String[] javaCmdArgs = makeCommandLineArgs(classToExecute, address);
        debuggee.launch(javaCmdArgs);

//        jdb.waitForPrompt(0, false);
    }

    /**
     * Run test in remote mode using attaching connector.
     */
    private void remoteLaunchAndAttach
       (String[] jdbCmdArgs, String classToExecute) throws IOException {

        debuggee = new RemoteLaunchedDebuggee(this);
        String address = makeTransportAddress();
        String[] javaCmdArgs = makeCommandLineArgs(classToExecute, address);
        try {
            debuggee.launch(javaCmdArgs);
        } catch (IOException e) {
            throw new Failure("Caught exception while launching debuggee VM process:\n\t"
                            + e);
        };

        display("Start jdb attaching to remote debuggee");
        jdb = Jdb.startAttachingJdb (this, jdbCmdArgs, JDB_STARTED);
//        jdb.waitForPrompt(0, false);
    }

    /**
     * Run test in remote mode using listening connector.
     */
    private void remoteLaunchAndListen
       (String[] jdbCmdArgs, String classToExecute) throws IOException {

        jdb = new Jdb(this);
        display("Starting jdb listening to remote debuggee");
        jdb.launch(jdbCmdArgs);
        String address = jdb.waitForListeningJdb();
        display("Listening address found: " + address);

        debuggee = new RemoteLaunchedDebuggee(this);
        String[] javaCmdArgs = makeCommandLineArgs(classToExecute);
        try {
            debuggee.launch(javaCmdArgs);
        } catch (IOException e) {
            throw new Failure("Caught exception while launching debuggee VM process:\n\t"
                            + e);
        };

        jdb.waitForMessage(0, JDB_STARTED);
//        jdb.waitForPrompt(0, false);
    }

} // End of Launcher
