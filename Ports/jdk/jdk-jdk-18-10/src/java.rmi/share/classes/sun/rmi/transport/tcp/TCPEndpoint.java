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
package sun.rmi.transport.tcp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;
import sun.rmi.transport.Channel;
import sun.rmi.transport.Endpoint;
import sun.rmi.transport.Target;
import sun.rmi.transport.Transport;

/**
 * TCPEndpoint represents some communication endpoint for an address
 * space (VM).
 *
 * @author Ann Wollrath
 */
public class TCPEndpoint implements Endpoint {
    /** IP address or host name */
    private String host;
    /** port number */
    private int port;
    /** custom client socket factory (null if not custom factory) */
    private final RMIClientSocketFactory csf;
    /** custom server socket factory (null if not custom factory) */
    private final RMIServerSocketFactory ssf;

    /** if local, the port number to listen on */
    private int listenPort = -1;
    /** if local, the transport object associated with this endpoint */
    private TCPTransport transport = null;

    /** the local host name */
    private static String localHost;
    /** true if real local host name is known yet */
    private static boolean localHostKnown;

    // this should be a *private* method since it is privileged
    @SuppressWarnings("removal")
    private static int getInt(String name, int def) {
        return AccessController.doPrivileged(
                (PrivilegedAction<Integer>) () -> Integer.getInteger(name, def));
    }

    // this should be a *private* method since it is privileged
    @SuppressWarnings("removal")
    private static boolean getBoolean(String name) {
        return AccessController.doPrivileged(
                (PrivilegedAction<Boolean>) () -> Boolean.getBoolean(name));
    }

