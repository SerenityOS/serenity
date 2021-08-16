/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import jdk.jshell.execution.JdiExecutionControl;
import jdk.jshell.execution.JdiInitiator;
import jdk.jshell.execution.Util;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionEnv;
import static org.testng.Assert.fail;
import static jdk.jshell.execution.Util.remoteInputOutput;

class MyExecutionControl extends JdiExecutionControl {

    private static final String REMOTE_AGENT = MyRemoteExecutionControl.class.getName();
    private static final int TIMEOUT = 2000;

    private VirtualMachine vm;
    private Process process;

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
     * @return the channel
     * @throws IOException if there are errors in set-up
     */
    static ExecutionControl make(ExecutionEnv env, UserJdiUserRemoteTest test) throws IOException {
        try (final ServerSocket listener = new ServerSocket(0)) {
            // timeout for socket
            listener.setSoTimeout(TIMEOUT);
            int port = listener.getLocalPort();

            // Set-up the JDI connection
            List<String> opts = new ArrayList<>(env.extraRemoteVMOptions());
            opts.add("-classpath");
            opts.add(System.getProperty("java.class.path")
                    + System.getProperty("path.separator")
                    + System.getProperty("user.dir"));
            JdiInitiator jdii = new JdiInitiator(port,
                    opts, REMOTE_AGENT, true, null, TIMEOUT, Collections.emptyMap());
            VirtualMachine vm = jdii.vm();
            Process process = jdii.process();

            List<Consumer<String>> deathListeners = new ArrayList<>();
            deathListeners.add(s -> env.closeDown());
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
            outputs.put("aux", test.auxStream);
            Map<String, InputStream> input = new HashMap<>();
            input.put("in", env.userIn());
            ExecutionControl myec = remoteInputOutput(socket.getInputStream(),
                    out, outputs, input,
                    (objIn, objOut) -> new MyExecutionControl(objOut, objIn, vm, process, deathListeners));
            test.currentEC = myec;
            return myec;
        }
    }

    /**
     * Create an instance.
     *
     * @param out the output for commands
     * @param in the input for responses
     */
    private MyExecutionControl(ObjectOutput out, ObjectInput in,
            VirtualMachine vm, Process process,
            List<Consumer<String>> deathListeners) {
        super(out, in);
        this.vm = vm;
        this.process = process;
        deathListeners.add(s -> disposeVM());
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
        } catch (Throwable e) {
            fail("disposeVM threw: " + e);
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

}
