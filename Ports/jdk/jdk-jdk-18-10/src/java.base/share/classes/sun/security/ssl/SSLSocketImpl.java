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

package sun.security.ssl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import jdk.internal.access.JavaNetInetAddressAccess;
import jdk.internal.access.SharedSecrets;

/**
 * Implementation of an SSL socket.
 * <P>
 * This is a normal connection type socket, implementing SSL over some lower
 * level socket, such as TCP.  Because it is layered over some lower level
 * socket, it MUST override all default socket methods.
 * <P>
 * This API offers a non-traditional option for establishing SSL
 * connections.  You may first establish the connection directly, then pass
 * that connection to the SSL socket constructor with a flag saying which
 * role should be taken in the handshake protocol.  (The two ends of the
 * connection must not choose the same role!)  This allows setup of SSL
 * proxying or tunneling, and also allows the kind of "role reversal"
 * that is required for most FTP data transfers.
 *
 * @see javax.net.ssl.SSLSocket
 * @see SSLServerSocket
 *
 * @author David Brownell
 */
public final class SSLSocketImpl
        extends BaseSSLSocketImpl implements SSLTransport {

    /**
     * ERROR HANDLING GUIDELINES
     * (which exceptions to throw and catch and which not to throw and catch)
     *
     * - if there is an IOException (SocketException) when accessing the
     *   underlying Socket, pass it through
     *
     * - do not throw IOExceptions, throw SSLExceptions (or a subclass)
     */

    final SSLContextImpl            sslContext;
    final TransportContext          conContext;

    private final AppInputStream    appInput = new AppInputStream();
    private final AppOutputStream   appOutput = new AppOutputStream();

    private String                  peerHost;
    private boolean                 autoClose;
    private boolean                 isConnected;
    private volatile boolean        tlsIsClosed;

    private final ReentrantLock     socketLock = new ReentrantLock();
    private final ReentrantLock     handshakeLock = new ReentrantLock();

    /*
     * Is the local name service trustworthy?
     *
     * If the local name service is not trustworthy, reverse host name
     * resolution should not be performed for endpoint identification.
     */
    private static final boolean trustNameService =
            Utilities.getBooleanProperty("jdk.tls.trustNameService", false);

    /**
     * Package-private constructor used to instantiate an unconnected
     * socket.
     *
     * This instance is meant to set handshake state to use "client mode".
     */
    SSLSocketImpl(SSLContextImpl sslContext) {
        super();
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), true);
    }

    /**
     * Package-private constructor used to instantiate a server socket.
     *
     * This instance is meant to set handshake state to use "server mode".
     */
    SSLSocketImpl(SSLContextImpl sslContext, SSLConfiguration sslConfig) {
        super();
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this, sslConfig,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash));
    }

    /**
     * Constructs an SSL connection to a named host at a specified
     * port, using the authentication context provided.
     *
     * This endpoint acts as the client, and may rejoin an existing SSL session
     * if appropriate.
     */
    SSLSocketImpl(SSLContextImpl sslContext, String peerHost,
            int peerPort) throws IOException {
        super();
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), true);
        this.peerHost = peerHost;
        SocketAddress socketAddress =
               peerHost != null ? new InetSocketAddress(peerHost, peerPort) :
               new InetSocketAddress(InetAddress.getByName(null), peerPort);
        connect(socketAddress, 0);
    }

    /**
     * Constructs an SSL connection to a server at a specified
     * address, and TCP port, using the authentication context
     * provided.
     *
     * This endpoint acts as the client, and may rejoin an existing SSL
     * session if appropriate.
     */
    SSLSocketImpl(SSLContextImpl sslContext,
            InetAddress address, int peerPort) throws IOException {
        super();
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), true);

        SocketAddress socketAddress = new InetSocketAddress(address, peerPort);
        connect(socketAddress, 0);
    }

    /**
     * Constructs an SSL connection to a named host at a specified
     * port, using the authentication context provided.
     *
     * This endpoint acts as the client, and may rejoin an existing SSL
     * session if appropriate.
     */
    SSLSocketImpl(SSLContextImpl sslContext,
            String peerHost, int peerPort, InetAddress localAddr,
            int localPort) throws IOException {
        super();
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), true);
        this.peerHost = peerHost;

        bind(new InetSocketAddress(localAddr, localPort));
        SocketAddress socketAddress =
               peerHost != null ? new InetSocketAddress(peerHost, peerPort) :
               new InetSocketAddress(InetAddress.getByName(null), peerPort);
        connect(socketAddress, 0);
    }

    /**
     * Constructs an SSL connection to a server at a specified
     * address, and TCP port, using the authentication context
     * provided.
     *
     * This endpoint acts as the client, and may rejoin an existing SSL
     * session if appropriate.
     */
    SSLSocketImpl(SSLContextImpl sslContext,
            InetAddress peerAddr, int peerPort,
            InetAddress localAddr, int localPort) throws IOException {
        super();
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), true);

        bind(new InetSocketAddress(localAddr, localPort));
        SocketAddress socketAddress = new InetSocketAddress(peerAddr, peerPort);
        connect(socketAddress, 0);
    }

    /**
     * Creates a server mode {@link Socket} layered over an
     * existing connected socket, and is able to read data which has
     * already been consumed/removed from the {@link Socket}'s
     * underlying {@link InputStream}.
     */
    SSLSocketImpl(SSLContextImpl sslContext, Socket sock,
            InputStream consumed, boolean autoClose) throws IOException {
        super(sock, consumed);
        // We always layer over a connected socket
        if (!sock.isConnected()) {
            throw new SocketException("Underlying socket is not connected");
        }

        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), false);
        this.autoClose = autoClose;
        doneConnect();
    }

    /**
     * Layer SSL traffic over an existing connection, rather than
     * creating a new connection.
     *
     * The existing connection may be used only for SSL traffic (using this
     * SSLSocket) until the SSLSocket.close() call returns. However, if a
     * protocol error is detected, that existing connection is automatically
     * closed.
     * <p>
     * This particular constructor always uses the socket in the
     * role of an SSL client. It may be useful in cases which start
     * using SSL after some initial data transfers, for example in some
     * SSL tunneling applications or as part of some kinds of application
     * protocols which negotiate use of a SSL based security.
     */
    SSLSocketImpl(SSLContextImpl sslContext, Socket sock,
            String peerHost, int port, boolean autoClose) throws IOException {
        super(sock);
        // We always layer over a connected socket
        if (!sock.isConnected()) {
            throw new SocketException("Underlying socket is not connected");
        }

        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        this.conContext = new TransportContext(sslContext, this,
                new SSLSocketInputRecord(handshakeHash),
                new SSLSocketOutputRecord(handshakeHash), true);
        this.peerHost = peerHost;
        this.autoClose = autoClose;
        doneConnect();
    }

    @Override
    public void connect(SocketAddress endpoint,
            int timeout) throws IOException {

        if (isLayered()) {
            throw new SocketException("Already connected");
        }

        if (!(endpoint instanceof InetSocketAddress)) {
            throw new SocketException(
                    "Cannot handle non-Inet socket addresses.");
        }

        super.connect(endpoint, timeout);
        doneConnect();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return CipherSuite.namesOf(sslContext.getSupportedCipherSuites());
    }

    @Override
    public String[] getEnabledCipherSuites() {
        socketLock.lock();
        try {
            return CipherSuite.namesOf(
                    conContext.sslConfig.enabledCipherSuites);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        socketLock.lock();
        try {
            conContext.sslConfig.enabledCipherSuites =
                    CipherSuite.validValuesOf(suites);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public String[] getSupportedProtocols() {
        return ProtocolVersion.toStringArray(
                sslContext.getSupportedProtocolVersions());
    }

    @Override
    public String[] getEnabledProtocols() {
        socketLock.lock();
        try {
            return ProtocolVersion.toStringArray(
                    conContext.sslConfig.enabledProtocols);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        if (protocols == null) {
            throw new IllegalArgumentException("Protocols cannot be null");
        }

        socketLock.lock();
        try {
            conContext.sslConfig.enabledProtocols =
                    ProtocolVersion.namesOf(protocols);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public SSLSession getSession() {
        try {
            // start handshaking, if failed, the connection will be closed.
            ensureNegotiated(false);
        } catch (IOException ioe) {
            if (SSLLogger.isOn && SSLLogger.isOn("handshake")) {
                SSLLogger.severe("handshake failed", ioe);
            }

            return new SSLSessionImpl();
        }

        return conContext.conSession;
    }

    @Override
    public SSLSession getHandshakeSession() {
        socketLock.lock();
        try {
            return conContext.handshakeContext == null ?
                    null : conContext.handshakeContext.handshakeSession;
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void addHandshakeCompletedListener(
            HandshakeCompletedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }

        socketLock.lock();
        try {
            conContext.sslConfig.addHandshakeCompletedListener(listener);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void removeHandshakeCompletedListener(
            HandshakeCompletedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }

        socketLock.lock();
        try {
            conContext.sslConfig.removeHandshakeCompletedListener(listener);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void startHandshake() throws IOException {
        startHandshake(true);
    }

    private void startHandshake(boolean resumable) throws IOException {
        if (!isConnected) {
            throw new SocketException("Socket is not connected");
        }

        if (conContext.isBroken || conContext.isInboundClosed() ||
                conContext.isOutboundClosed()) {
            throw new SocketException("Socket has been closed or broken");
        }

        handshakeLock.lock();
        try {
            // double check the context status
            if (conContext.isBroken || conContext.isInboundClosed() ||
                    conContext.isOutboundClosed()) {
                throw new SocketException("Socket has been closed or broken");
            }

            try {
                conContext.kickstart();

                // All initial handshaking goes through this operation until we
                // have a valid SSL connection.
                //
                // Handle handshake messages only, need no application data.
                if (!conContext.isNegotiated) {
                    readHandshakeRecord();
                }
            } catch (InterruptedIOException iioe) {
                if(resumable){
                    handleException(iioe);
                } else{
                    throw conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Couldn't kickstart handshaking", iioe);
                }
            } catch (SocketException se) {
                handleException(se);
            } catch (IOException ioe) {
                throw conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Couldn't kickstart handshaking", ioe);
            } catch (Exception oe) {    // including RuntimeException
                handleException(oe);
            }
        } finally {
            handshakeLock.unlock();
        }
    }

    @Override
    public void setUseClientMode(boolean mode) {
        socketLock.lock();
        try {
            conContext.setUseClientMode(mode);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public boolean getUseClientMode() {
        socketLock.lock();
        try {
            return conContext.sslConfig.isClientMode;
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        socketLock.lock();
        try {
            conContext.sslConfig.clientAuthType =
                    (need ? ClientAuthType.CLIENT_AUTH_REQUIRED :
                            ClientAuthType.CLIENT_AUTH_NONE);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public boolean getNeedClientAuth() {
        socketLock.lock();
        try {
            return (conContext.sslConfig.clientAuthType ==
                        ClientAuthType.CLIENT_AUTH_REQUIRED);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void setWantClientAuth(boolean want) {
        socketLock.lock();
        try {
            conContext.sslConfig.clientAuthType =
                    (want ? ClientAuthType.CLIENT_AUTH_REQUESTED :
                            ClientAuthType.CLIENT_AUTH_NONE);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public boolean getWantClientAuth() {
        socketLock.lock();
        try {
            return (conContext.sslConfig.clientAuthType ==
                        ClientAuthType.CLIENT_AUTH_REQUESTED);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        socketLock.lock();
        try {
            conContext.sslConfig.enableSessionCreation = flag;
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        socketLock.lock();
        try {
            return conContext.sslConfig.enableSessionCreation;
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        return tlsIsClosed;
    }

    // Please don't synchronized this method.  Otherwise, the read and close
    // locks may be deadlocked.
    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }

        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.fine("duplex close of SSLSocket");
        }

        try {
            if (isConnected()) {
                // shutdown output bound, which may have been closed previously.
                if (!isOutputShutdown()) {
                    duplexCloseOutput();
                }

                // shutdown input bound, which may have been closed previously.
                if (!isInputShutdown()) {
                    duplexCloseInput();
                }
            }
        } catch (IOException ioe) {
            // ignore the exception
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("SSLSocket duplex close failed. Debug info only. Exception details:", ioe);
            }
        } finally {
            if (!isClosed()) {
                // close the connection directly
                try {
                    closeSocket(false);
                } catch (IOException ioe) {
                    // ignore the exception
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.warning("SSLSocket close failed. Debug info only. Exception details:", ioe);
                    }
                } finally {
                    tlsIsClosed = true;
                }
            }
        }
    }

    /**
     * Duplex close, start from closing outbound.
     *
     * For TLS 1.2 [RFC 5246], unless some other fatal alert has been
     * transmitted, each party is required to send a close_notify alert
     * before closing the write side of the connection.  The other party
     * MUST respond with a close_notify alert of its own and close down
     * the connection immediately, discarding any pending writes.  It is
     * not required for the initiator of the close to wait for the responding
     * close_notify alert before closing the read side of the connection.
     *
     * For TLS 1.3, Each party MUST send a close_notify alert before
     * closing its write side of the connection, unless it has already sent
     * some error alert.  This does not have any effect on its read side of
     * the connection.  Both parties need not wait to receive a close_notify
     * alert before closing their read side of the connection, though doing
     * so would introduce the possibility of truncation.
     *
     * In order to support user initiated duplex-close for TLS 1.3 connections,
     * the user_canceled alert is used together with the close_notify alert.
     */
    private void duplexCloseOutput() throws IOException {
        boolean useUserCanceled = false;
        boolean hasCloseReceipt = false;
        if (conContext.isNegotiated) {
            if (!conContext.protocolVersion.useTLS13PlusSpec()) {
                hasCloseReceipt = true;
            } else {
                // Use a user_canceled alert for TLS 1.3 duplex close.
                useUserCanceled = true;
            }
        } else if (conContext.handshakeContext != null) {   // initial handshake
            // Use user_canceled alert regardless the protocol versions.
            useUserCanceled = true;

            // The protocol version may have been negotiated.
            ProtocolVersion pv = conContext.handshakeContext.negotiatedProtocol;
            if (pv == null || (!pv.useTLS13PlusSpec())) {
                hasCloseReceipt = true;
            }
        }

        // Deliver the user_canceled alert and the close notify alert.
        closeNotify(useUserCanceled);

        if (!isInputShutdown()) {
            bruteForceCloseInput(hasCloseReceipt);
        }
    }

    void closeNotify(boolean useUserCanceled) throws IOException {
        // Need a lock here so that the user_canceled alert and the
        // close_notify alert can be delivered together.
        int linger = getSoLinger();
        if (linger >= 0) {
            // don't wait more than SO_LINGER for obtaining the
            // the lock.
            //
            // keep and clear the current thread interruption status.
            boolean interrupted = Thread.interrupted();
            try {
                if (conContext.outputRecord.recordLock.tryLock() ||
                        conContext.outputRecord.recordLock.tryLock(
                                linger, TimeUnit.SECONDS)) {
                    try {
                        deliverClosedNotify(useUserCanceled);
                    } finally {
                        conContext.outputRecord.recordLock.unlock();
                    }
                } else {
                    // For layered, non-autoclose sockets, we are not
                    // able to bring them into a usable state, so we
                    // treat it as fatal error.
                    if (!super.isOutputShutdown()) {
                        if (isLayered() && !autoClose) {
                            throw new SSLException(
                                    "SO_LINGER timeout, " +
                                    "close_notify message cannot be sent.");
                        } else {
                            super.shutdownOutput();
                            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                                SSLLogger.warning(
                                    "SSLSocket output duplex close failed: " +
                                    "SO_LINGER timeout, " +
                                    "close_notify message cannot be sent.");
                            }
                        }
                    }

                    // RFC2246 requires that the session becomes
                    // unresumable if any connection is terminated
                    // without proper close_notify messages with
                    // level equal to warning.
                    //
                    // RFC4346 no longer requires that a session not be
                    // resumed if failure to properly close a connection.
                    //
                    // We choose to make the session unresumable if
                    // failed to send the close_notify message.
                    //
                    conContext.conSession.invalidate();
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.warning(
                                "Invalidate the session: SO_LINGER timeout, " +
                                "close_notify message cannot be sent.");
                    }
                }
            } catch (InterruptedException ex) {
                // keep interrupted status
                interrupted = true;
            }

            // restore the interrupted status
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            conContext.outputRecord.recordLock.lock();
            try {
                deliverClosedNotify(useUserCanceled);
            } finally {
                conContext.outputRecord.recordLock.unlock();
            }
        }
    }

    private void deliverClosedNotify(
            boolean useUserCanceled) throws IOException {
        try {
            // send a user_canceled alert if needed.
            if (useUserCanceled) {
                conContext.warning(Alert.USER_CANCELED);
            }

            // send a close_notify alert
            conContext.warning(Alert.CLOSE_NOTIFY);
        } finally {
            if (!conContext.isOutboundClosed()) {
                conContext.outputRecord.close();
            }

            if (!super.isOutputShutdown() &&
                    (autoClose || !isLayered())) {
                super.shutdownOutput();
            }
        }
    }

    /**
     * Duplex close, start from closing inbound.
     *
     * This method should only be called when the outbound has been closed,
     * but the inbound is still open.
     */
    private void duplexCloseInput() throws IOException {
        boolean hasCloseReceipt = false;
        if (conContext.isNegotiated &&
                !conContext.protocolVersion.useTLS13PlusSpec()) {
            hasCloseReceipt = true;
        }   // No close receipt if handshake has no completed.

        bruteForceCloseInput(hasCloseReceipt);
    }

    /**
     * Brute force close the input bound.
     *
     * This method should only be called when the outbound has been closed,
     * but the inbound is still open.
     */
    private void bruteForceCloseInput(
            boolean hasCloseReceipt) throws IOException {
        if (hasCloseReceipt) {
            // It is not required for the initiator of the close to wait for
            // the responding close_notify alert before closing the read side
            // of the connection.  However, if the application protocol using
            // TLS provides that any data may be carried over the underlying
            // transport after the TLS connection is closed, the TLS
            // implementation MUST receive a "close_notify" alert before
            // indicating end-of-data to the application-layer.
            try {
                this.shutdown();
            } finally {
                if (!isInputShutdown()) {
                    shutdownInput(false);
                }
            }
        } else {
            if (!conContext.isInboundClosed()) {
                try (conContext.inputRecord) {
                    // Try the best to use up the input records and close the
                    // socket gracefully, without impact the performance too
                    // much.
                    appInput.deplete();
                }
            }

            if ((autoClose || !isLayered()) && !super.isInputShutdown()) {
                super.shutdownInput();
            }
        }
    }

    // Please don't synchronized this method.  Otherwise, the read and close
    // locks may be deadlocked.
    @Override
    public void shutdownInput() throws IOException {
        shutdownInput(true);
    }

    // It is not required to check the close_notify receipt unless an
    // application call shutdownInput() explicitly.
    private void shutdownInput(
            boolean checkCloseNotify) throws IOException {
        if (isInputShutdown()) {
            return;
        }

        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.fine("close inbound of SSLSocket");
        }

        // Is it ready to close inbound?
        //
        // No need to throw exception if the initial handshake is not started.
        try {
            if (checkCloseNotify && !conContext.isInputCloseNotified &&
                (conContext.isNegotiated || conContext.handshakeContext != null)) {
            throw new SSLException(
                    "closing inbound before receiving peer's close_notify");
            }
        } finally {
            conContext.closeInbound();
            if ((autoClose || !isLayered()) && !super.isInputShutdown()) {
                super.shutdownInput();
            }
        }
    }

    @Override
    public boolean isInputShutdown() {
        return conContext.isInboundClosed() &&
                (!autoClose && isLayered() || super.isInputShutdown());
    }

    // Please don't synchronized this method.  Otherwise, the read and close
    // locks may be deadlocked.
    @Override
    public void shutdownOutput() throws IOException {
        if (isOutputShutdown()) {
            return;
        }

        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.fine("close outbound of SSLSocket");
        }
        conContext.closeOutbound();

        if ((autoClose || !isLayered()) && !super.isOutputShutdown()) {
            super.shutdownOutput();
        }
    }

    @Override
    public boolean isOutputShutdown() {
        return conContext.isOutboundClosed() &&
                (!autoClose && isLayered() || super.isOutputShutdown());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        socketLock.lock();
        try {
            if (isClosed()) {
                throw new SocketException("Socket is closed");
            }

            if (!isConnected) {
                throw new SocketException("Socket is not connected");
            }

            if (conContext.isInboundClosed() || isInputShutdown()) {
                throw new SocketException("Socket input is already shutdown");
            }

            return appInput;
        } finally {
            socketLock.unlock();
        }
    }

    private void ensureNegotiated(boolean resumable) throws IOException {
        if (conContext.isNegotiated || conContext.isBroken ||
                conContext.isInboundClosed() || conContext.isOutboundClosed()) {
            return;
        }

        handshakeLock.lock();
        try {
            // double check the context status
            if (conContext.isNegotiated || conContext.isBroken ||
                    conContext.isInboundClosed() ||
                    conContext.isOutboundClosed()) {
                return;
            }

            startHandshake(resumable);
        } finally {
            handshakeLock.unlock();
        }
    }

    /**
     * InputStream for application data as returned by
     * SSLSocket.getInputStream().
     */
    private class AppInputStream extends InputStream {
        // One element array used to implement the single byte read() method
        private final byte[] oneByte = new byte[1];

        // the temporary buffer used to read network
        private ByteBuffer buffer;

        // Is application data available in the stream?
        private volatile boolean appDataIsAvailable;

        // reading lock
        private final ReentrantLock readLock = new ReentrantLock();

        // closing status
        private volatile boolean isClosing;
        private volatile boolean hasDepleted;

        AppInputStream() {
            this.appDataIsAvailable = false;
            this.buffer = ByteBuffer.allocate(4096);
        }

        /**
         * Return the minimum number of bytes that can be read
         * without blocking.
         */
        @Override
        public int available() throws IOException {
            // Currently not synchronized.
            if ((!appDataIsAvailable) || checkEOF()) {
                return 0;
            }

            return buffer.remaining();
        }

        /**
         * Read a single byte, returning -1 on non-fault EOF status.
         */
        @Override
        public int read() throws IOException {
            int n = read(oneByte, 0, 1);
            if (n <= 0) {   // EOF
                return -1;
            }

            return oneByte[0] & 0xFF;
        }

        /**
         * Reads up to {@code len} bytes of data from the input stream
         * into an array of bytes.
         *
         * An attempt is made to read as many as {@code len} bytes, but a
         * smaller number may be read. The number of bytes actually read
         * is returned as an integer.
         *
         * If the layer above needs more data, it asks for more, so we
         * are responsible only for blocking to fill at most one buffer,
         * and returning "-1" on non-fault EOF status.
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException("the target buffer is null");
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException(
                        "buffer length: " + b.length + ", offset; " + off +
                        ", bytes to read:" + len);
            } else if (len == 0) {
                return 0;
            }

            if (checkEOF()) {
                return -1;
            }

            // start handshaking if the connection has not been negotiated.
            if (!conContext.isNegotiated && !conContext.isBroken &&
                    !conContext.isInboundClosed() &&
                    !conContext.isOutboundClosed()) {
                ensureNegotiated(true);
            }

            // Check if the Socket is invalid (error or closed).
            if (!conContext.isNegotiated ||
                    conContext.isBroken || conContext.isInboundClosed()) {
                throw new SocketException("Connection or inbound has closed");
            }

            // Check if the input stream has been depleted.
            //
            // Note that the "hasDepleted" rather than the isClosing
            // filed is checked here, in case the closing process is
            // still in progress.
            if (hasDepleted) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("The input stream has been depleted");
                }

                return -1;
            }

            // Read the available bytes at first.
            //
            // Note that the receiving and processing of post-handshake message
            // are also synchronized with the read lock.
            readLock.lock();
            try {
                // Double check if the Socket is invalid (error or closed).
                if (conContext.isBroken || conContext.isInboundClosed()) {
                    throw new SocketException(
                            "Connection or inbound has closed");
                }

                // Double check if the input stream has been depleted.
                if (hasDepleted) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.fine("The input stream is closing");
                    }

                    return -1;
                }

                int remains = available();
                if (remains > 0) {
                    int howmany = Math.min(remains, len);
                    buffer.get(b, off, howmany);

                    return howmany;
                }

                appDataIsAvailable = false;
                try {
                    ByteBuffer bb = readApplicationRecord(buffer);
                    if (bb == null) {   // EOF
                        return -1;
                    } else {
                        // The buffer may be reallocated for bigger capacity.
                        buffer = bb;
                    }

                    bb.flip();
                    int volume = Math.min(len, bb.remaining());
                    buffer.get(b, off, volume);
                    appDataIsAvailable = true;

                    return volume;
                } catch (Exception e) {   // including RuntimeException
                    // shutdown and rethrow (wrapped) exception as appropriate
                    handleException(e);

                    // dummy for compiler
                    return -1;
                }
            } finally {
                // Check if the input stream is closing.
                //
                // If the deplete() did not hold the lock, clean up the
                // input stream here.
                try {
                    if (isClosing) {
                        readLockedDeplete();
                    }
                } finally {
                    readLock.unlock();
                }
            }
        }

        /**
         * Skip n bytes.
         *
         * This implementation is somewhat less efficient than possible, but
         * not badly so (redundant copy).  We reuse the read() code to keep
         * things simpler.
         */
        @Override
        public long skip(long n) throws IOException {
            // dummy array used to implement skip()
            byte[] skipArray = new byte[256];
            long skipped = 0;

            readLock.lock();
            try {
                while (n > 0) {
                    int len = (int)Math.min(n, skipArray.length);
                    int r = read(skipArray, 0, len);
                    if (r <= 0) {
                        break;
                    }
                    n -= r;
                    skipped += r;
                }
            } finally {
                readLock.unlock();
            }

            return skipped;
        }

        @Override
        public void close() throws IOException {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("Closing input stream");
            }

            try {
                SSLSocketImpl.this.close();
            } catch (IOException ioe) {
                // ignore the exception
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning("input stream close failed. Debug info only. Exception details:", ioe);
                }
            }
        }

        /**
         * Return whether we have reached end-of-file.
         *
         * If the socket is not connected, has been shutdown because of an error
         * or has been closed, throw an Exception.
         */
        private boolean checkEOF() throws IOException {
            if (conContext.isBroken) {
                if (conContext.closeReason == null) {
                    return true;
                } else {
                    throw new SSLException(
                            "Connection has closed: " + conContext.closeReason,
                            conContext.closeReason);
                }
            } else if (conContext.isInboundClosed()) {
                return true;
            } else if (conContext.isInputCloseNotified) {
                if (conContext.closeReason == null) {
                    return true;
                } else {
                    throw new SSLException(
                        "Connection has closed: " + conContext.closeReason,
                        conContext.closeReason);
                }
            }

            return false;
        }

        /**
         * Try the best to use up the input records so as to close the
         * socket gracefully, without impact the performance too much.
         */
        private void deplete() {
            if (conContext.isInboundClosed() || isClosing) {
                return;
            }

            isClosing = true;
            if (readLock.tryLock()) {
                try {
                    readLockedDeplete();
                } finally {
                    readLock.unlock();
                }
            }
        }

        /**
         * Try to use up the input records.
         *
         * Please don't call this method unless the readLock is held by
         * the current thread.
         */
        private void readLockedDeplete() {
            // double check
            if (hasDepleted || conContext.isInboundClosed()) {
                return;
            }

            if (!(conContext.inputRecord instanceof SSLSocketInputRecord)) {
                return;
            }

            SSLSocketInputRecord socketInputRecord =
                    (SSLSocketInputRecord)conContext.inputRecord;
            try {
                socketInputRecord.deplete(
                    conContext.isNegotiated && (getSoTimeout() > 0));
            } catch (Exception ex) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning(
                        "input stream close depletion failed", ex);
                }
            } finally {
                hasDepleted = true;
            }
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        socketLock.lock();
        try {
            if (isClosed()) {
                throw new SocketException("Socket is closed");
            }

            if (!isConnected) {
                throw new SocketException("Socket is not connected");
            }

            if (conContext.isOutboundDone() || isOutputShutdown()) {
                throw new SocketException("Socket output is already shutdown");
            }

            return appOutput;
        } finally {
            socketLock.unlock();
        }
    }


    /**
     * OutputStream for application data as returned by
     * SSLSocket.getOutputStream().
     */
    private class AppOutputStream extends OutputStream {
        // One element array used to implement the write(byte) method
        private final byte[] oneByte = new byte[1];

        @Override
        public void write(int i) throws IOException {
            oneByte[0] = (byte)i;
            write(oneByte, 0, 1);
        }

        @Override
        public void write(byte[] b,
                int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException("the source buffer is null");
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException(
                        "buffer length: " + b.length + ", offset; " + off +
                        ", bytes to read:" + len);
            } else if (len == 0) {
                //
                // Don't bother to really write empty records.  We went this
                // far to drive the handshake machinery, for correctness; not
                // writing empty records improves performance by cutting CPU
                // time and network resource usage.  However, some protocol
                // implementations are fragile and don't like to see empty
                // records, so this also increases robustness.
                //
                return;
            }

            // Start handshaking if the connection has not been negotiated.
            if (!conContext.isNegotiated && !conContext.isBroken &&
                    !conContext.isInboundClosed() &&
                    !conContext.isOutboundClosed()) {
                ensureNegotiated(true);
            }

            // Check if the Socket is invalid (error or closed).
            if (!conContext.isNegotiated ||
                    conContext.isBroken || conContext.isOutboundClosed()) {
                throw new SocketException("Connection or outbound has closed");
            }

            //

            // Delegate the writing to the underlying socket.
            try {
                conContext.outputRecord.deliver(b, off, len);
            } catch (SSLHandshakeException she) {
                // may be record sequence number overflow
                throw conContext.fatal(Alert.HANDSHAKE_FAILURE, she);
            } catch (SSLException ssle) {
                throw conContext.fatal(Alert.UNEXPECTED_MESSAGE, ssle);
            }   // re-throw other IOException, which should be caused by
                // the underlying plain socket and could be handled by
                // applications (for example, re-try the connection).

            // Is the sequence number is nearly overflow, or has the key usage
            // limit been reached?
            if (conContext.outputRecord.seqNumIsHuge() ||
                    conContext.outputRecord.writeCipher.atKeyLimit()) {
                tryKeyUpdate();
            }
            // Check if NewSessionTicket PostHandshake message needs to be sent
            if (conContext.conSession.updateNST) {
                conContext.conSession.updateNST = false;
                tryNewSessionTicket();
            }
        }

        @Override
        public void close() throws IOException {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("Closing output stream");
            }

            try {
                SSLSocketImpl.this.close();
            } catch (IOException ioe) {
                // ignore the exception
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning("output stream close failed. Debug info only. Exception details:", ioe);
                }
            }
        }
    }

    @Override
    public SSLParameters getSSLParameters() {
        socketLock.lock();
        try {
            return conContext.sslConfig.getSSLParameters();
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void setSSLParameters(SSLParameters params) {
        socketLock.lock();
        try {
            conContext.sslConfig.setSSLParameters(params);

            if (conContext.sslConfig.maximumPacketSize != 0) {
                conContext.outputRecord.changePacketSize(
                        conContext.sslConfig.maximumPacketSize);
            }
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public String getApplicationProtocol() {
        socketLock.lock();
        try {
            return conContext.applicationProtocol;
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public String getHandshakeApplicationProtocol() {
        socketLock.lock();
        try {
            if (conContext.handshakeContext != null) {
                return conContext.handshakeContext.applicationProtocol;
            }
        } finally {
            socketLock.unlock();
        }

        return null;
    }

    @Override
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLSocket, List<String>, String> selector) {
        socketLock.lock();
        try {
            conContext.sslConfig.socketAPSelector = selector;
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public BiFunction<SSLSocket, List<String>, String>
            getHandshakeApplicationProtocolSelector() {
        socketLock.lock();
        try {
            return conContext.sslConfig.socketAPSelector;
        } finally {
            socketLock.unlock();
        }
    }

    /**
     * Read the initial handshake records.
     */
    private int readHandshakeRecord() throws IOException {
        while (!conContext.isInboundClosed()) {
            try {
                Plaintext plainText = decode(null);
                if ((plainText.contentType == ContentType.HANDSHAKE.id) &&
                        conContext.isNegotiated) {
                    return 0;
                }
            } catch (SSLException |
                    InterruptedIOException | SocketException se) {
                // Don't change exception in case of timeouts or interrupts
                // or SocketException.
                throw se;
            } catch (IOException ioe) {
                throw new SSLException("readHandshakeRecord", ioe);
            }
        }

        return -1;
    }

    /**
     * Read application data record. Used by AppInputStream only, but defined
     * here so as to use the socket level synchronization.
     *
     * Note that the connection guarantees that handshake, alert, and change
     * cipher spec data streams are handled as they arrive, so we never see
     * them here.
     *
     * Note: Please be careful about the synchronization, and don't use this
     * method other than in the AppInputStream class!
     */
    private ByteBuffer readApplicationRecord(
            ByteBuffer buffer) throws IOException {
        while (!conContext.isInboundClosed()) {
            /*
             * clean the buffer and check if it is too small, e.g. because
             * the AppInputStream did not have the chance to see the
             * current packet length but rather something like that of the
             * handshake before. In that case we return 0 at this point to
             * give the caller the chance to adjust the buffer.
             */
            buffer.clear();
            int inLen = conContext.inputRecord.bytesInCompletePacket();
            if (inLen < 0) {    // EOF
                handleEOF(null);

                // if no exception thrown
                return null;
            }

            // Is this packet bigger than SSL/TLS normally allows?
            if (inLen > SSLRecord.maxLargeRecordSize) {
                throw new SSLProtocolException(
                        "Illegal packet size: " + inLen);
            }

            if (inLen > buffer.remaining()) {
                buffer = ByteBuffer.allocate(inLen);
            }

            try {
                Plaintext plainText = decode(buffer);
                if (plainText.contentType == ContentType.APPLICATION_DATA.id &&
                        buffer.position() > 0) {
                    return buffer;
                }
            } catch (SSLException |
                    InterruptedIOException | SocketException se) {
                // Don't change exception in case of timeouts or interrupts
                // or SocketException.
                throw se;
            } catch (IOException ioe) {
                throw new SSLException("readApplicationRecord", ioe);
            }
        }

        //
        // Couldn't read, due to some kind of error or inbound
        // has been closed.
        //
        return null;
    }

    private Plaintext decode(ByteBuffer destination) throws IOException {
        Plaintext plainText;
        try {
            if (destination == null) {
                plainText = SSLTransport.decode(conContext,
                        null, 0, 0, null, 0, 0);
            } else {
                plainText = SSLTransport.decode(conContext,
                        null, 0, 0, new ByteBuffer[]{destination}, 0, 1);
            }
        } catch (EOFException eofe) {
            // EOFException is special as it is related to close_notify.
            plainText = handleEOF(eofe);
        }

        // Is the sequence number is nearly overflow?
        if (plainText != Plaintext.PLAINTEXT_NULL &&
                (conContext.inputRecord.seqNumIsHuge() ||
                conContext.inputRecord.readCipher.atKeyLimit())) {
            tryKeyUpdate();
        }

        return plainText;
    }

    /**
     * Try key update for sequence number wrap or key usage limit.
     *
     * Note that in order to maintain the handshake status properly, we check
     * the sequence number and key usage limit after the last record
     * reading/writing process.
     *
     * As we request renegotiation or close the connection for wrapped sequence
     * number when there is enough sequence number space left to handle a few
     * more records, so the sequence number of the last record cannot be
     * wrapped.
     */
    private void tryKeyUpdate() throws IOException {
        // Don't bother to kickstart if handshaking is in progress, or if the
        // connection is not duplex-open.
        if ((conContext.handshakeContext == null) &&
                !conContext.isOutboundClosed() &&
                !conContext.isInboundClosed() &&
                !conContext.isBroken) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("trigger key update");
            }
            startHandshake();
        }
    }

    // Try to generate a PostHandshake NewSessionTicket message.  This is
    // TLS 1.3 only.
    private void tryNewSessionTicket() throws IOException {
        // Don't bother to kickstart if handshaking is in progress, or if the
        // connection is not duplex-open.
        if (!conContext.sslConfig.isClientMode &&
                conContext.protocolVersion.useTLS13PlusSpec() &&
                conContext.handshakeContext == null &&
                !conContext.isOutboundClosed() &&
                !conContext.isInboundClosed() &&
                !conContext.isBroken) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("trigger new session ticket");
            }
            NewSessionTicket.t13PosthandshakeProducer.produce(
                    new PostHandshakeContext(conContext));
        }
    }

    /**
     * Initialize the handshaker and socket streams.
     *
     * Called by connect, the layered constructor, and SSLServerSocket.
     */
    void doneConnect() throws IOException {
        socketLock.lock();
        try {
            // In server mode, it is not necessary to set host and serverNames.
            // Otherwise, would require a reverse DNS lookup to get
            // the hostname.
            if (peerHost == null || peerHost.isEmpty()) {
                boolean useNameService =
                        trustNameService && conContext.sslConfig.isClientMode;
                useImplicitHost(useNameService);
            } else {
                conContext.sslConfig.serverNames =
                        Utilities.addToSNIServerNameList(
                                conContext.sslConfig.serverNames, peerHost);
            }

            InputStream sockInput = super.getInputStream();
            conContext.inputRecord.setReceiverStream(sockInput);

            OutputStream sockOutput = super.getOutputStream();
            conContext.inputRecord.setDeliverStream(sockOutput);
            conContext.outputRecord.setDeliverStream(sockOutput);

            this.isConnected = true;
        } finally {
            socketLock.unlock();
        }
    }

    private void useImplicitHost(boolean useNameService) {
        // Note: If the local name service is not trustworthy, reverse
        // host name resolution should not be performed for endpoint
        // identification.  Use the application original specified
        // hostname or IP address instead.

        // Get the original hostname via jdk.internal.access.SharedSecrets
        InetAddress inetAddress = getInetAddress();
        if (inetAddress == null) {      // not connected
            return;
        }

        JavaNetInetAddressAccess jna =
                SharedSecrets.getJavaNetInetAddressAccess();
        String originalHostname = jna.getOriginalHostName(inetAddress);
        if (originalHostname != null && !originalHostname.isEmpty()) {

            this.peerHost = originalHostname;
            if (conContext.sslConfig.serverNames.isEmpty() &&
                    !conContext.sslConfig.noSniExtension) {
                conContext.sslConfig.serverNames =
                        Utilities.addToSNIServerNameList(
                                conContext.sslConfig.serverNames, peerHost);
            }

            return;
        }

        // No explicitly specified hostname, no server name indication.
        if (!useNameService) {
            // The local name service is not trustworthy, use IP address.
            this.peerHost = inetAddress.getHostAddress();
        } else {
            // Use the underlying reverse host name resolution service.
            this.peerHost = getInetAddress().getHostName();
        }
    }

    // ONLY used by HttpsClient to setup the URI specified hostname
    //
    // Please NOTE that this method MUST be called before calling to
    // SSLSocket.setSSLParameters(). Otherwise, the {@code host} parameter
    // may override SNIHostName in the customized server name indication.
    public void setHost(String host) {
        socketLock.lock();
        try {
            this.peerHost = host;
            this.conContext.sslConfig.serverNames =
                    Utilities.addToSNIServerNameList(
                            conContext.sslConfig.serverNames, host);
        } finally {
            socketLock.unlock();
        }
    }

    /**
     * Handle an exception.
     *
     * This method is called by top level exception handlers (in read(),
     * write()) to make sure we always shutdown the connection correctly
     * and do not pass runtime exception to the application.
     *
     * This method never returns normally, it always throws an IOException.
     */
    private void handleException(Exception cause) throws IOException {
        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.warning("handling exception", cause);
        }

        // Don't close the Socket in case of timeouts or interrupts.
        if (cause instanceof InterruptedIOException) {
            throw (IOException)cause;
        }

        // need to perform error shutdown
        boolean isSSLException = (cause instanceof SSLException);
        Alert alert;
        if (isSSLException) {
            if (cause instanceof SSLHandshakeException) {
                alert = Alert.HANDSHAKE_FAILURE;
            } else {
                alert = Alert.UNEXPECTED_MESSAGE;
            }
        } else {
            if (cause instanceof IOException) {
                alert = Alert.UNEXPECTED_MESSAGE;
            } else {
                // RuntimeException
                alert = Alert.INTERNAL_ERROR;
            }
        }

        if (cause instanceof SocketException) {
            try {
                throw conContext.fatal(alert, cause);
            } catch (Exception e) {
                // Just delivering the fatal alert, re-throw the socket exception instead.
            }

            throw (SocketException)cause;
        }

        throw conContext.fatal(alert, cause);
    }

    private Plaintext handleEOF(EOFException eofe) throws IOException {
        if (requireCloseNotify || conContext.handshakeContext != null) {
            SSLException ssle;
            if (conContext.handshakeContext != null) {
                ssle = new SSLHandshakeException(
                        "Remote host terminated the handshake");
            } else {
                ssle = new SSLProtocolException(
                        "Remote host terminated the connection");
            }

            if (eofe != null) {
                ssle.initCause(eofe);
            }
            throw ssle;
        } else {
            // treat as if we had received a close_notify
            conContext.isInputCloseNotified = true;
            shutdownInput();

            return Plaintext.PLAINTEXT_NULL;
        }
    }


    @Override
    public String getPeerHost() {
        return peerHost;
    }

    @Override
    public int getPeerPort() {
        return getPort();
    }

    @Override
    public boolean useDelegatedTask() {
        return false;
    }

    @Override
    public void shutdown() throws IOException {
        if (!isClosed()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("close the underlying socket");
            }

            try {
                // If conContext.isInputCloseNotified is false, close the
                // connection, no wait for more peer response.  Otherwise,
                // may wait for peer close_notify.
                closeSocket(conContext.isNegotiated &&
                        !conContext.isInputCloseNotified);
            } finally {
                tlsIsClosed = true;
            }
        }
    }

    @Override
    public String toString() {
        return "SSLSocket[" +
                "hostname=" + getPeerHost() +
                ", port=" + getPeerPort() +
                ", " + conContext.conSession +  // SSLSessionImpl.toString()
                "]";
    }

    private void closeSocket(boolean selfInitiated) throws IOException {
        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.fine("close the SSL connection " +
                (selfInitiated ? "(initiative)" : "(passive)"));
        }

        if (autoClose || !isLayered()) {
            // Try to clear the kernel buffer to avoid TCP connection resets.
            if (conContext.inputRecord instanceof
                    SSLSocketInputRecord inputRecord && isConnected) {
                if (appInput.readLock.tryLock()) {
                    try {
                        inputRecord.deplete(false);
                    } finally {
                        appInput.readLock.unlock();
                    }
                }
            }

            super.close();
        } else if (selfInitiated) {
            if (!conContext.isInboundClosed() && !isInputShutdown()) {
                // wait for close_notify alert to clear input stream.
                waitForClose();
            }
        }
    }

   /**
    * Wait for close_notify alert for a graceful closure.
    *
    * [RFC 5246] If the application protocol using TLS provides that any
    * data may be carried over the underlying transport after the TLS
    * connection is closed, the TLS implementation must receive the responding
    * close_notify alert before indicating to the application layer that
    * the TLS connection has ended.  If the application protocol will not
    * transfer any additional data, but will only close the underlying
    * transport connection, then the implementation MAY choose to close the
    * transport without waiting for the responding close_notify.
    */
    private void waitForClose() throws IOException {
        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.fine("wait for close_notify or alert");
        }

        appInput.readLock.lock();
        try {
            while (!conContext.isInboundClosed()) {
                try {
                    Plaintext plainText = decode(null);
                    // discard and continue
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.finest(
                                "discard plaintext while waiting for close",
                                plainText);
                    }
                } catch (Exception e) {   // including RuntimeException
                    handleException(e);
                }
            }
        } finally {
            appInput.readLock.unlock();
        }
    }
}
