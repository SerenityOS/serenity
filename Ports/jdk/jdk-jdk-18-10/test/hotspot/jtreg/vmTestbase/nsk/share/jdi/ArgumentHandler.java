/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.VirtualMachine;

/**
 * Parser for JDI test's specific command-line arguments.
 * <p>
 * <code>ArgumentHandler</code> handles JDI test's specific
 * arguments related to launching debugee VM using JDI features
 * in addition to general arguments recognized by
 * <code>DebugeeArgumentHandler</code> and <code>ArgumentParser</code>.
 * <p>
 * Following is the list of specific options recognized by
 * <code>AgrumentHandler</code>:
 * <ul>
 * <li> <code>-connector=[launching|attaching|listening|default]</code> -
 *   JDI connector type
 * <li> <code>-transport=[socket|shmem|default]</code> -
 *   JDWP transport kind
 * <li> <code>-jdi.trace=[none|all|events|sends|receives|reftypes|objrefs]</code> -
 *   JDI trace mode for debugee VM
 * </ul>
 * <p>
 * See also list of arguments recognized by the base <code>DebugeeArgumentHandler</code>
 * and <code>ArgumentParser</code> classes.
 * <p>
 * See also description of <code>ArgumentParser</code> how to work with
 * command line arguments and options.
 *
 * @see ArgumentParser
 * @see DebugeeArgumentHandler
 */
public class ArgumentHandler extends DebugeeArgumentHandler {

    static private String JDI_CONNECTOR_NAME_PREFIX = "com.sun.jdi.";

    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param  args  Array of the raw command-line arguments.
     *
     * @throws  NullPointerException  If <code>args==null</code>.
     * @throws  IllegalArgumentException  If Binder or Log options
     *                                    are set incorrectly.
     *
     * @see #setRawArguments(String[])
     */
    public ArgumentHandler(String args[]) {
        super(args);
//        parseArguments();
    }

    /**
     * Overriden method returns transport type for JDWP connection, specified by
     * <code>-transport</code> command line option, or <i>"default"</i> value
     * by default.
     *
     * @see #getTransportName()
     * @see #isSocketTransport()
     * @see #isShmemTransport()
     * @see #isDefaultTransport()
     * @see #setRawArguments(String[])
     */
    public String getTransportType() {
        return options.getProperty("transport", "default");
    }

    /**
     * Overriden method returns <i>true</i> if <code>socket</code> transport
     * is used either as specified or as a platform default transport.
     *
     * @see #getTransportType()
     */
    public boolean isSocketTransport() {
        String transport = getTransportType();
        if (transport.equals("socket"))
            return true;
        if (transport.equals("shmem"))
            return false;
        if (transport.equals("default")) {
            String arch = getArch();
            if (arch == null)
                if (System.getProperty("os.arch").equals("windows-i586"))
                    return false;
                else
                    return true;
            else if (arch.equals("windows-i586"))
                return false;
            else
                return true;
        }
        throw new TestBug("Bad value of argument transport: " + transport);
    }

    /**
     * Overriden method returns <i>true</i> if <code>shmem</code> transport is used
     * either as specified or as a platform default transport.
     *
     * @see #getTransportType()
     */
    public boolean isShmemTransport() {
        return ! isSocketTransport();
    }

    /**
     * Overriden method returns <i>true</i> if transport type is <code>default</code>.
     *
     * @see #getTransportType()
     */
    public boolean isDefaultTransport() {
        String transport = getTransportType();
        return transport.equals("default");
    }

    /**
     * Overriden methos returns JDI connector type, specified by
     * <code>-connector</code>. or <i>"default"</i> value by default.
     *
     * @see #getConnectorName()
     * @see #isLaunchingConnector()
     * @see #isAttachingConnector()
     * @see #isListeningConnector()
     * @see #isDefaultConnector()
     * @see #setRawArguments(String[])
     */
    public String getConnectorType() {
        return options.getProperty("connector", "default");
    }

