/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jpda;

import nsk.share.*;

import java.io.*;
import java.net.ServerSocket;

/**
 * Parser for JPDA test's launching and connection arguments.
 * <p>
 * <Code>DebugeeArgumentHandler</code> handles specific JDI/JDWP/JDB tests
 * command line arguments related to launching and connection parameters
 * for debugee VM in addition to general arguments recognized by
 * <code>ArgumentParser</code>.
 * <p>
 * Following is the list of specific options for
 * <code>DebugeeAgrumentHandler</code>:
 * <ul>
 * <li> <code>-test.host=</code>&lt;<i>host</i>&gt; -
 *   address of a host where test executes
 * <li> <code>-debugee.host=</code>&lt;<i>host</i>&gt; -
 *   address of a host where debugee VM executes
 * <li> <code>-connector=[attaching|listening]</code> -
 *   connector type to connect to debugee VM
 * <li> <code>-transport=[socket|shmem]</code> -
 *   transport type to connect to debugee VM
 * <li> <code>-transport.port=</code>&lt;<i>port</i>&gt; -
 *   port number for <code>socket</code> transport
 * <li> <code>-transport.shname=</code>&lt;<i>name</i>&gt; -
 *   shared memory name for <code>shmem</code> transport
 * <li> <code>-transport.address=</code>&lt;<i>dynamic</i>&gt; -
 *   use dynamically allocated unique transport address for JDWP connection
 *   ignoring settings for <code>-transport.port</code> and <code>-transport.shname</code>
 *   (this works only with <code>-connector=listening</code> and <code>-transport=socket</code>)
 * <li> <code>-debugee.suspend=[yes|no|default]</code> -
 *   should debugee start in suspend mode or not
 * <li> <code>-debugee.launch=[local|remote|manual]</code> -
 *   launch and bind to debugee VM locally, remotely (via BindSever) or manually
 * <li> <code>-debugee.vmhome=</code>&lt;<i>path</i>&gt; -
 *   path to JDK used for launching debugee VM
 * <li> <code>-debugee.vmkind=</code>&lt;<i>name</i>&gt; -
 *   name of debugee VM launcher executable
 * <li> <code>-debugee.vmkeys=</code>&lt;<i>string</i>&gt; -
 *   additional options for launching debugee VM
 * <li> <code>-jvmdi.strict=[yes|no|default]</code> -
 *   using JVMDI strict mode
 * <li> <code>-pipe.port=</code>&lt;<i>port</i>&gt; -
 *   port number for internal IOPipe connection
 * <li> <code>-bind.port=</code>&lt;<i>port</i>&gt; -
 *   port number for BindServer connection
 * </ul>
 * <p>
 * See also list of basic options recognized by
 * <code>ArgumentParser</code>.
 * <p>
 * See also comments to <code>ArgumentParser</code> for list of general
 * recognized options and how to work with command line arguments and options.
 *
 * @see ArgumentParser
 * @see nsk.share.jdi.ArgumentHandler
 * @see nsk.share.jdwp.ArgumentHandler
 */
public class DebugeeArgumentHandler extends ArgumentParser {

    public static final String DEFAULT_PIPE_PORT                                = "7123";
    public static final String DEFAULT_TRANSPORT_PORT                   = "8123";
    public static final String DEFAULT_BIND_PORT                                = "9123";


    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param  args  Array of the raw command-line arguments.
     *
     * @throws  BadOption  If unknown option or illegal
     *                     option value found
     *
     * @see #setRawArguments(String[])
     */
    public DebugeeArgumentHandler(String args[]) {
        super(args);
    }

    /**
     * Return name of the host where test executes, specified by
     * <code>-test.host</code> command line option or
     * "<i>localhost</i>" string by default.
     *
     * @see #setRawArguments(String[])
     */
    public String getTestHost() {
        return options.getProperty("test.host", "localhost");
    }

    /**
     * Return name of host where the debugee VM is executed, specified by
     * <code>-debugee.host</code> command line option or value of
     * getTestHost() by default.
     *
     * @see #getTestHost()
     * @see #setRawArguments(String[])
     */
    public String getDebugeeHost() {
        return options.getProperty("debugee.host", getTestHost());
    }

