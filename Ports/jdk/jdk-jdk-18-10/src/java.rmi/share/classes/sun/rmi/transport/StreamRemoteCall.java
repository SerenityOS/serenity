/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputFilter;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.rmi.RemoteException;
import java.rmi.MarshalException;
import java.rmi.UnmarshalException;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteCall;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.rmi.runtime.Log;
import sun.rmi.server.UnicastRef;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * Stream-based implementation of the RemoteCall interface.
 *
 * @author Ann Wollrath
 */
@SuppressWarnings("deprecation")
public class StreamRemoteCall implements RemoteCall {
    private ConnectionInputStream in = null;
    private ConnectionOutputStream out = null;
    private Connection conn;
    private ObjectInputFilter filter = null;
    private boolean resultStarted = false;
    private Exception serverException = null;

    public StreamRemoteCall(Connection c) {
        conn = c;
    }

    public StreamRemoteCall(Connection c, ObjID id, int op, long hash)
        throws RemoteException
    {
        try {
            conn = c;
            Transport.transportLog.log(Log.VERBOSE,
                "write remote call header...");

            // write out remote call header info...
            // call header, part 1 (read by Transport)
            conn.getOutputStream().write(TransportConstants.Call);
            getOutputStream();           // creates a MarshalOutputStream
            id.write(out);               // object id (target of call)
            // call header, part 2 (read by Dispatcher)
            out.writeInt(op);            // method number (operation index)
            out.writeLong(hash);         // stub/skeleton hash
        } catch (IOException e) {
            throw new MarshalException("Error marshaling call header", e);
        }
    }

    /**
     * Return the connection associated with this call.
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * Return the output stream the stub/skeleton should put arguments/results
     * into.
     */
    public ObjectOutput getOutputStream() throws IOException {
        return getOutputStream(false);
    }

    private ObjectOutput getOutputStream(boolean resultStream)
        throws IOException
    {
        if (out == null) {
            Transport.transportLog.log(Log.VERBOSE, "getting output stream");

            out = new ConnectionOutputStream(conn, resultStream);
        }
        return out;
    }

    /**
     * Release the outputStream  Currently, will not complain if the
     * output stream is released more than once.
     */
    public void releaseOutputStream() throws IOException {
        try {
            if (out != null) {
                try {
                    out.flush();
                } finally {
                    out.done();         // always start DGC ack timer
                }
            }
            conn.releaseOutputStream();
        } finally {
            out = null;
        }
    }

    public void setObjectInputFilter(ObjectInputFilter filter) {
        if (in != null) {
            throw new IllegalStateException("set filter must occur before calling getInputStream");
        }
        this.filter = filter;
    }