    /**
     * Overriden method returns full connector name corresponding to
     * the used connector and transport types.
     *
     * @see #getConnectorType()
     * @see #getTransportType()
     */
    public String getConnectorName() {
        if (isLaunchingConnector()) {
            if (isRawLaunchingConnector())
                return JDI_CONNECTOR_NAME_PREFIX + "RawCommandLineLaunch";
            return JDI_CONNECTOR_NAME_PREFIX + "CommandLineLaunch";
        }
        if (isAttachingConnector()) {
            if (isSocketTransport())
                return JDI_CONNECTOR_NAME_PREFIX + "SocketAttach";
            if (isShmemTransport())
                return JDI_CONNECTOR_NAME_PREFIX + "SharedMemoryAttach";
            return JDI_CONNECTOR_NAME_PREFIX + "SocketAttach";
        }
        if (isListeningConnector()) {
            if (isSocketTransport())
                return JDI_CONNECTOR_NAME_PREFIX + "SocketListen";
            if (isShmemTransport())
                return JDI_CONNECTOR_NAME_PREFIX + "SharedMemoryListen";
            return JDI_CONNECTOR_NAME_PREFIX + "SocketListen";
        }
        throw new Failure("Unable to find full name of connector \"" + getConnectorType()
                        + "\" for transport \"" + getTransportType() + "\"");
    }

    /**
     * Overriden method returns <i>true</i> if connector type is <code>default</code>.
     *
     * @see #getConnectorType()
     */
    public boolean isDefaultConnector() {
        return getConnectorType().equals("default");
    }

    /**
     * Return <i>true</i> if connector type is <code>launching</code>
     * <code>rawlaunching<code> or <code>default</code>.
     *
     * @see #getConnectorType()
     */
    public boolean isLaunchingConnector() {
        return getConnectorType().equals("launching")
                || getConnectorType().equals("rawlaunching")
                || getConnectorType().equals("default");
    }

    /**
     * Return <i>true</i> if connector type is <code>rawlaunching</code>.
     *
     * @see #getConnectorType()
     */
    public boolean isRawLaunchingConnector() {
        return getConnectorType().equals("rawlaunching");
    }

    /**
     * Return string representation of debug trace mode, specified by
     * <code>-jdi.trace</code> command line option, or <i>"none"</i>
     * value by default.
     * <p>
     * Possible values for this option are the same as symbolic constants names
     * in <code>com.sun.jdi.VirtualMachine</code> interface:
     * <br>&nbsp;&nbsp;<code>"all"</code>, or
     * <br>&nbsp;&nbsp;<code>"events"</code>, or
     * <br>&nbsp;&nbsp;<code>"none"</code>, or
     * <br>&nbsp;&nbsp;<code>"objrefs"</code>, or
     * <br>&nbsp;&nbsp;<code>"receives"</code>, or
     * <br>&nbsp;&nbsp;<code>"reftypes"</code>, or
     * <br>&nbsp;&nbsp;<code>"sends"</code>.
     *
     * @see #getTraceMode()
     * @see #setRawArguments(String[])
     */
    public String getTraceModeString() {
        return options.getProperty("jdi.trace", "none");
    }

    /**
     * Return integer code corresponding to debug trace mode, specified by
     * <code>-jdi.trace</code> command line option, or <i>VirtualMachine.TRACE_NONE</i>
     * value by default.
     * <p>
     * Possible values are the same as symbolic constant values
     * in <code>com.sun.jdi.VirtualMachine</code> interface:
     * <ul>
     *   <li><code>VirtualMachine.TRACE_ALL</code>
     *   <li><code>VirtualMachine.TRACE_EVENTS</code>
     *   <li><code>VirtualMachine.TRACE_NONE</code>
     *   <li><code>VirtualMachine.TRACE_OBJREFS</code>
     *   <li><code>VirtualMachine.TRACE_RECEIVES</code>
     *   <li><code>VirtualMachine.TRACE_REFTYPES</code>
     *   <li><code>VirtualMachine.TRACE_SENDS</code>
     * </ul>
     *
     * @see #getTraceModeString()
     * @see #setRawArguments(String[])
     */
    public int getTraceMode() {
        String val = getTraceModeString();
        if (val == null)
            return VirtualMachine.TRACE_NONE;
        if (val.equals("none"))
            return VirtualMachine.TRACE_NONE;
        if (val.equals("all"))
            return VirtualMachine.TRACE_ALL;
        if (val.equals("events"))
            return VirtualMachine.TRACE_EVENTS;
        if (val.equals("objrefs"))
            return VirtualMachine.TRACE_OBJREFS;
        if (val.equals("receives"))
            return VirtualMachine.TRACE_RECEIVES;
        if (val.equals("reftypes"))
            return VirtualMachine.TRACE_REFTYPES;
        if (val.equals("sends"))
            return VirtualMachine.TRACE_SENDS;
        throw new TestBug("Unknown JDI trace mode string: " + val);
    }

