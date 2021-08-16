/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.rmi.server.UID;
import sun.rmi.server.MarshalOutputStream;

/**
 * Special stream to keep track of refs being marshaled as return
 * results to determine whether a special ack needs to be sent
 * to the distributed collector.
 *
 * @author Ann Wollrath
 */
class ConnectionOutputStream extends MarshalOutputStream {

    /** connection associated with ConnectionOutputStream */
    private final Connection conn;
    /** indicates whether output stream is used to marshal results */
    private final boolean resultStream;
    /** identifier for gc ack*/
    private final UID ackID;

    /** to store refs to returned remote object until DGC ack is received */
    private DGCAckHandler dgcAckHandler = null;

    /**
     * Constructs an marshal output stream using the underlying
     * stream associated with the connection, the parameter c.
     * @param c is the Connection object associated with the
     * ConnectionOutputStream object being constructed
     * @param resultStream indicates whether this stream is used
     * to marshal return results
     */
    ConnectionOutputStream(Connection conn, boolean resultStream)
        throws IOException
    {
        super(conn.getOutputStream());
        this.conn = conn;
        this.resultStream = resultStream;
        ackID = resultStream ? new UID() : null;
    }

    void writeID() throws IOException {
        assert resultStream;
        ackID.write(this);
    }

    /**
     * Returns true if this output stream is used to marshal return
     * results; otherwise returns false.
     */
    boolean isResultStream() {
        return resultStream;
    }

    /**
     * Saves a reference to the specified object in this stream's
     * DGCAckHandler.
     **/
    void saveObject(Object obj) {
        // should always be accessed from same thread
        if (dgcAckHandler == null) {
            dgcAckHandler = new DGCAckHandler(ackID);
        }
        dgcAckHandler.add(obj);
    }

    /**
     * Returns this stream's DGCAckHandler, or null if it doesn't have
     * one (saveObject was not invoked).  This method should only be
     * invoked after all objects have been written to the stream,
     * because future objects written may yet cause a DGCAckHandler to
     * be created (by invoking saveObject).
     **/
    DGCAckHandler getDGCAckHandler() {
        return dgcAckHandler;
    }

    void done() {
        if (dgcAckHandler != null) {
            dgcAckHandler.startTimer();
        }
    }
}