    private boolean transportPortInited = false;
    /**
     * Return string representation of port number for socket transport,
     * specified by <code>-tranport.port</code> command line option or
     * "<code>DEFAULT_TRANSPORT_PORT</code>" string by default.
     *
     * @see #getTransportPortIfNotDynamic()
     * @see #getTransportPortNumber()
     * @see #setTransportPortNumber(int)
     * @see #setRawArguments(String[])
     */
    synchronized public String getTransportPort() {
        String port = options.getProperty("transport.port");
        if (port == null) {
            if (!transportPortInited) {
                port = findFreePort();
                if (port == null) {
                    port = DEFAULT_TRANSPORT_PORT;
                }
                options.setProperty("transport.port", port);
                transportPortInited = true;
            }
        }
        return port;
    }

    /**
     * Return string representation of port number for socket transport,
     * specified by <code>-tranport.port</code> command line option or
     * "<code>DEFAULT_TRANSPORT_PORT</code>" string by default in case transport address is
     * not dynamic.
     * Otherwise null is returned.
     *
     * @see #getTransportPort()
     * @see #getTransportPortNumber()
     * @see #setTransportPortNumber(int)
     * @see #setRawArguments(String[])
     */
    public String getTransportPortIfNotDynamic() {
        return ( isTransportAddressDynamic() ?
                    null : getTransportPort() );
    }