    // delay between connection attempts, used in connectors tests
    public int getConnectionDelay() {
        String value = options.getProperty("connectionDelay", "4000");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"connectionDelay\" argument: " + value);
        }
    }


    /**
     * Return <i>true</i> if the test should pass in any case i.e.
     * an entity specified by the arguments <code>entry[]</code> is
     * not implemented on the tested platform. Name of the tested
     * platform is resolved from the "<code>-arch</code>" option.
     *
     * @param  entry   Array with the arguments which are specifing
     * the entity.
     *
     * @throws Oddity  If test parameter<code>-arch</code>
     *                 has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public boolean shouldPass(String entry[]) {
        String arch;
        boolean found = false;

        if ((arch=getArch()) == null)
            throw new Oddity("Test parameter -arch should be set");

        for (int i=0; i < CheckedFeatures.notImplemented.length; i++) {
            if (CheckedFeatures.notImplemented[i][0].equals(arch) &&
                CheckedFeatures.notImplemented[i].length == (entry.length+1)) {
                for (int j=1; j < (entry.length+1); j++) {
                    if (CheckedFeatures.notImplemented[i][j].equals(entry[j-1]))
                        found = true;
                    else {
                        found = false;
                        break;
                    }
                }
                if (found) return true; // the entry[] is not implemented
            }
        }

        return false; // the entry[] is implemented
    }

    /**
     * Return <i>true</i> if the test should pass in any case i.e.
     * an entity specified by the argument <code>entry</code> is
     * not implemented on the tested platform. Name of the tested
     * platform is resolved from the "<code>-arch</code>" option.
     *
     * @param  entry   String with the argument which is specifing
     * the entity.
     *
     * @throws Oddity  If test parameter<code>-arch</code>
     *                 has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public boolean shouldPass(String entry) {
        return (shouldPass(new String[] {entry}));
    }

    /**
     * Return <i>true</i> if the test should pass in any case i.e.
     * an entity specified by the arguments <code>entry1</code> and
     * <code>entry2</code> is not implemented on the tested platform.
     * The entry is considered to be not implemented if exact entry
     *  "entry1, entry2" or its main entry "entry1" is not implemented.
     *
     * Name of the tested platform is resolved from the "<code>-arch</code>"
     * option.
     *
     * @param  entry1   String with the argument 1 which is specifing
     * the entity.
     *
     * @param  entry2   String with the argument 2 which is specifing
     * the entity.
     *
     * @throws Oddity  If test parameter<code>-arch</code>
     *                 has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public boolean shouldPass(String entry1, String entry2) {
        return ( shouldPass(new String[] {entry1, entry2}) ||
                 shouldPass(new String[] {entry1})  );
    }

    /**
     * Check if an option is admissible and has proper value.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @param option option name
     * @param value string representation of value (could be an empty string)
     *              null if this option has no value
     * @return <i>true</i> if option is admissible and has proper value
     *         <i>false</i> if otion is not admissible
     *
     * @throws <i>BadOption</i> if option has illegal value
     *
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {

        // check options with enumerated values

        if (option.equals("connectionDelay")) {
            try {
                int number = Integer.parseInt(value);
                if (number <= 0) {
                    throw new BadOption(option + ": must be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw new BadOption(option + ": must be an integer");
            }

            return true;
        }

        if (option.equals("connector")) {
            if ((!value.equals("launching"))
                && (!value.equals("rawlaunching"))
                && (!value.equals("attaching"))
                && (!value.equals("listening"))
                && (!value.equals("default"))) {
                throw new BadOption(option + ": value must be one of: "
                                           + "launching, attaching, listening, default");
            }
            return true;
        }

        if (option.equals("transport")) {
            if ((!value.equals("socket"))
                && (!value.equals("shmem"))
                && (!value.equals("default"))) {
                throw new BadOption(option + ": must be one of: "
                                           + "socket, shmem, default");
            }
            return true;
        }

        if (option.equals("jdi.trace")) {
            if ((!value.equals("all"))
                && (!value.equals("none"))
                && (!value.equals("events"))
                && (!value.equals("receives"))
                && (!value.equals("sends"))
                && (!value.equals("reftypes"))
                && (!value.equals("objrefs"))) {
                throw new BadOption(option + ": value must be one of: "
                                           + "none, all, events, receives, sends, reftypes, objrefs");
            }
            return true;
        }

        return super.checkOption(option, value);
    }

    /**
     * Check options against inconcistence.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @see #parseArguments()
     */
    protected void checkOptions() {
/*
        if (isTransportAddressDynamic() &&
                (!isDefaultConnector() && isRawLaunchingConnector())) {
            throw new BadOption("-transport.address=dynamic should NOT be used with"
                                + " -connector=rawlaunching");
        }
 */

        if (! isLaunchedLocally() && ! isDefaultDebugeeSuspendMode()) {
            throw new BadOption("inconsistent options: "
                                + "-debugee.launch=" + getLaunchMode()
                                + " and -debugee.suspend=" + getDebugeeSuspendMode());
        }

        if (! isLaunchedLocally() && isLaunchingConnector()) {
            throw new BadOption("inconsistent options: "
                                + "-debugee.launch=" + getLaunchMode()
                                + " and -connector=" + getConnectorType());
        }

        if (isLaunchingConnector() && ! isDefaultTransport()) {
            throw new BadOption("inconsistent options: "
                                + "-connector=" + getConnectorType()
                                + " and -transport=" + getTransportType());
        }

        if (! isLaunchingConnector() && isDefaultTransport()) {
            throw new BadOption("inconsistent options: "
                                + "-connector=" + getConnectorType()
                                + " and -transport=" + getTransportType());
        }

        if (! isDefaultJVMDIStrictMode()) {
            throw new BadOption("unsupported options: "
                                + "jvmdi.strict: non default JVMDI strict mode is not supported now" + getJVMDIStrictMode());
        }

/*
        if (! isLaunchedLocally() && ! isDefaultJVMDIStrictMode()) {
            throw new BadOption("inconsistent options: "
                                + "-launch.mode=" + getLaunchMode()
                                + " and -jvmdi.strict=" + getJVMDIStrictMode());
        }
 */

        super.checkOptions();
    }
}

