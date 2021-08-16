/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;

import sun.net.PlatformSocketImpl;
import sun.nio.ch.NioSocketImpl;

/**
 * The abstract class {@code SocketImpl} is a common superclass
 * of all classes that actually implement sockets. It is used to
 * create both client and server sockets.
 *
 * @since   1.0
 */
public abstract class SocketImpl implements SocketOptions {

    /**
     * Creates an instance of platform's SocketImpl
     */
    @SuppressWarnings("unchecked")
    static <S extends SocketImpl & PlatformSocketImpl> S createPlatformSocketImpl(boolean server) {
        return (S) new NioSocketImpl(server);
    }

    /**
     * The file descriptor object for this socket.
     */
    protected FileDescriptor fd;

    /**
     * The IP address of the remote end of this socket.
     */
    protected InetAddress address;

    /**
     * The port number on the remote host to which this socket is connected.
     */
    protected int port;

    /**
     * The local port number to which this socket is connected.
     */
    protected int localport;

    /**
     * Initialize a new instance of this class
     */
    public SocketImpl() { }

    /**
     * Creates either a stream or a datagram socket.
     *
     * @param      stream   if {@code true}, create a stream socket;
     *                      otherwise, create a datagram socket.
     * @throws     IOException  if an I/O error occurs while creating the
     *               socket.
     */
    protected abstract void create(boolean stream) throws IOException;

    /**
     * Connects this socket to the specified port on the named host.
     *
     * @param      host   the name of the remote host.
     * @param      port   the port number.
     * @throws     IOException  if an I/O error occurs when connecting to the
     *               remote host.
     */
    protected abstract void connect(String host, int port) throws IOException;

    /**
     * Connects this socket to the specified port number on the specified host.
     *
     * @param      address   the IP address of the remote host.
     * @param      port      the port number.
     * @throws     IOException  if an I/O error occurs when attempting a
     *               connection.
     */
    protected abstract void connect(InetAddress address, int port) throws IOException;

    /**
     * Connects this socket to the specified port number on the specified host.
     * A timeout of zero is interpreted as an infinite timeout. The connection
     * will then block until established or an error occurs.
     *
     * @param      address   the Socket address of the remote host.
     * @param     timeout  the timeout value, in milliseconds, or zero for no timeout.
     * @throws     IOException  if an I/O error occurs when attempting a
     *               connection.
     * @since 1.4
     */
    protected abstract void connect(SocketAddress address, int timeout) throws IOException;

    /**
     * Binds this socket to the specified local IP address and port number.
     *
     * @param      host   an IP address that belongs to a local interface.
     * @param      port   the port number.
     * @throws     IOException  if an I/O error occurs when binding this socket.
     */
    protected abstract void bind(InetAddress host, int port) throws IOException;

    /**
     * Sets the maximum queue length for incoming connection indications
     * (a request to connect) to the {@code count} argument. If a
     * connection indication arrives when the queue is full, the
     * connection is refused.
     *
     * @param      backlog   the maximum length of the queue.
     * @throws     IOException  if an I/O error occurs when creating the queue.
     */
    protected abstract void listen(int backlog) throws IOException;

    /**
     * Accepts a connection.
     *
     * @param      s   the accepted connection.
     * @throws     IOException  if an I/O error occurs when accepting the
     *               connection.
     */
    protected abstract void accept(SocketImpl s) throws IOException;

    /**
     * Returns an input stream for this socket.
     *
     * @return     a stream for reading from this socket.
     * @throws     IOException  if an I/O error occurs when creating the
     *               input stream.
     */
    protected abstract InputStream getInputStream() throws IOException;

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing to this socket.
     * @throws     IOException  if an I/O error occurs when creating the
     *               output stream.
     */
    protected abstract OutputStream getOutputStream() throws IOException;

    /**
     * Returns the number of bytes that can be read from this socket
     * without blocking.
     *
     * @return     the number of bytes that can be read from this socket
     *             without blocking.
     * @throws     IOException  if an I/O error occurs when determining the
     *               number of bytes available.
     */
    protected abstract int available() throws IOException;

    /**
     * Closes this socket.
     *
     * @throws     IOException  if an I/O error occurs when closing this socket.
     */
    protected abstract void close() throws IOException;

    /**
     * Closes this socket, ignoring any IOException that is thrown by close.
     */
    void closeQuietly() {
        try {
            close();
        } catch (IOException ignore) { }
    }

    /**
     * Places the input stream for this socket at "end of stream".
     * Any data sent to this socket is acknowledged and then
     * silently discarded.
     *
     * If you read from a socket input stream after invoking this method on the
     * socket, the stream's {@code available} method will return 0, and its
     * {@code read} methods will return {@code -1} (end of stream).
     *
     * @throws    IOException if an I/O error occurs when shutting down this
     * socket.
     * @see java.net.Socket#shutdownOutput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @since 1.3
     */
    protected void shutdownInput() throws IOException {
      throw new IOException("Method not implemented!");
    }

    /**
     * Disables the output stream for this socket.
     * For a TCP socket, any previously written data will be sent
     * followed by TCP's normal connection termination sequence.
     *
     * If you write to a socket output stream after invoking
     * shutdownOutput() on the socket, the stream will throw
     * an IOException.
     *
     * @throws    IOException if an I/O error occurs when shutting down this
     * socket.
     * @see java.net.Socket#shutdownInput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @since 1.3
     */
    protected void shutdownOutput() throws IOException {
      throw new IOException("Method not implemented!");
    }

