/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Arrays;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * NOTE: There is a JDK-internal dependency on the existence of this
 * class and its getClientSocketFactory method in the implementation
 * of javax.management.remote.rmi.RMIConnector.
 **/
public class LiveRef implements Cloneable {
    /** wire representation for the object*/
    private final Endpoint ep;
    private final ObjID id;

    /** cached connection service for the object */
    private transient Channel ch;

    /** flag to indicate whether this ref specifies a local server or
     * is a ref for a remote object (surrogate)
     */
    private final boolean isLocal;

    /**
     * Construct a "well-known" live reference to a remote object
     * @param isLocal If true, indicates this ref specifies a local
     * server in this address space; if false, the ref is for a remote
     * object (hence a surrogate or proxy) in another address space.
     */
    public LiveRef(ObjID objID, Endpoint endpoint, boolean isLocal) {
        ep = endpoint;
        id = objID;
        this.isLocal = isLocal;
    }

    /**
     * Construct a new live reference for a server object in the local
     * address space.
     */
    public LiveRef(int port) {
        this((new ObjID()), port);
    }

    /**
     * Construct a new live reference for a server object in the local
     * address space, to use sockets of the specified type.
     */
    public LiveRef(int port,
                   RMIClientSocketFactory csf,
                   RMIServerSocketFactory ssf)
    {
        this((new ObjID()), port, csf, ssf);
    }

    /**
     * Construct a new live reference for a "well-known" server object
     * in the local address space.
     */
    public LiveRef(ObjID objID, int port) {
        this(objID, TCPEndpoint.getLocalEndpoint(port), true);
    }

    /**
     * Construct a new live reference for a "well-known" server object
     * in the local address space, to use sockets of the specified type.
     */
    public LiveRef(ObjID objID, int port, RMIClientSocketFactory csf,
                   RMIServerSocketFactory ssf)
    {
        this(objID, TCPEndpoint.getLocalEndpoint(port, csf, ssf), true);
    }

