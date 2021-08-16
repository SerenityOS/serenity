/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jshell.execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionEnv;
import static jdk.jshell.execution.Util.remoteInputOutput;

/**
 * The implementation of {@link jdk.jshell.spi.ExecutionControl} that the
 * JShell-core uses by default.
 * Launches a remote process -- the "remote agent".
 * Interfaces to the remote agent over a socket and via JDI.
 * Designed to work with {@link RemoteExecutionControl}.
 *
 * @author Robert Field
 * @author Jan Lahoda
 * @since 9
 */
public class JdiDefaultExecutionControl extends JdiExecutionControl {

    private VirtualMachine vm;
    private Process process;
    private final String remoteAgent;

    private final Object STOP_LOCK = new Object();
    private boolean userCodeRunning = false;

    /**
     * Creates an ExecutionControl instance based on a JDI
     * {@code ListeningConnector} or {@code LaunchingConnector}.
     *
     * Initialize JDI and use it to launch the remote JVM. Set-up a socket for
     * commands and results. This socket also transports the user
     * input/output/error.
     *
     * @param env the context passed by
     * {@link jdk.jshell.spi.ExecutionControl#start(jdk.jshell.spi.ExecutionEnv) }
     * @param remoteAgent the remote agent to launch
     * @param isLaunch does JDI do the launch? That is, LaunchingConnector,
     * otherwise we start explicitly and use ListeningConnector
     * @param host explicit hostname to use, if null use discovered
     * hostname, applies to listening only (!isLaunch)
     * @return the channel
     * @throws IOException if there are errors in set-up
     */
    static ExecutionControl create(ExecutionEnv env, String remoteAgent,
            boolean isLaunch, String host, int timeout) throws IOException {
        try (final ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            // timeout on I/O-socket
            listener.setSoTimeout(timeout);
            int port = listener.getLocalPort();

            // Set-up the JDI connection
            JdiInitiator jdii = new JdiInitiator(port,
                    env.extraRemoteVMOptions(), remoteAgent, isLaunch, host,
                    timeout, Collections.emptyMap());
            VirtualMachine vm = jdii.vm();
            Process process = jdii.process();

            List<Consumer<String>> deathListeners = new ArrayList<>();
            Util.detectJdiExitEvent(vm, s -> {
                for (Consumer<String> h : deathListeners) {
                    h.accept(s);
                }
            });

            // Set-up the commands/reslts on the socket.  Piggy-back snippet
            // output.
            Socket socket = listener.accept();
            // out before in -- match remote creation so we don't hang
            OutputStream out = socket.getOutputStream();
            Map<String, OutputStream> outputs = new HashMap<>();
            outputs.put("out", env.userOut());
            outputs.put("err", env.userErr());
            Map<String, InputStream> input = new HashMap<>();
            input.put("in", env.userIn());
            return remoteInputOutput(socket.getInputStream(), out, outputs, input,
                    (objIn, objOut) -> new JdiDefaultExecutionControl(env,
                                        objOut, objIn, vm, process, remoteAgent, deathListeners));
        }
    }

    /**
     * Create an instance.
     *
     * @param cmdout the output for commands
     * @param cmdin the input for responses
     */
    private JdiDefaultExecutionControl(ExecutionEnv env,
            ObjectOutput cmdout, ObjectInput cmdin,
            VirtualMachine vm, Process process, String remoteAgent,
            List<Consumer<String>> deathListeners) {
        super(cmdout, cmdin);
        this.vm = vm;
        this.process = process;
        this.remoteAgent = remoteAgent;
        // We have now succeeded in establishing the connection.
        // If there is an exit now it propagates all the way up
        // and the VM should be disposed of.
        deathListeners.add(s -> env.closeDown());
        deathListeners.add(s -> disposeVM());
     }

    @Override
    public String invoke(String classname, String methodname)
            throws RunException,
            EngineTerminationException, InternalException {
        String res;
        synchronized (STOP_LOCK) {
            userCodeRunning = true;
        }
        try {
            res = super.invoke(classname, methodname);
        } finally {
            synchronized (STOP_LOCK) {
                userCodeRunning = false;
            }
        }
        return res;
    }

    /**
     * Interrupts a running remote invoke by manipulating remote variables
     * and sending a stop via JDI.
     *
     * @throws EngineTerminationException the execution engine has terminated
     * @throws InternalException an internal problem occurred
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        synchronized (STOP_LOCK) {
            if (!userCodeRunning) {
                return;
            }

            vm().suspend();
            try {
                OUTER:
                for (ThreadReference thread : vm().allThreads()) {
                    // could also tag the thread (e.g. using name), to find it easier
                    for (StackFrame frame : thread.frames()) {
                        if (remoteAgent.equals(frame.location().declaringType().name()) &&
                                (    "invoke".equals(frame.location().method().name())
                                || "varValue".equals(frame.location().method().name()))) {
                            ObjectReference thiz = frame.thisObject();
                            Field inClientCode = thiz.referenceType().fieldByName("inClientCode");
                            Field expectingStop = thiz.referenceType().fieldByName("expectingStop");
                            Field stopException = thiz.referenceType().fieldByName("stopException");
                            if (((BooleanValue) thiz.getValue(inClientCode)).value()) {
                                thiz.setValue(expectingStop, vm().mirrorOf(true));
                                ObjectReference stopInstance = (ObjectReference) thiz.getValue(stopException);

                                vm().resume();
                                debug("Attempting to stop the client code...\n");
                                thread.stop(stopInstance);
                                thiz.setValue(expectingStop, vm().mirrorOf(false));
                            }

                            break OUTER;
                        }
                    }
                }
            } catch (ClassNotLoadedException | IncompatibleThreadStateException | InvalidTypeException ex) {
                throw new InternalException("Exception on remote stop: " + ex);
            } finally {
                vm().resume();
            }
        }
    }

    @Override
    public void close() {
        super.close();
        disposeVM();
    }

    private synchronized void disposeVM() {
        try {
            if (vm != null) {
                vm.dispose(); // This could NPE, so it is caught below
                vm = null;
            }
        } catch (VMDisconnectedException ex) {
            // Ignore if already closed
        } catch (Throwable ex) {
            debug(ex, "disposeVM");
        } finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }

    @Override
    protected synchronized VirtualMachine vm() throws EngineTerminationException {
        if (vm == null) {
            throw new EngineTerminationException("VM closed");
        } else {
            return vm;
        }
    }

    /**
     * Log debugging information. Arguments as for {@code printf}.
     *
     * @param format a format string as described in Format string syntax
     * @param args arguments referenced by the format specifiers in the format
     * string.
     */
    private static void debug(String format, Object... args) {
        // Reserved for future logging
    }

    /**
     * Log a serious unexpected internal exception.
     *
     * @param ex the exception
     * @param where a description of the context of the exception
     */
    private static void debug(Throwable ex, String where) {
        // Reserved for future logging
    }

}