    /**
     * Returns the value of this socket's {@code fd} field.
     *
     * @return  the value of this socket's {@code fd} field.
     * @see     java.net.SocketImpl#fd
     */
    protected FileDescriptor getFileDescriptor() {
        return fd;
    }

    /**
     * Returns the value of this socket's {@code address} field.
     *
     * @return  the value of this socket's {@code address} field.
     * @see     java.net.SocketImpl#address
     */
    protected InetAddress getInetAddress() {
        return address;
    }

    /**
     * Returns the value of this socket's {@code port} field.
     *
     * @return  the value of this socket's {@code port} field.
     * @see     java.net.SocketImpl#port
     */
    protected int getPort() {
        return port;
    }

    /**
     * Returns whether or not this SocketImpl supports sending
     * urgent data. By default, false is returned
     * unless the method is overridden in a sub-class
     *
     * @return  true if urgent data supported
     * @see     java.net.SocketImpl#address
     * @since 1.4
     */
    protected boolean supportsUrgentData () {
        return false; // must be overridden in sub-class
    }

    /**
     * Send one byte of urgent data on the socket.
     * The byte to be sent is the low eight bits of the parameter
     * @param data The byte of data to send
     * @throws    IOException if there is an error
     *  sending the data.
     * @since 1.4
     */
    protected abstract void sendUrgentData (int data) throws IOException;

    /**
     * Returns the value of this socket's {@code localport} field.
     *
     * @return  the value of this socket's {@code localport} field.
     * @see     java.net.SocketImpl#localport
     */
    protected int getLocalPort() {
        return localport;
    }

    /**
     * Returns the address and port of this socket as a {@code String}.
     *
     * @return  a string representation of this socket.
     */
    public String toString() {
        return "Socket[addr=" + getInetAddress() +
            ",port=" + getPort() + ",localport=" + getLocalPort()  + "]";
    }

    void reset() {
        fd = null;
        address = null;
        port = 0;
        localport = 0;
    }

    /**
     * Sets performance preferences for this socket.
     *
     * <p> Sockets use the TCP/IP protocol by default.  Some implementations
     * may offer alternative protocols which have different performance
     * characteristics than TCP/IP.  This method allows the application to
     * express its own preferences as to how these tradeoffs should be made
     * when the implementation chooses from the available protocols.
     *
     * <p> Performance preferences are described by three integers
     * whose values indicate the relative importance of short connection time,
     * low latency, and high bandwidth.  The absolute values of the integers
     * are irrelevant; in order to choose a protocol the values are simply
     * compared, with larger values indicating stronger preferences. Negative
     * values represent a lower priority than positive values. If the
     * application prefers short connection time over both low latency and high
     * bandwidth, for example, then it could invoke this method with the values
     * {@code (1, 0, 0)}.  If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could
     * invoke this method with the values {@code (0, 1, 2)}.
     *
     * By default, this method does nothing, unless it is overridden in
     * a sub-class.
     *
     * @param  connectionTime
     *         An {@code int} expressing the relative importance of a short
     *         connection time
     *
     * @param  latency
     *         An {@code int} expressing the relative importance of low
     *         latency
     *
     * @param  bandwidth
     *         An {@code int} expressing the relative importance of high
     *         bandwidth
     *
     * @since 1.5
     */
    protected void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth)
    {
        /* Not implemented yet */
    }

    /**
     * Called to set a socket option.
     *
     * @implSpec
     * The default implementation of this method first checks that the given
     * socket option {@code name} is not null, then throws {@code
     * UnsupportedOperationException}. Subclasses should override this method
     * with an appropriate implementation.
     *
     * @param <T> The type of the socket option value
     * @param name The socket option
     * @param value The value of the socket option. A value of {@code null}
     *              may be valid for some options.
     *
     * @throws UnsupportedOperationException if the SocketImpl does not
     *         support the option
     * @throws IllegalArgumentException if the value is not valid for
     *         the option
     * @throws IOException if an I/O error occurs, or if the socket is closed
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * Called to get a socket option.
     *
     * @implSpec
     * The default implementation of this method first checks that the given
     * socket option {@code name} is not null, then throws {@code
     * UnsupportedOperationException}. Subclasses should override this method
     * with an appropriate implementation.
     *
     * @param <T> The type of the socket option value
     * @param name The socket option
     * @return the value of the named option
     *
     * @throws UnsupportedOperationException if the SocketImpl does not
     *         support the option
     * @throws IOException if an I/O error occurs, or if the socket is closed
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * Attempts to copy socket options from this SocketImpl to a target SocketImpl.
     * At this time, only the SO_TIMEOUT make sense to copy.
     */
    void copyOptionsTo(SocketImpl target) {
        try {
            Object timeout = getOption(SocketOptions.SO_TIMEOUT);
            if (timeout instanceof Integer) {
                target.setOption(SocketOptions.SO_TIMEOUT, timeout);
            }
        } catch (IOException ignore) { }
    }

    /**
     * Returns a set of SocketOptions supported by this impl
     * and by this impl's socket (Socket or ServerSocket)
     *
     * @implSpec
     * The default implementation of this method returns an empty set.
     * Subclasses should override this method with an appropriate implementation.
     *
     * @return a Set of SocketOptions
     *
     * @since 9
     */
    protected Set<SocketOption<?>> supportedOptions() {
        return Set.of();
    }
}