    /**
     * Return string port number for socket transport,
     * specified by <code>-debugee.port</code> command line option or
     * <code>DEFAULT_TRANSPORT_PORT</code> port number by default.
     *
     * @see #getTransportPort()
     * @see #getTransportPortIfNotDynamic()
     * @see #setTransportPortNumber(int)
     * @see #setRawArguments(String[])
     */
    public int getTransportPortNumber() {
        String value = getTransportPort();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"-transport.port\" argument: " + value);
        }
    }

    /**
     * Add or replace value of option <code>-transport.port</code> in options list
     * with the specified port number.
     *
     * @see #getTransportPortNumber()
     * @see #setRawArguments(String[])
     */
    public void setTransportPortNumber(int port) {
        String value = Integer.toString(port);
        setOption("-", "transport.port", value);
    }

    /**
     * Return shared name for shmem transport, specified by
     * <code>-transport.shname</code> command line option, or
     * "<i>nskjpdatestchannel</i>" + a process unique string by default.
     *
     * @see #setTransportSharedName(String)
     * @see #setRawArguments(String[])
     */
    // Use a unique id for this process by default. This makes sure that
    // tests running concurrently do not use the same shared name.
    private static String defaultTransportSharedName
            = "nskjpdatestchannel" + ProcessHandle.current().pid();
    public String getTransportSharedName() {
        return options.getProperty("transport.shname", defaultTransportSharedName);
    }

    /**
     * Add or replace value of option <code>-transport.shname</code> in options list
     * with the specified name.
     *
     * @see #getTransportSharedName()
     * @see #setRawArguments(String[])
     */
    public void setTransportSharedName(String name) {
        setOption("-", "transport.shname", name);
    }

    /**
     * Return <i>true</i> if <code>-transport.address=dynamic</code> command line option
     * is specified.
     *
     * @see #setRawArguments(String[])
     */
    public boolean isTransportAddressDynamic() {
        String value = options.getProperty("transport.address", null);
        if (value != null && value.equals("dynamic"))
            return true;
        return false;
    }

    /**
     * Return suspend mode for launching debugee VM, specified by
     * <code>-debugee.suspend</code> command line option, or
     * "<i>default</i>" string by default.
     *
     * @see #isDefaultDebugeeSuspendMode()
     * @see #willDebugeeSuspended()
     * @see #setRawArguments(String[])
     */
    public String getDebugeeSuspendMode() {
        return options.getProperty("debugee.suspend", "default");
    }

    /**
     * Return <i>true</i> if default suspend mode is used
     * for launching debugee VM.
     *
     * @see #getDebugeeSuspendMode()
     * @see #willDebugeeSuspended()
     */
    public boolean isDefaultDebugeeSuspendMode() {
        String mode = getDebugeeSuspendMode();
        return mode.equals("default");
    }

    /**
     * Return <i>true</i> if debugee VM will be suspended after launching,
     * either according to specified suspend mode or by default.
     *
     * @see #getDebugeeSuspendMode()
     * @see #isDefaultDebugeeSuspendMode()
     */
    public boolean willDebugeeSuspended() {
        if (isLaunchedLocally()) {
            String mode = getDebugeeSuspendMode();
            return mode.equals("no");
        }
        return true;
    }

    private boolean pipePortInited = false;
    /**
     * Return string representation of the port number for IOPipe connection,
     * specified by <code>-pipe.port</code> command line option, or
     * "<i>DEFAULT_PIPE_PORT</i>" string by default.
     *
     * @see #getPipePortNumber()
     * @see #setPipePortNumber(int)
     * @see #setRawArguments(String[])
     */
    synchronized public String getPipePort() {
        String port = options.getProperty("pipe.port");
        if (port == null) {
            if (!pipePortInited) {
                port = findFreePort();
                if (port == null) {
                    port = DEFAULT_PIPE_PORT;
                }
                pipePortInited = true;
                options.setProperty("pipe.port", port);
            }
        }
        return port;
    }

    /**
     * Return port number for IOPipe connection,
     * specified by <code>-pipe.port</code> command line option, or
     * <i>DEFAULT_PIPE_PORT</i> port number by default.
     *
     * @see #getPipePort()
     * @see #setPipePortNumber(int)
     * @see #setRawArguments(String[])
     */
    public int getPipePortNumber() {
        String value = getPipePort();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"-pipe.port\" argument: " + value);
        }
    }

    /**
     * Add or replace value of option <code>-pipe.port</code> in options list
     * with the specified port number.
     *
     * @see #getPipePortNumber()
     * @see #setRawArguments(String[])
     */
    public void setPipePortNumber(int port) {
        String value = Integer.toString(port);
        setOption("-", "pipe.port", value);
    }

    /**
     * Return debugee VM launching mode, specified by
     * <code>-launch.mode</code> command line option, or
     * "<i>local</i>" string by default.
     *
     * Possible values for this option are:
     * <ul>
     * <li> "<code>local</code>"
     * <li> "<code>remote</code>"
     * <li> "<code>manual</code>"
     * </ul>
     *
     * @see #isLaunchedLocally()
     * @see #isLaunchedRemotely()
     * @see #isLaunchedManually()
     * @see #setRawArguments(String[])
     */
    public String getLaunchMode() {
        return options.getProperty("debugee.launch", "local");
    }

    /**
     * Return <i>true</i> if debugee should be launched locally.
     *
     * @see #getLaunchMode()
     */
    public boolean isLaunchedLocally() {
        return getLaunchMode().equals("local");
    }

    /**
     * Return <i>true</i> if debugee should be launched remotely via
     * BindServer.
     *
     * @see #getLaunchMode()
     */
    public boolean isLaunchedRemotely() {
        return getLaunchMode().equals("remote");
    }

    /**
     * Return <i>true</i> if debugee should be launched manually by user.
     *
     * @see #getLaunchMode()
     */
    public boolean isLaunchedManually() {
        return getLaunchMode().equals("manual");
    }

    /**
     * Return additional options for launching debugee VM, specified by
     * <code>-launch.options</code> command line option, or
     * empty string by default.
     *
     * @see #setRawArguments(String[])
     */
    public String getLaunchOptions() {
        String result = options.getProperty("debugee.vmkeys", "").trim();
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    /**
     * Return name of debugee VM launcher executable, specified by
     * <code>-launch.vmexec</code> command line option, or
     * "<i>java</i>" string by default.
     *
     * @see #setRawArguments(String[])
     */
    public String getLaunchExecName() {
        return options.getProperty("debugee.vmkind", "java");
    }

    /**
     * Return full path to debugee VM launcher executable.
     *
     * @see #getLaunchExecName()
     * @see #getLaunchExecPath(String)
     * @see #getDebugeeJavaHome()
     */
    public String getLaunchExecPath() {
        String java_home = getDebugeeJavaHome();
        return getLaunchExecPath(java_home);
    }

    /**
     * Return full path to VM launcher executable using givet JAVA_HOME path.
     *
     * @see #getLaunchExecName()
     */
    public String getLaunchExecPath(String java_home) {
        String filesep = System.getProperty("file.separator");
        return java_home + filesep + "bin" + filesep + getLaunchExecName();
    }

    /**
     * Return full JAVA_HOME path for debugee VM.
     *
     * @see #getLaunchExecName()
     */
    public String getDebugeeJavaHome() {
        String java_home = System.getProperty("java.home");
        return options.getProperty("debugee.vmhome", java_home);
    }

    /**
     * Return true if default debuggee VM launcher executable is used.
     *
     * @see #getLaunchExecName()
     */
    public boolean isDefaultLaunchExecName() {
        String vmkind = options.getProperty("debugee.vmkind", null);
        return (vmkind == null);
    }

    /**
     * Return true if default JAVA_HOME path for debuggee VM is used.
     *
     * @see #getDebugeeJavaHome()
     */
    public boolean isDefaultDebugeeJavaHome() {
        String java_home = options.getProperty("debugee.vmhome", null);
        return (java_home == null);
    }

    private boolean bindPortInited = false;
    /**
     * Return string representation of the port number for BindServer connection,
     * specified by <code>-bind.port</code> command line option, or
     * "<i>DEFAULT_BIND_PORT</i>" string by default.
     *
     * @see #getBindPortNumber()
     * @see #setRawArguments(String[])
     */
    public String getBindPort() {
        String port = options.getProperty("bind.port");
        if (port == null) {
            if (!bindPortInited) {
                port = findFreePort();
                if (port == null) {
                    port = DEFAULT_BIND_PORT;
                }
                options.setProperty("bind.port", port);
                bindPortInited = true;
            }
        }
        return port;
    }

    /**
     * Return port number for BindServer connection,
     * specified by <code>-bind.port</code> command line option, or
     * "<i>DEFAULT_BIND_PORT</i>" port number by default.
     *
     * @see #getBindPort()
     * @see #setRawArguments(String[])
     */
    public int getBindPortNumber() {
        String value = getBindPort();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"bind.port\" argument: " + value);
        }
    }

    /**
     * Return JVMDI strict mode for launching debugee VM, specified by.
     * <code>-jvmdi.strict</code> command line option, or
     * "<i>default</i>" string by default.
     *
     * Possible values for this option are:
     * <ul>
     * <li> "<code>yes</code>"
     * <li> "<code>no</code>"
     * <li> "<code>default</code>"
     * </ul>
     *
     * @see #setRawArguments(String[])
     */
    public String getJVMDIStrictMode() {
        return options.getProperty("jvmdi.strict", "default");
    }

    /**
     * Return <i>true</i> if JVMDI strict mode for launching debugeeVM is used^
     * either by specifying in command line or by default.
     *
     * @see #getJVMDIStrictMode()
     * @see #isDefaultJVMDIStrictMode()
     * @see #setRawArguments(String[])
     */
    public boolean isJVMDIStrictMode() {
        String mode = getJVMDIStrictMode();
        return mode.equals("yes");
    }

    /**
     * Return <i>true</i> if JVMDI default strict mode for launching debugee VM is used.
     *
     * @see #getJVMDIStrictMode()
     * @see #isJVMDIStrictMode()
     * @see #setRawArguments(String[])
     */
    public boolean isDefaultJVMDIStrictMode() {
        String mode = getJVMDIStrictMode();
        return mode.equals("default");
    }

    /**
     * Return type of JDI connector used for connecting to debugee VM, specified by
     * <code>-connector</code> command line option, or
     * "<i>listening</i>" string by default.
     *
     * Possible values for this option are:
     * <ul>
     * <li> "<code>attaching</code>"
     * <li> "<code>listening</code>"
     * </ul>
     *
     * @see #isAttachingConnector()
     * @see #isListeningConnector()
     * @see #setRawArguments(String[])
     */
    public String getConnectorType() {
        return options.getProperty("connector", "listening");
    }

    /**
     * Return <i>true</i> if type of the used JDI connector is <code>attaching</code>.
     *
     * @see #getConnectorType()
     */
    public boolean isAttachingConnector() {
        return getConnectorType().equals("attaching");
    }

    /**
     * Return <i>true</i> if type of the used JDI connector is <code>listening</code>.
     *
     * @see #getConnectorType()
     */
    public boolean isListeningConnector() {
        return getConnectorType().equals("listening");
    }

    /**
     * Return <i>true</i> if connector type is not actually specified.
     * In this case getConnectorType() returns some default connector type.
     *
     * @see #getConnectorType()
     */
    public boolean isDefaultConnector() {
        return options.getProperty("connector") == null;
    }

    /**
     * Return type of JDWP transport for connecting to debugee VM, specified by
     * <code>-transport</code> command line option, or
     * "<i>socket</i>" string by default.
     *
     * Possible values for this option are:
     * <ul>
     * <li> "<code>socket</code>"
     * <li> "<code>shmem</code>"
     * </ul>
     *
     * @see #getTransportName()
     * @see #isSocketTransport()
     * @see #isShmemTransport()
     * @see #setRawArguments(String[])
     */
    public String getTransportType() {
        return options.getProperty("transport", "socket");
    }

    /**
     * Return transport name corresponding to the used JDWP transport type.
     *
     * @see #getTransportType()
     */
    public String getTransportName() {
        if (isSocketTransport()) {
            return "dt_socket";
        } else if (isShmemTransport()) {
            return "dt_shmem";
        } else {
            throw new TestBug("Undefined transport type");
        }
    }

    /**
     * Return <i>true</i> if the used JDWP transport type is <code>socket</code>,
     * either by specifying in command line or as a platform default transport.
     *
     * @see #getTransportType()
     */
    public boolean isSocketTransport() {
        String transport = getTransportType();
        return transport.equals("socket");
    }

    /**
     * Return <i>true</i> if the used JDWP transport type is <code>shmem</code>,
     * either by specifying in command line or as a platform default transport.
     *
     * @see #getTransportType()
     */
    public boolean isShmemTransport() {
        String transport = getTransportType();
        return transport.equals("shmem");
    }

    /**
     * Return <i>true</i> if transport type is not actually specified.
     * In this case getTransportType() returns some default transport kind.
     *
     * @see #getTransportType()
     */
    public boolean isDefaultTransport() {
        return options.getProperty("transport") == null;
    }

    /**
     * Create <code>Log</code> for debugee application using command line options.
     */
    public Log createDebugeeLog() {
        return new Log(System.err, this);
    };

    /**
     * Create IOPipe for debugee application using command line options.
     */
    public IOPipe createDebugeeIOPipe() {
        return createDebugeeIOPipe(createDebugeeLog());
    };

    /**
     * Create IOPipe for debugee application using connection
     * parameters from the command line and specify Log.
     */
    public IOPipe createDebugeeIOPipe(Log log) {
        return new IOPipe(this, log);
    };

    /**
     * Check if an option is aloowed and has proper value.
     * This method is invoked by <code>parseArgumentss()</code>
     *
     * @param option option name
     * @param value string representation of value
     *                      (could be an empty string too)
     *              null if this option has no value
     * @return <i>true</i> if option is allowed and has proper value
     *         <i>false</i> if otion is not admissible
     *
     * @throws <i>BadOption</i> if option has an illegal value
     *
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {

        if(option.equals("traceAll"))
            return true;

        // option with any string value
        if (option.equals("debugee.vmkeys")) {
            return true;
        }

        // option with any nonempty string value
        if (option.equals("test.host")
            || option.equals("debugee.host")
            || option.equals("debugee.vmkind")
            || option.equals("debugee.vmhome")
            || option.equals("transport.shname")) {
            if (value.length() <= 0) {
                throw new BadOption(option + ": cannot be an empty string");
            }
            return true;
        }

        // option with positive integer port value
        if (option.equals("transport.port")
            || option.equals("bind.port")
            || option.equals("pipe.port")) {
            try {
                int number = Integer.parseInt(value);
                if (number < 0) {
                    throw new BadOption(option + ": must be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw new BadOption(option + ": must be an integer");
            }
            return true;
        }

        // options with enumerated values

        if (option.equals("debugee.suspend")) {
            if ((!value.equals("yes"))
                && (!value.equals("no"))
                && (!value.equals("default"))) {
                throw new BadOption(option + ": must be one of: "
                                           + "yes, no, default");
            }
            return true;
        }

        if (option.equals("debugee.launch")) {
            if ((!value.equals("local"))
                && (!value.equals("remote"))
                && (!value.equals("manual"))) {
                throw new BadOption(option + ": must be one of: "
                                           + "local, remote, manual " + value);
            }
            return true;
        }

        if (option.equals("jvmdi.strict")) {
            if ((!value.equals("yes"))
                && (!value.equals("no"))
                && (!value.equals("default"))) {
                throw new BadOption(option + ": must be one of: "
                                           + "yes, no, default");
            }
            return true;
        }

        if (option.equals("transport")) {
            if ((!value.equals("socket"))
                && (!value.equals("shmem"))) {
                throw new BadOption(option + ": must be one of: "
                                           + "socket, shmem");
            }
            return true;
        }

        if (option.equals("connector")) {
            if ((!value.equals("attaching"))
                && (!value.equals("listening"))) {
                throw new BadOption(option + ": value must be one of: "
                                           + "attaching, listening");
            }
            return true;
        }

        if (option.equals("transport.address")) {
            if (!value.equals("dynamic")) {
                throw new BadOption(option + ": must be only: "
                                           + "dynamic");
            }
            return true;
        }

        return super.checkOption(option, value);
    }

    /**
     * Check if the values of all options are consistent.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @throws <i>BadOption</i> if options have inconsistent values
     *
     * @see #parseArguments()
     */
    protected void checkOptions() {
        super.checkOptions();
    }

    private String findFreePort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(0);
            return String.valueOf(ss.getLocalPort());
        } catch (IOException e) {
            return null;
        } finally {
            try {
                ss.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

} // DebugeeArgumentHandler