    /**
     * Returns the value of the java.rmi.server.hostname property.
     */
    @SuppressWarnings("removal")
    private static String getHostnameProperty() {
        return AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("java.rmi.server.hostname"));
    }

    /**
     * Find host name of local machine.  Property "java.rmi.server.hostname"
     * is used if set, so server administrator can compensate for the possible
     * inablility to get fully qualified host name from VM.
     */
    static {
        localHostKnown = true;
        localHost = getHostnameProperty();

        // could try querying CGI program here?
        if (localHost == null) {
            try {
                InetAddress localAddr = InetAddress.getLocalHost();
                byte[] raw = localAddr.getAddress();
                if ((raw[0] == 127) &&
                    (raw[1] ==   0) &&
                    (raw[2] ==   0) &&
                    (raw[3] ==   1)) {
                    localHostKnown = false;
                }

                /* if the user wishes to use a fully qualified domain
                 * name then attempt to find one.
                 */
                if (getBoolean("java.rmi.server.useLocalHostName")) {
                    localHost = FQDN.attemptFQDN(localAddr);
                } else {
                    /* default to using ip addresses, names will
                     * work across seperate domains.
                     */
                    localHost = localAddr.getHostAddress();
                }
            } catch (Exception e) {
                localHostKnown = false;
                localHost = null;
            }
        }

        if (TCPTransport.tcpLog.isLoggable(Log.BRIEF)) {
            TCPTransport.tcpLog.log(Log.BRIEF,
                "localHostKnown = " + localHostKnown +
                ", localHost = " + localHost);
        }
    }

    /** maps an endpoint key containing custom socket factories to
     * their own unique endpoint */
    // TBD: should this be a weak hash table?
    private static final
        Map<TCPEndpoint,LinkedList<TCPEndpoint>> localEndpoints =
        new HashMap<>();

    /**
     * Create an endpoint for a specified host and port.
     * This should not be used by external classes to create endpoints
     * for servers in this VM; use getLocalEndpoint instead.
     */
    public TCPEndpoint(String host, int port) {
        this(host, port, null, null);
    }

    /**
     * Create a custom socket factory endpoint for a specified host and port.
     * This should not be used by external classes to create endpoints
     * for servers in this VM; use getLocalEndpoint instead.
     */
    public TCPEndpoint(String host, int port, RMIClientSocketFactory csf,
                       RMIServerSocketFactory ssf)
    {
        if (host == null)
            host = "";
        this.host = host;
        this.port = port;
        this.csf = csf;
        this.ssf = ssf;
    }

    /**
     * Get an endpoint for the local address space on specified port.
     * If port number is 0, it returns shared default endpoint object
     * whose host name and port may or may not have been determined.
     */
    public static TCPEndpoint getLocalEndpoint(int port) {
        return getLocalEndpoint(port, null, null);
    }

    public static TCPEndpoint getLocalEndpoint(int port,
                                               RMIClientSocketFactory csf,
                                               RMIServerSocketFactory ssf)
    {
        /*
         * Find mapping for an endpoint key to the list of local unique
         * endpoints for this client/server socket factory pair (perhaps
         * null) for the specific port.
         */
        TCPEndpoint ep = null;

        synchronized (localEndpoints) {
            TCPEndpoint endpointKey = new TCPEndpoint(null, port, csf, ssf);
            LinkedList<TCPEndpoint> epList = localEndpoints.get(endpointKey);
            String localHost = resampleLocalHost();

            if (epList == null) {
                /*
                 * Create new endpoint list.
                 */
                ep = new TCPEndpoint(localHost, port, csf, ssf);
                epList = new LinkedList<TCPEndpoint>();
                epList.add(ep);
                ep.listenPort = port;
                ep.transport = new TCPTransport(epList);
                localEndpoints.put(endpointKey, epList);

                if (TCPTransport.tcpLog.isLoggable(Log.BRIEF)) {
                    TCPTransport.tcpLog.log(Log.BRIEF,
                        "created local endpoint for socket factory " + ssf +
                        " on port " + port);
                }
            } else {
                synchronized (epList) {
                    ep = epList.getLast();
                    String lastHost = ep.host;
                    int lastPort =  ep.port;
                    TCPTransport lastTransport = ep.transport;
                    // assert (localHost == null ^ lastHost != null)
                    if (localHost != null && !localHost.equals(lastHost)) {
                        /*
                         * Hostname has been updated; add updated endpoint
                         * to list.
                         */
                        if (lastPort != 0) {
                            /*
                             * Remove outdated endpoints only if the
                             * port has already been set on those endpoints.
                             */
                            epList.clear();
                        }
                        ep = new TCPEndpoint(localHost, lastPort, csf, ssf);
                        ep.listenPort = port;
                        ep.transport = lastTransport;
                        epList.add(ep);
                    }
                }
            }
        }

        return ep;
    }

    /**
     * Resamples the local hostname and returns the possibly-updated
     * local hostname.
     */
    private static String resampleLocalHost() {

        String hostnameProperty = getHostnameProperty();

        synchronized (localEndpoints) {
            // assert(localHostKnown ^ (localHost == null))

            if (hostnameProperty != null) {
                if (!localHostKnown) {
                    /*
                     * If the local hostname is unknown, update ALL
                     * existing endpoints with the new hostname.
                     */
                    setLocalHost(hostnameProperty);
                } else if (!hostnameProperty.equals(localHost)) {
                    /*
                     * Only update the localHost field for reference
                     * in future endpoint creation.
                     */
                    localHost = hostnameProperty;

                    if (TCPTransport.tcpLog.isLoggable(Log.BRIEF)) {
                        TCPTransport.tcpLog.log(Log.BRIEF,
                            "updated local hostname to: " + localHost);
                    }
                }
            }
            return localHost;
        }
    }

    /**
     * Set the local host name, if currently unknown.
     */
    static void setLocalHost(String host) {
        // assert (host != null)

        synchronized (localEndpoints) {
            /*
             * If host is not known, change the host field of ALL
             * the local endpoints.
             */
            if (!localHostKnown) {
                localHost = host;
                localHostKnown = true;

                if (TCPTransport.tcpLog.isLoggable(Log.BRIEF)) {
                    TCPTransport.tcpLog.log(Log.BRIEF,
                        "local host set to " + host);
                }
                for (LinkedList<TCPEndpoint> epList : localEndpoints.values())
                {
                    synchronized (epList) {
                        for (TCPEndpoint ep : epList) {
                            ep.host = host;
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the port of the (shared) default endpoint object.
     * When first created, it contains port 0 because the transport
     * hasn't tried to listen to get assigned a port, or if listening
     * failed, a port hasn't been assigned from the server.
     */
    static void setDefaultPort(int port, RMIClientSocketFactory csf,
                               RMIServerSocketFactory ssf)
    {
        TCPEndpoint endpointKey = new TCPEndpoint(null, 0, csf, ssf);

        synchronized (localEndpoints) {
            LinkedList<TCPEndpoint> epList = localEndpoints.get(endpointKey);

            synchronized (epList) {
                int size = epList.size();
                TCPEndpoint lastEp = epList.getLast();

                for (TCPEndpoint ep : epList) {
                    ep.port = port;
                }
                if (size > 1) {
                    /*
                     * Remove all but the last element of the list
                     * (which contains the most recent hostname).
                     */
                    epList.clear();
                    epList.add(lastEp);
                }
            }

            /*
             * Allow future exports to use the actual bound port
             * explicitly (see 6269166).
             */
            TCPEndpoint newEndpointKey = new TCPEndpoint(null, port, csf, ssf);
            localEndpoints.put(newEndpointKey, epList);

            if (TCPTransport.tcpLog.isLoggable(Log.BRIEF)) {
                TCPTransport.tcpLog.log(Log.BRIEF,
                    "default port for server socket factory " + ssf +
                    " and client socket factory " + csf +
                    " set to " + port);
            }
        }
    }

    /**
     * Returns transport for making connections to remote endpoints;
     * (here, the default transport at port 0 is used).
     */
    public Transport getOutboundTransport() {
        TCPEndpoint localEndpoint = getLocalEndpoint(0, null, null);
        return localEndpoint.transport;
    }

    /**
     * Returns the current list of known transports.
     * The returned list is an unshared collection of Transports,
     * including all transports which may have channels to remote
     * endpoints.
     */
    private static Collection<TCPTransport> allKnownTransports() {
        // Loop through local endpoints, getting the transport of each one.
        Set<TCPTransport> s;
        synchronized (localEndpoints) {
            // presize s to number of localEndpoints
            s = new HashSet<TCPTransport>(localEndpoints.size());
            for (LinkedList<TCPEndpoint> epList : localEndpoints.values()) {
                /*
                 * Each local endpoint has its transport added to s.
                 * Note: the transport is the same for all endpoints
                 * in the list, so it is okay to pick any one of them.
                 */
                TCPEndpoint ep = epList.getFirst();
                s.add(ep.transport);
            }
        }
        return s;
    }

    /**
     * Release idle outbound connections to reduce demand on I/O resources.
     * All transports are asked to release excess connections.
     */
    public static void shedConnectionCaches() {
        for (TCPTransport transport : allKnownTransports()) {
            transport.shedConnectionCaches();
        }
    }

    /**
     * Export the object to accept incoming calls.
     */
    public void exportObject(Target target) throws RemoteException {
        transport.exportObject(target);
    }

    /**
     * Returns a channel for this (remote) endpoint.
     */
    public Channel getChannel() {
        return getOutboundTransport().getChannel(this);
    }

    /**
     * Returns address for endpoint
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port for this endpoint.  If this endpoint was
     * created as a server endpoint (using getLocalEndpoint) for a
     * default/anonymous port and its inbound transport has started
     * listening, this method returns (instead of zero) the actual
     * bound port suitable for passing to clients.
     **/
    public int getPort() {
        return port;
    }

    /**
     * Returns the port that this endpoint's inbound transport listens
     * on, if this endpoint was created as a server endpoint (using
     * getLocalEndpoint).  If this endpoint was created for the
     * default/anonymous port, then this method returns zero even if
     * the transport has started listening.
     **/
    public int getListenPort() {
        return listenPort;
    }

    /**
     * Returns the transport for incoming connections to this
     * endpoint, if this endpoint was created as a server endpoint
     * (using getLocalEndpoint).
     **/
    public Transport getInboundTransport() {
        return transport;
    }

    /**
     * Get the client socket factory associated with this endpoint.
     */
    public RMIClientSocketFactory getClientSocketFactory() {
        return csf;
    }

    /**
     * Get the server socket factory associated with this endpoint.
     */
    public RMIServerSocketFactory getServerSocketFactory() {
        return ssf;
    }

    /**
     * Return string representation for endpoint.
     */
    public String toString() {
        return "[" + host + ":" + port +
            (ssf != null ? "," + ssf : "") +
            (csf != null ? "," + csf : "") +
            "]";
    }

    public int hashCode() {
        return port;
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof TCPEndpoint)) {
            TCPEndpoint ep = (TCPEndpoint) obj;
            if (port != ep.port || !host.equals(ep.host))
                return false;
            if (((csf == null) ^ (ep.csf == null)) ||
                ((ssf == null) ^ (ep.ssf == null)))
                return false;
            /*
             * Fix for 4254510: perform socket factory *class* equality check
             * before socket factory equality check to avoid passing
             * a potentially naughty socket factory to this endpoint's
             * {client,server} socket factory equals method.
             */
            if ((csf != null) &&
                !(csf.getClass() == ep.csf.getClass() && csf.equals(ep.csf)))
                return false;
            if ((ssf != null) &&
                !(ssf.getClass() == ep.ssf.getClass() && ssf.equals(ep.ssf)))
                return false;
            return true;
        } else {
            return false;
        }
    }

    /* codes for the self-describing formats of wire representation */
    private static final int FORMAT_HOST_PORT           = 0;
    private static final int FORMAT_HOST_PORT_FACTORY   = 1;

    /**
     * Write endpoint to output stream.
     */
    public void write(ObjectOutput out) throws IOException {
        if (csf == null) {
            out.writeByte(FORMAT_HOST_PORT);
            out.writeUTF(host);
            out.writeInt(port);
        } else {
            out.writeByte(FORMAT_HOST_PORT_FACTORY);
            out.writeUTF(host);
            out.writeInt(port);
            out.writeObject(csf);
        }
    }

    /**
     * Get the endpoint from the input stream.
     * @param in the input stream
     * @exception IOException If id could not be read (due to stream failure)
     */
    public static TCPEndpoint read(ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        String host;
        int port;
        RMIClientSocketFactory csf = null;

        byte format = in.readByte();
        switch (format) {
          case FORMAT_HOST_PORT:
            host = in.readUTF();
            port = in.readInt();
            break;

          case FORMAT_HOST_PORT_FACTORY:
            host = in.readUTF();
            port = in.readInt();
            csf = (RMIClientSocketFactory) in.readObject();
            if (csf != null && Proxy.isProxyClass(csf.getClass())) {
                throw new IOException("Invalid SocketFactory");
            }
          break;

          default:
            throw new IOException("invalid endpoint format");
        }
        return new TCPEndpoint(host, port, csf, null);
    }

    /**
     * Write endpoint to output stream in older format used by
     * UnicastRef for JDK1.1 compatibility.
     */
    public void writeHostPortFormat(DataOutput out) throws IOException {
        if (csf != null) {
            throw new InternalError("TCPEndpoint.writeHostPortFormat: " +
                "called for endpoint with non-null socket factory");
        }
        out.writeUTF(host);
        out.writeInt(port);
    }

    /**
     * Create a new endpoint from input stream data.
     * @param in the input stream
     */
    public static TCPEndpoint readHostPortFormat(DataInput in)
        throws IOException
    {
        String host = in.readUTF();
        int port = in.readInt();
        return new TCPEndpoint(host, port);
    }

    private static RMISocketFactory chooseFactory() {
        RMISocketFactory sf = RMISocketFactory.getSocketFactory();
        if (sf == null) {
            sf = TCPTransport.defaultSocketFactory;
        }
        return sf;
    }

    /**
     * Open and return new client socket connection to endpoint.
     */
    Socket newSocket() throws RemoteException {
        if (TCPTransport.tcpLog.isLoggable(Log.VERBOSE)) {
            TCPTransport.tcpLog.log(Log.VERBOSE,
                "opening socket to " + this);
        }

        Socket socket;

        try {
            RMIClientSocketFactory clientFactory = csf;
            if (clientFactory == null) {
                clientFactory = chooseFactory();
            }
            socket = clientFactory.createSocket(host, port);

        } catch (java.net.UnknownHostException e) {
            throw new java.rmi.UnknownHostException(
                "Unknown host: " + host, e);
        } catch (java.net.ConnectException e) {
            throw new java.rmi.ConnectException(
                "Connection refused to host: " + host, e);
        } catch (IOException e) {
            // We might have simply run out of file descriptors
            try {
                TCPEndpoint.shedConnectionCaches();
                // REMIND: should we retry createSocket?
            } catch (OutOfMemoryError | Exception mem) {
                // don't quit if out of memory
                // or shed fails non-catastrophically
            }

            throw new ConnectIOException("Exception creating connection to: " +
                host, e);
        }

        // set socket to disable Nagle's algorithm (always send immediately)
        // TBD: should this be left up to socket factory instead?
        try {
            socket.setTcpNoDelay(true);
        } catch (Exception e) {
            // if we fail to set this, ignore and proceed anyway
        }

        // fix 4187495: explicitly set SO_KEEPALIVE to prevent client hangs
        try {
            socket.setKeepAlive(true);
        } catch (Exception e) {
            // ignore and proceed
        }

        return socket;
    }

    /**
     * Return new server socket to listen for connections on this endpoint.
     */
    ServerSocket newServerSocket() throws IOException {
        if (TCPTransport.tcpLog.isLoggable(Log.VERBOSE)) {
            TCPTransport.tcpLog.log(Log.VERBOSE,
                "creating server socket on " + this);
        }

        RMIServerSocketFactory serverFactory = ssf;
        if (serverFactory == null) {
            serverFactory = chooseFactory();
        }
        ServerSocket server = serverFactory.createServerSocket(listenPort);

        // if we listened on an anonymous port, set the default port
        // (for this socket factory)
        if (listenPort == 0)
            setDefaultPort(server.getLocalPort(), csf, ssf);

        return server;
    }

    /**
     * The class FQDN encapsulates a routine that makes a best effort
     * attempt to retrieve the fully qualified domain name of the local
     * host.
     *
     * @author  Laird Dornin
     */
    private static class FQDN implements Runnable {

        /**
         * strings in which we can store discovered fqdn
         */
        private String reverseLookup;

        private String hostAddress;

        private FQDN(String hostAddress) {
            this.hostAddress = hostAddress;
        }

        /**
         * Do our best to obtain a fully qualified hostname for the local
         * host.  Perform the following steps to get a localhostname:
         *
         * 1. InetAddress.getLocalHost().getHostName() - if contains
         *    '.' use as FQDN
         * 2. if no '.' query name service for FQDN in a thread
         *    Note: We query the name service for an FQDN by creating
         *    an InetAddress via a stringified copy of the local ip
         *    address; this creates an InetAddress with a null hostname.
         *    Asking for the hostname of this InetAddress causes a name
         *    service lookup.
         *
         * 3. if name service takes too long to return, use ip address
         * 4. if name service returns but response contains no '.'
         *    default to ipaddress.
         */
        static String attemptFQDN(InetAddress localAddr)
            throws java.net.UnknownHostException
        {

            String hostName = localAddr.getHostName();

            if (hostName.indexOf('.') < 0 ) {

                String hostAddress = localAddr.getHostAddress();
                FQDN f = new FQDN(hostAddress);

                int nameServiceTimeOut =
                    TCPEndpoint.getInt("sun.rmi.transport.tcp.localHostNameTimeOut",
                                       10000);

                try {
                    synchronized(f) {
                        f.getFQDN();

                        /* wait to obtain an FQDN */
                        f.wait(nameServiceTimeOut);
                    }
                } catch (InterruptedException e) {
                    /* propagate the exception to the caller */
                    Thread.currentThread().interrupt();
                }
                hostName = f.getHost();

                if ((hostName == null) || (hostName.isEmpty())
                    || (hostName.indexOf('.') < 0 )) {

                    hostName = hostAddress;
                }
            }
            return hostName;
        }

        /**
         * Method that that will start a thread to wait to retrieve a
         * fully qualified domain name from a name service.  The spawned
         * thread may never return but we have marked it as a daemon so the vm
         * will terminate appropriately.
         */
        private void getFQDN() {

            /* FQDN finder will run in RMI threadgroup. */
            @SuppressWarnings("removal")
            Thread t = AccessController.doPrivileged(
                new NewThreadAction(FQDN.this, "FQDN Finder", true));
            t.start();
        }

        private synchronized String getHost() {
            return reverseLookup;
        }

        /**
         * thread to query a name service for the fqdn of this host.
         */
        public void run()  {

            String name = null;

            try {
                name = InetAddress.getByName(hostAddress).getHostName();
            } catch (java.net.UnknownHostException e) {
            } finally {
                synchronized(this) {
                    reverseLookup = name;
                    this.notify();
                }
            }
        }
    }
}
