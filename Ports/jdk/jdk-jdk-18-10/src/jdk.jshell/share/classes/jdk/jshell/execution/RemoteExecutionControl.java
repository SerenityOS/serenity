/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.Socket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import jdk.jshell.spi.ExecutionControl;
import static jdk.jshell.execution.Util.forwardExecutionControlAndIO;

/**
 * The remote agent runs in the execution process (separate from the main JShell
 * process). This agent loads code over a socket from the main JShell process,
 * executes the code, and other misc, Specialization of
 * {@link DirectExecutionControl} which adds stop support controlled by
 * an external process. Designed to work with {@link JdiDefaultExecutionControl}.
 *
 * @author Jan Lahoda
 * @author Robert Field
 * @since 9
 */
public class RemoteExecutionControl extends DirectExecutionControl implements ExecutionControl {

    /**
     * Launch the agent, connecting to the JShell-core over the socket specified
     * in the command-line argument.
     *
     * @param args standard command-line arguments, expectation is the socket
     * number is the only argument
     * @throws Exception any unexpected exception
     */
    public static void main(String[] args) throws Exception {
        String loopBack = null;
        Socket socket = new Socket(loopBack, Integer.parseInt(args[0]));
        InputStream inStream = socket.getInputStream();
        OutputStream outStream = socket.getOutputStream();
        Map<String, Consumer<OutputStream>> outputs = new HashMap<>();
        outputs.put("out", st -> System.setOut(new PrintStream(st, true)));
        outputs.put("err", st -> System.setErr(new PrintStream(st, true)));
        Map<String, Consumer<InputStream>> input = new HashMap<>();
        input.put("in", System::setIn);
        forwardExecutionControlAndIO(new RemoteExecutionControl(), inStream, outStream, outputs, input);
    }

    // These three variables are used by the main JShell process in interrupting
    // the running process.  Access is via JDI, so the reference is not visible
    // to code inspection.
    private boolean inClientCode; // Queried by the main process (in superclass)
    private boolean expectingStop; // Set by the main process
// Set by the main process

    // thrown by the main process via JDI:
    private final StopExecutionException stopException = new StopExecutionException();

    /**
     * Creates an instance, delegating loader operations to the specified
     * delegate.
     *
     * @param loaderDelegate the delegate to handle loading classes
     */
    public RemoteExecutionControl(LoaderDelegate loaderDelegate) {
        super(loaderDelegate);
    }

    /**
     * Create an instance using the default class loading.
     */
    public RemoteExecutionControl() {
    }

    /**
     * Redefine processing on the remote end is only to register the redefined classes
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        classesRedefined(cbcs);
    }

    @Override
    public void stop() throws EngineTerminationException, InternalException {
        // handled by JDI
    }

    // Overridden only so this stack frame is seen
    @Override
    protected String invoke(Method doitMethod) throws Exception {
        return super.invoke(doitMethod);
    }

    // Overridden only so this stack frame is seen
    @Override
    public String varValue(String className, String varName) throws RunException, EngineTerminationException, InternalException {
        return super.varValue(className, varName);
    }

    @Override
    protected String throwConvertedInvocationException(Throwable cause) throws RunException, InternalException {
        if (cause instanceof StopExecutionException) {
            expectingStop = false;
            throw new StoppedException();
        } else {
            return super.throwConvertedInvocationException(cause);
        }
    }

    @Override
    protected String throwConvertedOtherException(Throwable ex) throws RunException, InternalException {
        if (ex instanceof StopExecutionException ||
                 ex.getCause() instanceof StopExecutionException) {
            expectingStop = false;
            throw new StoppedException();
        }
        return super.throwConvertedOtherException(ex);
    }

    @Override
    protected void clientCodeEnter() {
        expectingStop = false;
        inClientCode = true;
    }

    @Override
    protected void clientCodeLeave() throws InternalException {
        inClientCode = false;
        while (expectingStop) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException ex) {
                throw new InternalException("*** Sleep interrupted while waiting for stop exception: " + ex);
            }
        }
    }

    @SuppressWarnings("serial")             // serialVersionUID intentionally omitted
    private class StopExecutionException extends ThreadDeath {

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

}