    /**
     * Return a shallow copy of this ref.
     */
    public Object clone() {
        try {
            LiveRef newRef = (LiveRef) super.clone();
            return newRef;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    /**
     * Return the port number associated with this ref.
     */
    public int getPort() {
        return ((TCPEndpoint) ep).getPort();
    }

    /**
     * Return the client socket factory associated with this ref.
     *
     * NOTE: There is a JDK-internal dependency on the existence of
     * this method in the implementation of
     * javax.management.remote.rmi.RMIConnector.
     **/
    public RMIClientSocketFactory getClientSocketFactory() {
        return ((TCPEndpoint) ep).getClientSocketFactory();
    }

    /**
     * Return the server socket factory associated with this ref.
     */
    public RMIServerSocketFactory getServerSocketFactory() {
        return ((TCPEndpoint) ep).getServerSocketFactory();
    }

    /**
     * Export the object to accept incoming calls.
     */
    public void exportObject(Target target) throws RemoteException {
        ep.exportObject(target);
    }

    public Channel getChannel() throws RemoteException {
        if (ch == null) {
            ch = ep.getChannel();
        }
        return ch;
    }

    public ObjID getObjID() {
        return id;
    }

    Endpoint getEndpoint() {
        return ep;
    }

    public String toString() {
        String type;

        if (isLocal)
            type = "local";
        else
            type = "remote";
        return "[endpoint:" + ep + "(" + type + ")," +
            "objID:" + id + "]";
    }

    public int hashCode() {
        return id.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj != null && obj instanceof LiveRef) {
            LiveRef ref = (LiveRef) obj;

            return (ep.equals(ref.ep) && id.equals(ref.id) &&
                    isLocal == ref.isLocal);
        } else {
            return false;
        }
    }

    public boolean remoteEquals(Object obj) {
        if (obj != null && obj instanceof LiveRef) {
            LiveRef ref = (LiveRef) obj;

            TCPEndpoint thisEp = ((TCPEndpoint) ep);
            TCPEndpoint refEp = ((TCPEndpoint) ref.ep);

            RMIClientSocketFactory thisClientFactory =
                thisEp.getClientSocketFactory();
            RMIClientSocketFactory refClientFactory =
                refEp.getClientSocketFactory();

            /**
             * Fix for 4254103: LiveRef.remoteEquals should not fail
             * if one of the objects in the comparison has a null
             * server socket.  Comparison should only consider the
             * following criteria:
             *
             * hosts, ports, client socket factories and object IDs.
             */
            if (thisEp.getPort() != refEp.getPort() ||
                !thisEp.getHost().equals(refEp.getHost()))
            {
                return false;
            }
            if ((thisClientFactory == null) ^ (refClientFactory == null)) {
                return false;
            }
            if ((thisClientFactory != null) &&
                !((thisClientFactory.getClass() ==
                   refClientFactory.getClass()) &&
                  (thisClientFactory.equals(refClientFactory))))
            {
                return false;
            }
            return (id.equals(ref.id));
        } else {
            return false;
        }
    }

    public void write(ObjectOutput out, boolean useNewFormat)
        throws IOException
    {
        boolean isResultStream = false;
        if (out instanceof ConnectionOutputStream) {
            ConnectionOutputStream stream = (ConnectionOutputStream) out;
            isResultStream = stream.isResultStream();
            /*
             * Ensure that referential integrity is not broken while
             * this LiveRef is in transit.  If it is being marshalled
             * as part of a result, it may not otherwise be strongly
             * reachable after the remote call has completed; even if
             * it is being marshalled as part of an argument, the VM
             * may determine that the reference on the stack is no
             * longer reachable after marshalling (see 6181943)--
             * therefore, tell the stream to save a reference until a
             * timeout expires or, for results, a DGCAck message has
             * been received from the caller, or for arguments, the
             * remote call has completed.  For a "local" LiveRef, save
             * a reference to the impl directly, because the impl is
             * not reachable from the LiveRef (see 4114579);
             * otherwise, save a reference to the LiveRef, for the
             * client-side DGC to watch over.  (Also see 4017232.)
             */
            if (isLocal) {
                ObjectEndpoint oe =
                    new ObjectEndpoint(id, ep.getInboundTransport());
                Target target = ObjectTable.getTarget(oe);

                if (target != null) {
                    Remote impl = target.getImpl();
                    if (impl != null) {
                        stream.saveObject(impl);
                    }
                }
            } else {
                stream.saveObject(this);
            }
        }
        // All together now write out the endpoint, id, and flag

        // (need to choose whether or not to use old JDK1.1 endpoint format)
        if (useNewFormat) {
            ((TCPEndpoint) ep).write(out);
        } else {
            ((TCPEndpoint) ep).writeHostPortFormat(out);
        }
        id.write(out);
        out.writeBoolean(isResultStream);
    }

    public static LiveRef read(ObjectInput in, boolean useNewFormat)
        throws IOException, ClassNotFoundException
    {
        Endpoint ep;
        ObjID id;

        // Now read in the endpoint, id, and result flag
        // (need to choose whether or not to read old JDK1.1 endpoint format)
        if (useNewFormat) {
            ep = TCPEndpoint.read(in);
        } else {
            ep = TCPEndpoint.readHostPortFormat(in);
        }
        id = ObjID.read(in);
        boolean isResultStream = in.readBoolean();

        LiveRef ref = new LiveRef(id, ep, false);

        if (in instanceof ConnectionInputStream) {
            ConnectionInputStream stream = (ConnectionInputStream)in;
            // save ref to send "dirty" call after all args/returns
            // have been unmarshaled.
            stream.saveRef(ref);
            if (isResultStream) {
                // set flag in stream indicating that remote objects were
                // unmarshaled.  A DGC ack should be sent by the transport.
                stream.setAckNeeded();
            }
        } else {
            DGCClient.registerRefs(ep, Arrays.asList(new LiveRef[] { ref }));
        }

        return ref;
    }
}