/**
 * This is an auxiliary class intended for <code>ArgumentHandler</code>.
 * The following information is used by the <code>ArgumentHandler</code>
 * for resolving features (i.e., JDI connectors and transport names)
 * which are not implemented on given platform (the first column).
 * This list is actual for JDK 1.3.x, 1.4.x, 1.5.0, 1.6.0.
 *
 * @see ArgumentHandler
 */
class CheckedFeatures {

    static final String[][] notImplemented = {

            // attaching connectors
        /*
         * From docs/technotes/guides/jpda/conninv.html:
         * "
         *  This connector can be used by a debugger application to attach to
         *  a currently running target VM through the shared memory transport. It is
         *  available only on the Microsoft Windows platform.
         *  "
         */
        {"linux-i586",      "com.sun.jdi.SharedMemoryAttach"},
        {"linux-ia64",      "com.sun.jdi.SharedMemoryAttach"},
        {"linux-amd64",     "com.sun.jdi.SharedMemoryAttach"},
        {"linux-x64",       "com.sun.jdi.SharedMemoryAttach"},
        {"linux-aarch64",   "com.sun.jdi.SharedMemoryAttach"},
        {"linux-arm",       "com.sun.jdi.SharedMemoryAttach"},
        {"linux-ppc64",     "com.sun.jdi.SharedMemoryAttach"},
        {"linux-ppc64le",   "com.sun.jdi.SharedMemoryAttach"},
        {"linux-s390x",     "com.sun.jdi.SharedMemoryAttach"},
        {"macosx-amd64",    "com.sun.jdi.SharedMemoryAttach"},
        {"mac-x64",         "com.sun.jdi.SharedMemoryAttach"},
        {"macosx-aarch64",  "com.sun.jdi.SharedMemoryAttach"},
        {"mac-aarch64",     "com.sun.jdi.SharedMemoryAttach"},
        {"aix-ppc64",       "com.sun.jdi.SharedMemoryAttach"},

            // listening connectors
        /*
         * From docs/technotes/guides/jpda/conninv.html:
         * "
         *  This connector can be used by a debugger application to accept a
         *  connection from  a separately invoked target VM through the shared memory
         *  transport.
         *  It is available only on the Microsoft Windows platform.
         *  "
         */
        {"linux-i586",      "com.sun.jdi.SharedMemoryListen"},
        {"linux-ia64",      "com.sun.jdi.SharedMemoryListen"},
        {"linux-amd64",     "com.sun.jdi.SharedMemoryListen"},
        {"linux-x64",       "com.sun.jdi.SharedMemoryListen"},
        {"linux-aarch64",   "com.sun.jdi.SharedMemoryListen"},
        {"linux-arm",       "com.sun.jdi.SharedMemoryListen"},
        {"linux-ppc64",     "com.sun.jdi.SharedMemoryListen"},
        {"linux-ppc64le",   "com.sun.jdi.SharedMemoryListen"},
        {"linux-s390x",     "com.sun.jdi.SharedMemoryListen"},
        {"macosx-amd64",    "com.sun.jdi.SharedMemoryListen"},
        {"mac-x64",         "com.sun.jdi.SharedMemoryListen"},
        {"macosx-aarch64",  "com.sun.jdi.SharedMemoryListen"},
        {"mac-aarch64",     "com.sun.jdi.SharedMemoryListen"},
        {"aix-ppc64",       "com.sun.jdi.SharedMemoryListen"},

            // launching connectors
        /*
         * From docs/technotes/guides/jpda/conninv.html:
         * "
         *  Sun Command Line Launching Connector
         *  This connector can be used by a debugger application to launch a
         *  Sun VM or any other VM which supports the same invocation options with
         *  respect to debugging. The details of launching the VM and specifying the
         *  necessary debug options are handled by the connector. The underlying
         *  transport used by this connector depends on the platform. On Microsoft
         *  Windows, the shared memory transport is used. On Linux the socket transport is used.
         * "
         */
        {"linux-i586",      "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-i586",      "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-ia64",      "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-ia64",      "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-amd64",     "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-amd64",     "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-x64",       "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-x64",       "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-aarch64",   "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-aarch64",   "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-arm",       "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-arm",       "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-ppc64",     "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-ppc64",     "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-ppc64le",   "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-ppc64le",   "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"linux-s390x",     "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"linux-s390x",     "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"windows-i586",    "com.sun.jdi.CommandLineLaunch", "dt_socket"},
        {"windows-i586",    "com.sun.jdi.RawCommandLineLaunch", "dt_socket"},

        {"windows-ia64",    "com.sun.jdi.CommandLineLaunch", "dt_socket"},
        {"windows-ia64",    "com.sun.jdi.RawCommandLineLaunch", "dt_socket"},

        {"windows-amd64",   "com.sun.jdi.CommandLineLaunch", "dt_socket"},
        {"windows-amd64",   "com.sun.jdi.RawCommandLineLaunch", "dt_socket"},

        {"windows-x64",     "com.sun.jdi.CommandLineLaunch", "dt_socket"},
        {"windows-x64",     "com.sun.jdi.RawCommandLineLaunch", "dt_socket"},

        {"macosx-amd64",     "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"macosx-amd64",     "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"mac-x64",          "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"mac-x64",          "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"macosx-aarch64",   "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"macosx-aarch64",   "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"mac-aarch64",      "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"mac-aarch64",      "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        {"aix-ppc64",       "com.sun.jdi.CommandLineLaunch", "dt_shmem"},
        {"aix-ppc64",       "com.sun.jdi.RawCommandLineLaunch", "dt_shmem"},

        // shared memory transport is implemented only on windows platform
        {"linux-i586",      "dt_shmem"},
        {"linux-ia64",      "dt_shmem"},
        {"linux-amd64",     "dt_shmem"},
        {"linux-x64",       "dt_shmem"},
        {"linux-aarch64",   "dt_shmem"},
        {"linux-arm",       "dt_shmem"},
        {"linux-ppc64",     "dt_shmem"},
        {"linux-ppc64le",   "dt_shmem"},
        {"linux-s390x",     "dt_shmem"},
        {"macosx-amd64",    "dt_shmem"},
        {"mac-x64",         "dt_shmem"},
        {"macosx-aarch64",  "dt_shmem"},
        {"mac-aarch64",     "dt_shmem"},
        {"aix-ppc64",       "dt_shmem"},
    };
}
