/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import sun.rmi.server.MarshalInputStream;
import sun.rmi.runtime.Log;

/**
 * Special stream to keep track of refs being unmarshaled so that
 * refs can be ref-counted locally.
 *
 * @author Ann Wollrath
 */
class ConnectionInputStream extends MarshalInputStream {

    /** indicates whether ack is required for DGC */
    private boolean dgcAckNeeded = false;

    /** Hashtable mapping Endpoints to lists of LiveRefs to register */
    private Map<Endpoint, List<LiveRef>> incomingRefTable = new HashMap<>(5);

    /** identifier for gc ack*/
    private UID ackID;

    /**
     * Constructs a marshal input stream using the underlying
     * stream "in".
     */
    ConnectionInputStream(InputStream in) throws IOException {
        super(in);
    }

    void readID() throws IOException {
        ackID = UID.read((DataInput) this);
    }

    /**
     * Save reference in order to send "dirty" call after all args/returns
     * have been unmarshaled.  Save in hashtable incomingRefTable.  This
     * table is keyed on endpoints, and holds objects of type
     * IncomingRefTableEntry.
     */
    void saveRef(LiveRef ref) {
        Endpoint ep = ref.getEndpoint();

        // check whether endpoint is already in the hashtable
        List<LiveRef> refList = incomingRefTable.get(ep);

        if (refList == null) {
            refList = new ArrayList<LiveRef>();
            incomingRefTable.put(ep, refList);
        }

        // add ref to list of refs for endpoint ep
        refList.add(ref);
    }

    /**
     * Discard the saved incoming refs so there is nothing to register
     * when {@code registerRefs} is called.
     */
    void discardRefs() {
        incomingRefTable.clear();
    }

    /**
     * Add references to DGC table (and possibly send dirty call).
     * RegisterRefs now calls DGCClient.referenced on all
     * refs with the same endpoint at once to achieve batching of
     * calls to the DGC
     */
    void registerRefs() throws IOException {
        if (!incomingRefTable.isEmpty()) {
            for (Map.Entry<Endpoint, List<LiveRef>> entry :
                     incomingRefTable.entrySet()) {
                DGCClient.registerRefs(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Indicate that an ack is required to the distributed
     * collector.
     */
    void setAckNeeded() {
        dgcAckNeeded = true;
    }

    /**
     * Done with input stream for remote call. Send DGC ack if necessary.
     * Allow sending of ack to fail without flagging an error.
     */
    void done(Connection c) {
        /*
         * WARNING: The connection c may have already been freed.  It
         * is only be safe to use c to obtain c's channel.
         */

        if (dgcAckNeeded) {
            Connection conn = null;
            Channel ch = null;
            boolean reuse = true;

            DGCImpl.dgcLog.log(Log.VERBOSE, "send ack");

            try {
                ch = c.getChannel();
                conn = ch.newConnection();
                DataOutputStream out =
                    new DataOutputStream(conn.getOutputStream());
                out.writeByte(TransportConstants.DGCAck);
                if (ackID == null) {
                    ackID = new UID();
                }
                ackID.write((DataOutput) out);
                conn.releaseOutputStream();

                /*
                 * Fix for 4221173: if this connection is on top of an
                 * HttpSendSocket, the DGCAck won't actually get sent until a
                 * read operation is attempted on the socket.  Calling
                 * available() is the most innocuous way of triggering the
                 * write.
                 */
                conn.getInputStream().available();
                conn.releaseInputStream();
            } catch (RemoteException e) {
                reuse = false;
            } catch (IOException e) {
                reuse = false;
            }
            try {
                if (conn != null)
                    ch.free(conn, reuse);
            } catch (RemoteException e){
                // eat exception
            }
        }
    }
}
