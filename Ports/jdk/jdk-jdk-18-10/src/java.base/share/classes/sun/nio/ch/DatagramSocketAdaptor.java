/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A multicast datagram socket based on a datagram channel.
 *
 * This class overrides every public method defined by java.net.DatagramSocket
 * and java.net.MulticastSocket. The methods in this class are defined in exactly
 * the same order as in java.net.DatagramSocket and java.net.MulticastSocket so
 * as to simplify tracking changes.
 */
public class DatagramSocketAdaptor
    extends MulticastSocket
{
    // The channel being adapted
    private final DatagramChannelImpl dc;

    // Timeout "option" value for receives
    private volatile int timeout;

    private DatagramSocketAdaptor(DatagramChannelImpl dc) throws IOException {
        super(/*SocketAddress*/ DatagramSockets.NO_DELEGATE);
        this.dc = dc;
    }

    static DatagramSocket create(DatagramChannelImpl dc) {
        try {
            return new DatagramSocketAdaptor(dc);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void connectInternal(SocketAddress remote) throws SocketException {
        try {
            dc.connect(remote, false); // skips check for already connected
        } catch (ClosedChannelException e) {
            // ignore
        } catch (Exception x) {
            Net.translateToSocketException(x);
        }
    }

    @Override
    public void bind(SocketAddress local) throws SocketException {
        if (local != null) {
            local = Net.asInetSocketAddress(local);
        } else {
            local = new InetSocketAddress(0);
        }
        try {
            dc.bind(local);
        } catch (Exception x) {
            Net.translateToSocketException(x);
        }
    }

    @Override
    public void connect(InetAddress address, int port) {
        if (address == null)
            throw new IllegalArgumentException("Address can't be null");
        try {
            connectInternal(new InetSocketAddress(address, port));
        } catch (SocketException x) {
            throw new UncheckedIOException(x);
        }
    }

    @Override
    public void connect(SocketAddress remote) throws SocketException {
        if (remote == null)
            throw new IllegalArgumentException("Address can't be null");
        connectInternal(Net.asInetSocketAddress(remote));
    }

    @Override
    public void disconnect() {
        try {
            dc.disconnect();
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    @Override
    public boolean isBound() {
        return dc.localAddress() != null;
    }

    @Override
    public boolean isConnected() {
        return dc.remoteAddress() != null;
    }

    @Override
    public InetAddress getInetAddress() {
        InetSocketAddress remote = dc.remoteAddress();
        return (remote != null) ? remote.getAddress() : null;
    }

    @Override
    public int getPort() {
        InetSocketAddress remote = dc.remoteAddress();
        return (remote != null) ? remote.getPort() : -1;
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return dc.remoteAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        InetSocketAddress local = dc.localAddress();
        if (local == null || isClosed())
            return null;

        InetAddress addr = local.getAddress();
        if (addr.isAnyLocalAddress())
            return local;

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkConnect(addr.getHostAddress(), -1);
            } catch (SecurityException x) {
                return new InetSocketAddress(local.getPort());
            }
        }
        return local;
    }

    @Override
    public void send(DatagramPacket p) throws IOException {
        ByteBuffer bb = null;
        try {
            InetSocketAddress target;
            synchronized (p) {
                // copy bytes to temporary direct buffer
                int len = p.getLength();
                bb = Util.getTemporaryDirectBuffer(len);
                bb.put(p.getData(), p.getOffset(), len);
                bb.flip();

                // target address
                if (p.getAddress() == null) {
                    InetSocketAddress remote = dc.remoteAddress();
                    if (remote == null) {
                        // not specified by DatagramSocket
                        throw new IllegalArgumentException("Address not set");
                    }
                    // set address/port to maintain compatibility with DatagramSocket
                    p.setAddress(remote.getAddress());
                    p.setPort(remote.getPort());
                    target = remote;
                } else {
                    target = (InetSocketAddress) p.getSocketAddress();
                }
            }
            // send datagram
            try {
                dc.blockingSend(bb, target);
            } catch (AlreadyConnectedException e) {
                throw new IllegalArgumentException("Connected and packet address differ");
            } catch (ClosedChannelException e) {
                var exc = new SocketException("Socket closed");
                exc.initCause(e);
                throw exc;
            }
        } finally {
            if (bb != null) {
                Util.offerFirstTemporaryDirectBuffer(bb);
            }
        }
    }

    @Override
    public void receive(DatagramPacket p) throws IOException {
        // get temporary direct buffer with a capacity of p.bufLength
        int bufLength = DatagramPackets.getBufLength(p);
        ByteBuffer bb = Util.getTemporaryDirectBuffer(bufLength);
        try {
            long nanos = MILLISECONDS.toNanos(timeout);
            SocketAddress sender = dc.blockingReceive(bb, nanos);
            bb.flip();
            synchronized (p) {
                // copy bytes to the DatagramPacket and set length
                int len = Math.min(bb.limit(), DatagramPackets.getBufLength(p));
                bb.get(p.getData(), p.getOffset(), len);
                DatagramPackets.setLength(p, len);

                // sender address
                p.setSocketAddress(sender);
            }
        } catch (ClosedChannelException e) {
            var exc = new SocketException("Socket closed");
            exc.initCause(e);
            throw exc;
        } finally {
            Util.offerFirstTemporaryDirectBuffer(bb);
        }
    }

    @Override
    public InetAddress getLocalAddress() {
        if (isClosed())
            return null;
        InetSocketAddress local = dc.localAddress();
        if (local == null)
            local = new InetSocketAddress(0);
        InetAddress result = local.getAddress();
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkConnect(result.getHostAddress(), -1);
            } catch (SecurityException x) {
                return new InetSocketAddress(0).getAddress();
            }
        }
        return result;
    }

    @Override
    public int getLocalPort() {
        if (isClosed())
            return -1;
        InetSocketAddress local = dc.localAddress();
        if (local != null) {
            return local.getPort();
        }
        return 0;
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout < 0");
        this.timeout = timeout;
    }

    @Override
    public int getSoTimeout() throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        return timeout;
    }

    private void setBooleanOption(SocketOption<Boolean> name, boolean value)
        throws SocketException
    {
        try {
            dc.setOption(name, value);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    private void setIntOption(SocketOption<Integer> name, int value)
        throws SocketException
    {
        try {
            dc.setOption(name, value);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    private boolean getBooleanOption(SocketOption<Boolean> name) throws SocketException {
        try {
            return dc.getOption(name).booleanValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return false;       // keep compiler happy
        }
    }

    private int getIntOption(SocketOption<Integer> name) throws SocketException {
        try {
            return dc.getOption(name).intValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return -1;          // keep compiler happy
        }
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        if (size <= 0)
            throw new IllegalArgumentException("Invalid send size");
        setIntOption(StandardSocketOptions.SO_SNDBUF, size);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        if (size <= 0)
            throw new IllegalArgumentException("Invalid receive size");
        setIntOption(StandardSocketOptions.SO_RCVBUF, size);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
    }

    @Override
    public void setBroadcast(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_BROADCAST, on);
    }

    @Override
    public boolean getBroadcast() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_BROADCAST);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, tc);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }

    @Override
    public void close() {
        try {
            dc.close();
        } catch (IOException x) {
            throw new Error(x);
        }
    }

    @Override
    public boolean isClosed() {
        return !dc.isOpen();
    }

    @Override
    public DatagramChannel getChannel() {
        return dc;
    }

    @Override
    public <T> DatagramSocket setOption(SocketOption<T> name, T value) throws IOException {
        dc.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return dc.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return dc.supportedOptions();
    }

    // -- java.net.MulticastSocket --

    // used to coordinate changing TTL with the deprecated send method
    private final ReentrantLock sendLock = new ReentrantLock();

    // cached outgoing interface (for use by setInterface/getInterface)
    private final Object outgoingInterfaceLock = new Object();
    private NetworkInterface outgoingNetworkInterface;
    private InetAddress outgoingInetAddress;

    @Override
    @Deprecated
    public void setTTL(byte ttl) throws IOException {
        setTimeToLive(Byte.toUnsignedInt(ttl));
    }

    @Override
    public void setTimeToLive(int ttl) throws IOException {
        sendLock.lock();
        try {
            setIntOption(StandardSocketOptions.IP_MULTICAST_TTL, ttl);
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    @Deprecated
    public byte getTTL() throws IOException {
        return (byte) getTimeToLive();
    }

    @Override
    public int getTimeToLive() throws IOException {
        sendLock.lock();
        try {
            return getIntOption(StandardSocketOptions.IP_MULTICAST_TTL);
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    @Deprecated
    public void joinGroup(InetAddress group) throws IOException {
        Objects.requireNonNull(group);
        try {
            joinGroup(new InetSocketAddress(group, 0), null);
        } catch (IllegalArgumentException iae) {
            // 1-arg joinGroup does not specify IllegalArgumentException
            throw (SocketException) new SocketException("joinGroup failed").initCause(iae);
        }
    }

    @Override
    @Deprecated
    public void leaveGroup(InetAddress group) throws IOException {
        Objects.requireNonNull(group);
        try {
            leaveGroup(new InetSocketAddress(group, 0), null);
        } catch (IllegalArgumentException iae) {
            // 1-arg leaveGroup does not specify IllegalArgumentException
            throw (SocketException) new SocketException("leaveGroup failed").initCause(iae);
        }
    }

    /**
     * Checks a SocketAddress to ensure that it is a multicast address.
     *
     * @return the multicast group
     * @throws IllegalArgumentException if group is null, an unsupported address
     *         type, or an unresolved address
     * @throws SocketException if group is not a multicast address
     */
    private static InetAddress checkGroup(SocketAddress mcastaddr) throws SocketException {
        if (!(mcastaddr instanceof InetSocketAddress addr))
            throw new IllegalArgumentException("Unsupported address type");
        InetAddress group = addr.getAddress();
        if (group == null)
            throw new IllegalArgumentException("Unresolved address");
        if (!group.isMulticastAddress())
            throw new SocketException("Not a multicast address");
        return group;
    }

    @Override
    public void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        InetAddress group = checkGroup(mcastaddr);
        NetworkInterface ni = (netIf != null) ? netIf : defaultNetworkInterface();
        if (isClosed())
            throw new SocketException("Socket is closed");
        synchronized (this) {
            MembershipKey key = dc.findMembership(group, ni);
            if (key != null) {
                // already a member but need to check permission anyway
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null)
                    sm.checkMulticast(group);
                throw new SocketException("Already a member of group");
            }
            dc.join(group, ni);  // checks permission
        }
    }

    @Override
    public void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        InetAddress group = checkGroup(mcastaddr);
        NetworkInterface ni = (netIf != null) ? netIf : defaultNetworkInterface();
        if (isClosed())
            throw new SocketException("Socket is closed");
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkMulticast(group);
        synchronized (this) {
            MembershipKey key = dc.findMembership(group, ni);
            if (key == null)
                throw new SocketException("Not a member of group");
            key.drop();
        }
    }

    @Override
    @Deprecated
    public void setInterface(InetAddress inf) throws SocketException {
        if (inf == null)
            throw new SocketException("Invalid value 'null'");
        NetworkInterface ni = NetworkInterface.getByInetAddress(inf);
        if (ni == null) {
            String address = inf.getHostAddress();
            throw new SocketException("No network interface with address " + address);
        }
        synchronized (outgoingInterfaceLock) {
            // set interface and update cached values
            setNetworkInterface(ni);
            outgoingNetworkInterface = ni;
            outgoingInetAddress = inf;
        }
    }

    @Override
    @Deprecated
    public InetAddress getInterface() throws SocketException {
        synchronized (outgoingInterfaceLock) {
            NetworkInterface ni = outgoingNetworkInterface();
            if (ni != null) {
                if (ni.equals(outgoingNetworkInterface)) {
                    return outgoingInetAddress;
                } else {
                    // network interface has changed so update cached values
                    PrivilegedAction<InetAddress> pa;
                    pa = () -> ni.inetAddresses().findFirst().orElse(null);
                    @SuppressWarnings("removal")
                    InetAddress ia = AccessController.doPrivileged(pa);
                    if (ia == null)
                        throw new SocketException("Network interface has no IP address");
                    outgoingNetworkInterface = ni;
                    outgoingInetAddress = ia;
                    return ia;
                }
            }
        }

        // no interface set
        return anyInetAddress();
    }

    @Override
    public void setNetworkInterface(NetworkInterface netIf) throws SocketException {
        try {
            setOption(StandardSocketOptions.IP_MULTICAST_IF, netIf);
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    @Override
    public NetworkInterface getNetworkInterface() throws SocketException {
        NetworkInterface ni = outgoingNetworkInterface();
        if (ni == null) {
            // return NetworkInterface with index == 0 as placeholder
            ni = anyNetworkInterface();
        }
        return ni;
    }

    @Override
    @Deprecated
    public void setLoopbackMode(boolean disable) throws SocketException {
        boolean enable = !disable;
        setBooleanOption(StandardSocketOptions.IP_MULTICAST_LOOP, enable);
    }

    @Override
    @Deprecated
    public boolean getLoopbackMode() throws SocketException {
        boolean enabled = getBooleanOption(StandardSocketOptions.IP_MULTICAST_LOOP);
        return !enabled;
    }

    @Override
    @Deprecated
    public void send(DatagramPacket p, byte ttl) throws IOException {
        sendLock.lock();
        try {
            int oldValue = getTimeToLive();
            try {
                setTTL(ttl);
                send(p);
            } finally {
                setTimeToLive(oldValue);
            }
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * Returns the outgoing NetworkInterface or null if not set.
     */
    private NetworkInterface outgoingNetworkInterface() throws SocketException {
        try {
            return getOption(StandardSocketOptions.IP_MULTICAST_IF);
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return null; // keep compiler happy
        }
    }

    /**
     * Returns the default NetworkInterface to use when joining or leaving a
     * multicast group and a network interface is not specified.
     * This method will return the outgoing NetworkInterface if set, otherwise
     * the result of NetworkInterface.getDefault(), otherwise a NetworkInterface
     * with index == 0 as a placeholder for "any network interface".
     */
    private NetworkInterface defaultNetworkInterface() throws SocketException {
        NetworkInterface ni = outgoingNetworkInterface();
        if (ni == null)
            ni = NetworkInterfaces.getDefault();   // macOS
        if (ni == null)
            ni = anyNetworkInterface();
        return ni;
    }

    /**
     * Returns the placeholder for "any network interface", its index is 0.
     */
    private NetworkInterface anyNetworkInterface() {
        InetAddress[] addrs = new InetAddress[1];
        addrs[0] = anyInetAddress();
        return NetworkInterfaces.newNetworkInterface(addrs[0].getHostName(), 0, addrs);
    }

    /**
     * Returns the InetAddress representing anyLocalAddress.
     */
    private InetAddress anyInetAddress() {
        return new InetSocketAddress(0).getAddress();
    }

    /**
     * Defines static methods to get/set DatagramPacket fields and workaround
     * DatagramPacket deficiencies.
     */
    private static class DatagramPackets {
        private static final VarHandle LENGTH;
        private static final VarHandle BUF_LENGTH;
        static {
            try {
                PrivilegedExceptionAction<Lookup> pa = () ->
                    MethodHandles.privateLookupIn(DatagramPacket.class, MethodHandles.lookup());
                @SuppressWarnings("removal")
                MethodHandles.Lookup l = AccessController.doPrivileged(pa);
                LENGTH = l.findVarHandle(DatagramPacket.class, "length", int.class);
                BUF_LENGTH = l.findVarHandle(DatagramPacket.class, "bufLength", int.class);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        /**
         * Sets the DatagramPacket.length field. DatagramPacket.setLength cannot be
         * used at this time because it sets both the length and bufLength fields.
         */
        static void setLength(DatagramPacket p, int value) {
            synchronized (p) {
                LENGTH.set(p, value);
            }
        }

        /**
         * Returns the value of the DatagramPacket.bufLength field.
         */
        static int getBufLength(DatagramPacket p) {
            synchronized (p) {
                return (int) BUF_LENGTH.get(p);
            }
        }
    }

    /**
     * Defines static methods to invoke non-public NetworkInterface methods.
     */
    private static class NetworkInterfaces {
        static final MethodHandle GET_DEFAULT;
        static final MethodHandle CONSTRUCTOR;
        static {
            try {
                PrivilegedExceptionAction<Lookup> pa = () ->
                    MethodHandles.privateLookupIn(NetworkInterface.class, MethodHandles.lookup());
                @SuppressWarnings("removal")
                MethodHandles.Lookup l = AccessController.doPrivileged(pa);
                MethodType methodType = MethodType.methodType(NetworkInterface.class);
                GET_DEFAULT = l.findStatic(NetworkInterface.class, "getDefault", methodType);
                methodType = MethodType.methodType(void.class, String.class, int.class, InetAddress[].class);
                CONSTRUCTOR = l.findConstructor(NetworkInterface.class, methodType);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        /**
         * Returns the default network interface or null.
         */
        static NetworkInterface getDefault() {
            try {
                return (NetworkInterface) GET_DEFAULT.invokeExact();
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        /**
         * Creates a NetworkInterface with the given name index and addresses.
         */
        static NetworkInterface newNetworkInterface(String name, int index, InetAddress[] addrs) {
            try {
                return (NetworkInterface) CONSTRUCTOR.invoke(name, index, addrs);
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }
    }

    /**
     * Provides access to the value of the private static DatagramSocket.NO_DELEGATE
     */
    private static class DatagramSockets {
        private static final SocketAddress NO_DELEGATE;

        static {
            try {
                PrivilegedExceptionAction<Lookup> pa = () ->
                        MethodHandles.privateLookupIn(DatagramSocket.class, MethodHandles.lookup());
                @SuppressWarnings("removal")
                MethodHandles.Lookup l = AccessController.doPrivileged(pa);
                NO_DELEGATE = (SocketAddress)
                        l.findStaticVarHandle(DatagramSocket.class, "NO_DELEGATE",
                                SocketAddress.class).get();
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}