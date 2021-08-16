/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


// Make a server-socket channel look like a server socket.
//
// The methods in this class are defined in exactly the same order as in
// java.net.ServerSocket so as to simplify tracking future changes to that
// class.
//

class ServerSocketAdaptor                        // package-private
    extends ServerSocket
{
    // The channel being adapted
    private final ServerSocketChannelImpl ssc;

    // Timeout "option" value for accepts
    private volatile int timeout;

    @SuppressWarnings("removal")
    static ServerSocket create(ServerSocketChannelImpl ssc) {
        PrivilegedExceptionAction<ServerSocket> pa = () -> new ServerSocketAdaptor(ssc);
        try {
            return AccessController.doPrivileged(pa);
        } catch (PrivilegedActionException pae) {
            throw new InternalError("Should not reach here", pae);
        }
    }

    private ServerSocketAdaptor(ServerSocketChannelImpl ssc) {
        super(DummySocketImpl.create());
        this.ssc = ssc;
    }

    @Override
    public void bind(SocketAddress local) throws IOException {
        bind(local, 50);
    }

    @Override
    public void bind(SocketAddress local, int backlog) throws IOException {
        if (local == null)
            local = new InetSocketAddress(0);
        try {
            ssc.bind(local, backlog);
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    @Override
    public InetAddress getInetAddress() {
        SocketAddress local = ssc.localAddress();
        if (local == null) {
            return null;
        } else {
            return Net.getRevealedLocalAddress(local).getAddress();
        }
    }

    @Override
    public int getLocalPort() {
        InetSocketAddress local = (InetSocketAddress) ssc.localAddress();
        if (local == null) {
            return -1;
        } else {
            return local.getPort();
        }
    }

    @Override
    public Socket accept() throws IOException {
        SocketChannel sc = null;
        try {
            int timeout = this.timeout;
            if (timeout > 0) {
                long nanos = MILLISECONDS.toNanos(timeout);
                sc = ssc.blockingAccept(nanos);
            } else {
                // accept connection if possible when non-blocking (to preserve
                // long standing behavior)
                sc = ssc.accept();
                if (sc == null) {
                    throw new IllegalBlockingModeException();
                }
            }
        } catch (Exception e) {
            Net.translateException(e);
        }
        return sc.socket();
    }

    @Override
    public void close() throws IOException {
        ssc.close();
    }

    @Override
    public ServerSocketChannel getChannel() {
        return ssc;
    }

    @Override
    public boolean isBound() {
        return ssc.isBound();
    }

    @Override
    public boolean isClosed() {
        return !ssc.isOpen();
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        if (!ssc.isOpen())
            throw new SocketException("Socket is closed");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout < 0");
        this.timeout = timeout;
    }

    @Override
    public int getSoTimeout() throws SocketException {
        if (!ssc.isOpen())
            throw new SocketException("Socket is closed");
        return timeout;
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        try {
            ssc.setOption(StandardSocketOptions.SO_REUSEADDR, on);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        try {
            return ssc.getOption(StandardSocketOptions.SO_REUSEADDR).booleanValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return false;       // Never happens
        }
    }

    @Override
    public String toString() {
        if (!isBound())
            return "ServerSocket[unbound]";
        return "ServerSocket[addr=" + getInetAddress() +
               ",localport=" + getLocalPort()  + "]";
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        // size 0 valid for ServerSocketChannel, invalid for ServerSocket
        if (size <= 0)
            throw new IllegalArgumentException("size cannot be 0 or negative");
        try {
            ssc.setOption(StandardSocketOptions.SO_RCVBUF, size);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        try {
            return ssc.getOption(StandardSocketOptions.SO_RCVBUF).intValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return -1;          // Never happens
        }
    }

    @Override
    public <T> ServerSocket setOption(SocketOption<T> name, T value) throws IOException {
        ssc.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return ssc.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return ssc.supportedOptions();
    }
}
