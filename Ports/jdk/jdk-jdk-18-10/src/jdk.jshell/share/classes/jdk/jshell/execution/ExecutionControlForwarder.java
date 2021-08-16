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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import jdk.jshell.spi.ExecutionControl.ResolutionException;
import jdk.jshell.spi.ExecutionControl.StoppedException;
import jdk.jshell.spi.ExecutionControl.UserException;
import static jdk.jshell.execution.RemoteCodes.*;

/**
 * Forwards commands from the input to the specified {@link ExecutionControl}
 * instance, then responses back on the output.
 */
class ExecutionControlForwarder {

    /**
     * Represent null in a streamed UTF string. Vanishingly improbable string to
     * occur in a user string.
     */
    static final String NULL_MARKER = "\u0002*\u03C0*NULL*\u03C0*\u0003";

    /**
     * Maximum number of characters for writeUTF().  Byte maximum is 65535, at
     * maximum three bytes per character that is 65535 / 3 == 21845.  Minus one
     * for safety.
     */
    private static final int MAX_UTF_CHARS = 21844;

    private final ExecutionControl ec;
    private final ObjectInput in;
    private final ObjectOutput out;

    ExecutionControlForwarder(ExecutionControl ec, ObjectInput in, ObjectOutput out) {
        this.ec = ec;
        this.in = in;
        this.out = out;
    }

    private boolean writeSuccess() throws IOException {
        writeStatus(RESULT_SUCCESS);
        flush();
        return true;
    }

    private boolean writeSuccessAndResult(String result) throws IOException {
        writeStatus(RESULT_SUCCESS);
        writeUTF(result);
        flush();
        return true;
    }

    private boolean writeSuccessAndResult(Object result) throws IOException {
        writeStatus(RESULT_SUCCESS);
        writeObject(result);
        flush();
        return true;
    }

    private void writeStatus(int status) throws IOException {
        out.writeInt(status);
    }

    private void writeObject(Object o) throws IOException {
        out.writeObject(o);
    }

    private void writeInt(int i) throws IOException {
        out.writeInt(i);
    }

    private void writeNullOrUTF(String s) throws IOException {
        writeUTF(s == null ? NULL_MARKER : s);
    }

    private void writeUTF(String s) throws IOException {
        if (s == null) {
            s = "";
        } else if (s.length() > MAX_UTF_CHARS) {
            // Truncate extremely long strings to prevent writeUTF from crashing the VM
            s = s.substring(0, MAX_UTF_CHARS);
        }
        out.writeUTF(s);
    }

    private void flush() throws IOException {
        out.flush();
    }

    private boolean processCommand() throws IOException {
        try {
            int prefix = in.readInt();
            if (prefix != COMMAND_PREFIX) {
                throw new EngineTerminationException("Invalid command prefix: " + prefix);
            }
            String cmd = in.readUTF();
            switch (cmd) {
                case CMD_LOAD: {
                    // Load a generated class file over the wire
                    ClassBytecodes[] cbcs = (ClassBytecodes[]) in.readObject();
                    ec.load(cbcs);
                    return writeSuccess();
                }
                case CMD_REDEFINE: {
                    // Load a generated class file over the wire
                    ClassBytecodes[] cbcs = (ClassBytecodes[]) in.readObject();
                    ec.redefine(cbcs);
                    return writeSuccess();
                }
                case CMD_INVOKE: {
                    // Invoke executable entry point in loaded code
                    String className = in.readUTF();
                    String methodName = in.readUTF();
                    String res = ec.invoke(className, methodName);
                    return writeSuccessAndResult(res);
                }
                case CMD_VAR_VALUE: {
                    // Retrieve a variable value
                    String className = in.readUTF();
                    String varName = in.readUTF();
                    String res = ec.varValue(className, varName);
                    return writeSuccessAndResult(res);
                }
                case CMD_ADD_CLASSPATH: {
                    // Append to the claspath
                    String cp = in.readUTF();
                    ec.addToClasspath(cp);
                    return writeSuccess();
                }
                case CMD_STOP: {
                    // Stop the current execution
                    try {
                        ec.stop();
                    } catch (Throwable ex) {
                        // JShell-core not waiting for a result, ignore
                    }
                    return true;
                }
                case CMD_CLOSE: {
                    // Terminate this process
                    try {
                        ec.close();
                    } catch (Throwable ex) {
                        // JShell-core not waiting for a result, ignore
                    }
                    return true;
                }
                default: {
                    Object arg = in.readObject();
                    Object res = ec.extensionCommand(cmd, arg);
                    return writeSuccessAndResult(res);
                }
            }
        } catch (IOException ex) {
            // handled by the outer level
            throw ex;
        } catch (EngineTerminationException ex) {
            writeStatus(RESULT_TERMINATED);
            writeUTF(ex.getMessage());
            flush();
            return false;
        } catch (NotImplementedException ex) {
            writeStatus(RESULT_NOT_IMPLEMENTED);
            writeUTF(ex.getMessage());
            flush();
            return true;
        } catch (InternalException ex) {
            writeInternalException(ex);
            flush();
            return true;
        } catch (ClassInstallException ex) {
            writeStatus(RESULT_CLASS_INSTALL_EXCEPTION);
            writeUTF(ex.getMessage());
            writeObject(ex.installed());
            flush();
            return true;
        } catch (UserException ex) {
            writeStatus(RESULT_USER_EXCEPTION_CHAINED);
            for (Throwable e = ex; e != null; ) {
                if (e instanceof UserException) {
                    writeUserException((UserException) e);
                    e = e.getCause();
                } else if (e instanceof ResolutionException) {
                    writeResolutionException((ResolutionException) e);
                    e = null;
                } else {
                    writeInternalException(e);
                    e = null;
                }
            }
            writeStatus(RESULT_SUCCESS);
            flush();
            return true;
        } catch (ResolutionException ex) {
            writeResolutionException(ex);
            flush();
            return true;
        } catch (StoppedException ex) {
            writeStatus(RESULT_STOPPED);
            flush();
            return true;
        } catch (Throwable ex) {
            // Unexpected exception, have something in the message
            writeStatus(RESULT_TERMINATED);
            String msg = ex.getMessage();
            writeUTF(msg == null? ex.toString() : msg);
            flush();
            return false;
        }
    }

    void writeInternalException(Throwable ex) throws IOException {
        writeStatus(RESULT_INTERNAL_PROBLEM);
        writeUTF(ex.getMessage());
    }

    void writeUserException(UserException ex) throws IOException {
        writeStatus(RESULT_USER_EXCEPTION);
        writeNullOrUTF(ex.getMessage());
        writeUTF(ex.causeExceptionClass());
        writeObject(ex.getStackTrace());
    }

    void writeResolutionException(ResolutionException ex) throws IOException {
        writeStatus(RESULT_CORRALLED);
        writeInt(ex.id());
        writeObject(ex.getStackTrace());
    }

    void commandLoop() {
        try {
            while (processCommand()) {
                // condition is loop action
            }
        } catch (IOException ex) {
            // drop out of loop
        }
    }

}
