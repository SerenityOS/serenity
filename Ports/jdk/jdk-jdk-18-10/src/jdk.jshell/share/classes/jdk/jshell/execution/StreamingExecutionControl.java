/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import jdk.jshell.JShellException;
import jdk.jshell.spi.ExecutionControl;
import static jdk.jshell.execution.ExecutionControlForwarder.NULL_MARKER;
import static jdk.jshell.execution.RemoteCodes.*;

/**
 * An implementation of the {@link jdk.jshell.spi.ExecutionControl}
 * execution engine SPI which streams requests to a remote agent where
 * execution takes place.
 *
 * @author Robert Field
 * @since 9
 */
public class StreamingExecutionControl implements ExecutionControl {

    private final ObjectOutput out;
    private final ObjectInput in;

    /**
     * Creates an instance.
     *
     * @param out the output for commands
     * @param in the input for command responses
     */
    public StreamingExecutionControl(ObjectOutput out, ObjectInput in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            // Send a load command to the remote agent.
            writeCommand(CMD_LOAD);
            out.writeObject(cbcs);
            out.flush();
            // Retrieve and report results from the remote agent.
            readAndReportClassInstallResult();
        } catch (IOException ex) {
            throw new EngineTerminationException("Exception writing remote load: " + ex);
        }
    }

    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            // Send a load command to the remote agent.
            writeCommand(CMD_REDEFINE);
            out.writeObject(cbcs);
            out.flush();
            // Retrieve and report results from the remote agent.
            readAndReportClassInstallResult();
        } catch (IOException ex) {
            throw new EngineTerminationException("Exception writing remote redefine: " + ex);
        }
    }

    @Override
    public String invoke(String classname, String methodname)
            throws RunException, EngineTerminationException, InternalException {
        try {
            // Send the invoke command to the remote agent.
            writeCommand(CMD_INVOKE);
            out.writeUTF(classname);
            out.writeUTF(methodname);
            out.flush();
            // Retrieve and report results from the remote agent.
            readAndReportExecutionResult();
            String result = in.readUTF();
            return result;
        } catch (IOException ex) {
            throw new EngineTerminationException("Exception writing remote invoke: " + ex);
        }
    }

    @Override
    public String varValue(String classname, String varname)
            throws RunException, EngineTerminationException, InternalException {
        try {
            // Send the variable-value command to the remote agent.
            writeCommand(CMD_VAR_VALUE);
            out.writeUTF(classname);
            out.writeUTF(varname);
            out.flush();
            // Retrieve and report results from the remote agent.
            readAndReportExecutionResult();
            String result = in.readUTF();
            return result;
        } catch (IOException ex) {
            throw new EngineTerminationException("Exception writing remote varValue: " + ex);
        }
    }


    @Override
    public void addToClasspath(String path)
            throws EngineTerminationException, InternalException {
        try {
            // Send the classpath addition command to the remote agent.
            writeCommand(CMD_ADD_CLASSPATH);
            out.writeUTF(path);
            out.flush();
            // Retrieve and report results from the remote agent.
            readAndReportClassSimpleResult();
        } catch (IOException ex) {
            throw new EngineTerminationException("Exception writing remote add to classpath: " + ex);
        }
    }

    @Override
    public void stop()
            throws EngineTerminationException, InternalException {
        try {
            // Send the variable-value command to the remote agent.
            writeCommand(CMD_STOP);
            out.flush();
        } catch (IOException ex) {
            throw new EngineTerminationException("Exception writing remote stop: " + ex);
        }
    }

    @Override
    public Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(command);
            out.writeObject(arg);
            out.flush();
            // Retrieve and report results from the remote agent.
            readAndReportExecutionResult();
            Object result = in.readObject();
            return result;
        } catch (IOException | ClassNotFoundException ex) {
            throw new EngineTerminationException("Exception transmitting remote extensionCommand: "
                    + command + " -- " + ex);
        }
    }

    /**
     * Closes the execution engine. Send an exit command to the remote agent.
     */
    @Override
    public void close() {
        try {
            writeCommand(CMD_CLOSE);
            out.flush();
        } catch (IOException ex) {
            // ignore;
        }
    }

    private void writeCommand(String cmd) throws IOException {
        out.writeInt(COMMAND_PREFIX);
        out.writeUTF(cmd);
    }

    /**
     * Read a UTF or a null encoded as a null marker.
     * @return a string or null
     * @throws IOException passed through from readUTF()
     */
    private String readNullOrUTF() throws IOException {
        String s = in.readUTF();
        return s.equals(NULL_MARKER) ? null : s;
    }

    /**
     * Reports results from a remote agent command that does not expect
     * exceptions.
     */
    private void readAndReportClassSimpleResult() throws EngineTerminationException, InternalException {
        try {
            int status = in.readInt();
            switch (status) {
                case RESULT_SUCCESS:
                    return;
                case RESULT_NOT_IMPLEMENTED: {
                    String message = in.readUTF();
                    throw new NotImplementedException(message);
                }
                case RESULT_INTERNAL_PROBLEM: {
                    String message = in.readUTF();
                    throw new InternalException(message);
                }
                case RESULT_TERMINATED: {
                    String message = in.readUTF();
                    throw new EngineTerminationException(message);
                }
                default: {
                    throw new EngineTerminationException("Bad remote result code: " + status);
                }
            }
        } catch (IOException ex) {
            throw new EngineTerminationException(ex.toString());
        }
    }

    /**
     * Reports results from a remote agent command that does not expect
     * exceptions.
     */
    private void readAndReportClassInstallResult() throws ClassInstallException,
            NotImplementedException, EngineTerminationException {
        try {
            int status = in.readInt();
            switch (status) {
                case RESULT_SUCCESS:
                    return;
                case RESULT_NOT_IMPLEMENTED: {
                    String message = in.readUTF();
                    throw new NotImplementedException(message);
                }
                case RESULT_CLASS_INSTALL_EXCEPTION: {
                    String message = in.readUTF();
                    boolean[] loaded = (boolean[]) in.readObject();
                    throw new ClassInstallException(message, loaded);
                }
                case RESULT_TERMINATED: {
                    String message = in.readUTF();
                    throw new EngineTerminationException(message);
                }
                default: {
                    throw new EngineTerminationException("Bad remote result code: " + status);
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new EngineTerminationException(ex.toString());
        }
    }

    /**
     * Reports results from a remote agent command that expects runtime
     * exceptions.
     *
     * @return true if successful
     * @throws IOException if the connection has dropped
     * @throws JShellException {@link jdk.jshell.EvalException}, if a user
     * exception was encountered on invoke;
     * {@link jdk.jshell.UnresolvedReferenceException}, if an unresolved
     * reference was encountered
     * @throws java.lang.ClassNotFoundException
     */
    private void readAndReportExecutionResult() throws RunException,
            EngineTerminationException, InternalException {
        try {
            int status = in.readInt();
            switch (status) {
                case RESULT_SUCCESS:
                    return;
                case RESULT_NOT_IMPLEMENTED: {
                    String message = in.readUTF();
                    throw new NotImplementedException(message);
                }
                case RESULT_USER_EXCEPTION: {
                    // A user exception was encountered.  Handle pre JDK 11 back-ends
                    throw readUserException();
                }
                case RESULT_CORRALLED: {
                    // An unresolved reference was encountered.
                    throw readResolutionException();
                }
                case RESULT_USER_EXCEPTION_CHAINED: {
                    // A user exception was encountered -- transmit chained.
                    in.readInt(); // always RESULT_USER_EXCEPTION
                    UserException result = readUserException();
                    RunException caused = result;
                    // Loop through the chained causes (if any) building a chained exception
                    loop: while (true) {
                        RunException ex;
                        int cstatus = in.readInt();
                        switch (cstatus) {
                            case RESULT_USER_EXCEPTION: {
                                // A user exception was the proximal cause.
                                ex = readUserException();
                                break;
                            }
                            case RESULT_CORRALLED: {
                                // An unresolved reference was the underlying cause.
                                ex = readResolutionException();
                                break;
                            }
                            case RESULT_SUCCESS: {
                                // End of chained exceptions
                                break loop;
                            }
                            default: {
                                throw new EngineTerminationException("Bad chained remote result code: " + cstatus);
                            }
                        }
                        caused.initCause(ex);
                        caused = ex;
                    }
                    caused.initCause(null); // root cause has no cause
                    throw result;
                }
                case RESULT_STOPPED: {
                    // Execution was aborted by the stop()
                    throw new StoppedException();
                }
                case RESULT_INTERNAL_PROBLEM: {
                    // An internal error has occurred.
                    String message = in.readUTF();
                    throw new InternalException(message);
                }
                case RESULT_TERMINATED: {
                    String message = in.readUTF();
                    throw new EngineTerminationException(message);
                }
                default: {
                    throw new EngineTerminationException("Bad remote result code: " + status);
                }
            }
        } catch (EOFException ex) {
            throw new EngineTerminationException("Terminated.");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new EngineTerminationException(ex.toString());
        }
    }

    private UserException readUserException() throws IOException, ClassNotFoundException {
        String message = readNullOrUTF();
        String exceptionClassName = in.readUTF();
        StackTraceElement[] elems = (StackTraceElement[]) in.readObject();
        return new UserException(message, exceptionClassName, elems);
    }

    private ResolutionException readResolutionException() throws IOException, ClassNotFoundException {
        int id = in.readInt();
        StackTraceElement[] elems = (StackTraceElement[]) in.readObject();
        return new ResolutionException(id, elems);
    }
}