    /**
     * Get the InputStream the stub/skeleton should get results/arguments
     * from.
     */
    @SuppressWarnings("removal")
    public ObjectInput getInputStream() throws IOException {
        if (in == null) {
            Transport.transportLog.log(Log.VERBOSE, "getting input stream");

            in = new ConnectionInputStream(conn.getInputStream());
            if (filter != null) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    in.setObjectInputFilter(filter);
                    return null;
                });
            }
        }
        return in;
    }

    /**
     * Release the input stream, this would allow some transports to release
     * the channel early.
     */
    public void releaseInputStream() throws IOException {
        /* WARNING: Currently, the UnicastRef.java invoke methods rely
         * upon this method not throwing an IOException.
         */

        try {
            if (in != null) {
                // execute MarshalInputStream "done" callbacks
                try {
                    in.done();
                } catch (RuntimeException e) {
                }

                // add saved references to DGC table
                in.registerRefs();

                /* WARNING: The connection being passed to done may have
                 * already been freed.
                 */
                in.done(conn);
            }
            conn.releaseInputStream();
        } finally {
            in = null;
        }
    }

    /**
     * Discard any post-processing of refs the InputStream.
     */
    public void discardPendingRefs() {
        in.discardRefs();
    }

    /**
     * Returns an output stream (may put out header information
     * relating to the success of the call).
     * @param success If true, indicates normal return, else indicates
     * exceptional return.
     * @exception StreamCorruptedException If result stream previously
     * acquired
     * @exception IOException For any other problem with I/O.
     */
    public ObjectOutput getResultStream(boolean success) throws IOException {
        /* make sure result code only marshaled once. */
        if (resultStarted)
            throw new StreamCorruptedException("result already in progress");
        else
            resultStarted = true;

        // write out return header
        // return header, part 1 (read by Transport)
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeByte(TransportConstants.Return);// transport op
        getOutputStream(true);  // creates a MarshalOutputStream
        // return header, part 2 (read by client-side RemoteCall)
        if (success)            //
            out.writeByte(TransportConstants.NormalReturn);
        else
            out.writeByte(TransportConstants.ExceptionalReturn);
        out.writeID();          // write id for gcAck
        return out;
    }

    /**
     * Do whatever it takes to execute the call.
     */
    @SuppressWarnings("fallthrough")
    public void executeCall() throws Exception {
        byte returnType;

        // read result header
        DGCAckHandler ackHandler = null;
        try {
            if (out != null) {
                ackHandler = out.getDGCAckHandler();
            }
            releaseOutputStream();
            DataInputStream rd = new DataInputStream(conn.getInputStream());
            byte op = rd.readByte();
            if (op != TransportConstants.Return) {
                if (Transport.transportLog.isLoggable(Log.BRIEF)) {
                    Transport.transportLog.log(Log.BRIEF,
                        "transport return code invalid: " + op);
                }
                throw new UnmarshalException("Transport return code invalid");
            }
            getInputStream();
            returnType = in.readByte();
            in.readID();        // id for DGC acknowledgement
        } catch (UnmarshalException e) {
            throw e;
        } catch (IOException e) {
            throw new UnmarshalException("Error unmarshaling return header",
                                         e);
        } finally {
            if (ackHandler != null) {
                ackHandler.release();
            }
        }

        // read return value
        switch (returnType) {
        case TransportConstants.NormalReturn:
            break;

        case TransportConstants.ExceptionalReturn:
            Object ex;
            try {
                ex = in.readObject();
            } catch (Exception e) {
                discardPendingRefs();
                throw new UnmarshalException("Error unmarshaling return", e);
            }

            // An exception should have been received,
            // if so throw it, else flag error
            if (ex instanceof Exception) {
                exceptionReceivedFromServer((Exception) ex);
            } else {
                discardPendingRefs();
                throw new UnmarshalException("Return type not Exception");
            }
            // Exception is thrown before fallthrough can occur
        default:
            if (Transport.transportLog.isLoggable(Log.BRIEF)) {
                Transport.transportLog.log(Log.BRIEF,
                    "return code invalid: " + returnType);
            }
            throw new UnmarshalException("Return code invalid");
        }
    }

    /**
     * Routine that causes the stack traces of remote exceptions to be
     * filled in with the current stack trace on the client.  Detail
     * exceptions are filled in iteratively.
     */
    protected void exceptionReceivedFromServer(Exception ex) throws Exception {
        serverException = ex;

        StackTraceElement[] serverTrace = ex.getStackTrace();
        StackTraceElement[] clientTrace = (new Throwable()).getStackTrace();
        StackTraceElement[] combinedTrace =
            new StackTraceElement[serverTrace.length + clientTrace.length];
        System.arraycopy(serverTrace, 0, combinedTrace, 0,
                         serverTrace.length);
        System.arraycopy(clientTrace, 0, combinedTrace, serverTrace.length,
                         clientTrace.length);
        ex.setStackTrace(combinedTrace);

        /*
         * Log the details of a server exception thrown as a result of a
         * remote method invocation.
         */
        if (UnicastRef.clientCallLog.isLoggable(Log.BRIEF)) {
            /* log call exception returned from server before it is rethrown */
            TCPEndpoint ep = (TCPEndpoint) conn.getChannel().getEndpoint();
            UnicastRef.clientCallLog.log(Log.BRIEF, "outbound call " +
                "received exception: [" + ep.getHost() + ":" +
                ep.getPort() + "] exception: ", ex);
        }

        throw ex;
    }

    /*
     * method to retrieve possible server side exceptions (which will
     * be throw from exceptionReceivedFromServer(...) )
     */
    public Exception getServerException() {
        return serverException;
    }

    public void done() throws IOException {
        /* WARNING: Currently, the UnicastRef.java invoke methods rely
         * upon this method not throwing an IOException.
         */

        releaseInputStream();
    }
}
