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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.RunException;
import static jdk.jshell.execution.Util.forwardExecutionControlAndIO;

/**
 * A custom remote agent to verify aux channel and custom ExecutionControl.
 */
public class MyRemoteExecutionControl extends DirectExecutionControl implements ExecutionControl {

    static PrintStream auxPrint;

    /**
     * Launch the agent, connecting to the JShell-core over the socket specified
     * in the command-line argument.
     *
     * @param args standard command-line arguments, expectation is the socket
     * number is the only argument
     * @throws Exception any unexpected exception
     */
    public static void main(String[] args) throws Exception {
        try {
            String loopBack = null;
            Socket socket = new Socket(loopBack, Integer.parseInt(args[0]));
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            Map<String, Consumer<OutputStream>> outputs = new HashMap<>();
            outputs.put("out", st -> System.setOut(new PrintStream(st, true)));
            outputs.put("err", st -> System.setErr(new PrintStream(st, true)));
            outputs.put("aux", st -> { auxPrint = new PrintStream(st, true); });
            Map<String, Consumer<InputStream>> input = new HashMap<>();
            input.put("in", st -> System.setIn(st));
            forwardExecutionControlAndIO(new MyRemoteExecutionControl(), inStream, outStream, outputs, input);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    @Override
    public String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException {
        auxPrint.print(varName);
        return super.varValue(className, varName);
    }

    @Override
    public Object extensionCommand(String className, Object arg)
            throws RunException, EngineTerminationException, InternalException {
        if (!arg.equals("test")) {
            throw new InternalException("expected extensionCommand arg to be 'test' got: " + arg);
        }
        return "ribbit";
    }

}
