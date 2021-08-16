/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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


package javax.net.ssl;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.function.BiFunction;

/**
 * This class extends <code>Socket</code> and provides secure
 * sockets using protocols such as the "Secure
 * Sockets Layer" (SSL) or IETF "Transport Layer Security" (TLS) protocols.
 * <P>
 * Such sockets are normal stream sockets, but they
 * add a layer of security protections over the underlying network transport
 * protocol, such as TCP.  Those protections include: <UL>
 *
 *      <LI> <em>Integrity Protection</em>.  SSL protects against
 *      modification of messages by an active wiretapper.
 *
 *      <LI> <em>Authentication</em>.  In most modes, SSL provides
 *      peer authentication.  Servers are usually authenticated,
 *      and clients may be authenticated as requested by servers.
 *
 *      <LI> <em>Confidentiality (Privacy Protection)</em>.  In most
 *      modes, SSL encrypts data being sent between client and server.
 *      This protects the confidentiality of data, so that passive
 *      wiretappers won't see sensitive data such as financial
 *      information or personal information of many kinds.
 *
 *      </UL>
 *
 * <P>These kinds of protection are specified by a "cipher suite", which
 * is a combination of cryptographic algorithms used by a given SSL connection.
 * During the negotiation process, the two endpoints must agree on
 * a ciphersuite that is available in both environments.
 * If there is no such suite in common, no SSL connection can
 * be established, and no data can be exchanged.
 *
 * <P> The cipher suite used is established by a negotiation process
 * called "handshaking".  The goal of this
 * process is to create or rejoin a "session", which may protect many
 * connections over time.  After handshaking has completed, you can access
 * session attributes by using the <em>getSession</em> method.
 * The initial handshake on this connection can be initiated in
 * one of three ways: <UL>
 *
 *      <LI> calling <code>startHandshake</code> which explicitly
 *              begins handshakes, or
 *      <LI> any attempt to read or write application data on
 *              this socket causes an implicit handshake, or
 *      <LI> a call to <code>getSession</code> tries to set up a session
 *              if there is no currently valid session, and
 *              an implicit handshake is done.
 * </UL>
 *
 * <P>If handshaking fails for any reason, the <code>SSLSocket</code>
 * is closed, and no further communications can be done.
 *
 * <P>There are two groups of cipher suites which you will need to know
 * about when managing cipher suites: <UL>
 *
 *      <LI> <em>Supported</em> cipher suites:  all the suites which are
 *      supported by the SSL implementation.  This list is reported
 *      using <em>getSupportedCipherSuites</em>.
 *
 *      <LI> <em>Enabled</em> cipher suites, which may be fewer
 *      than the full set of supported suites.  This group is
 *      set using the <em>setEnabledCipherSuites</em> method, and
 *      queried using the <em>getEnabledCipherSuites</em> method.
 *      Initially, a default set of cipher suites will be enabled on
 *      a new socket that represents the minimum suggested configuration.
 *
 *      </UL>
 *
 * <P> Implementation defaults require that only cipher
 * suites which authenticate servers and provide confidentiality
 * be enabled by default.
 * Only if both sides explicitly agree to unauthenticated and/or
 * non-private (unencrypted) communications will such a ciphersuite be
 * selected.
 *
 * <P>When an <code>SSLSocket</code> is first created, no handshaking
 * is done so that applications may first set their communication
 * preferences:  what cipher suites to use, whether the socket should be
 * in client or server mode, etc.
 * However, security is always provided by the time that application data
 * is sent over the connection.
 *
 * <P> You may register to receive event notification of handshake
 * completion.  This involves
 * the use of two additional classes.  <em>HandshakeCompletedEvent</em>
 * objects are passed to <em>HandshakeCompletedListener</em> instances,
 * which are registered by users of this API.
 *
 * An <code>SSLSocket</code> is created by <code>SSLSocketFactory</code>,
 * or by <code>accept</code>ing a connection from a
 * <code>SSLServerSocket</code>.
 *
 * <P>A SSL socket must choose to operate in the client or server mode.
 * This will determine who begins the handshaking process, as well
 * as which messages should be sent by each party.  Each
 * connection must have one client and one server, or handshaking
 * will not progress properly.  Once the initial handshaking has started, a
 * socket can not switch between client and server modes, even when
 * performing renegotiations.
 *
 * <P> The ApplicationProtocol {@code String} values returned by the methods
 * in this class are in the network byte representation sent by the peer.
 * The bytes could be directly compared, or converted to its Unicode
 * {code String} format for comparison.
 *
 * <blockquote><pre>
 *     String networkString = sslSocket.getHandshakeApplicationProtocol();
 *     byte[] bytes = networkString.getBytes(StandardCharsets.ISO_8859_1);
 *
 *     //
 *     // Match using bytes:
 *     //
 *     //   "http/1.1"                       (7-bit ASCII values same in UTF-8)
 *     //   MEETEI MAYEK LETTERS "HUK UN I"  (Unicode 0xabcd->0xabcf)
 *     //
 *     String HTTP1_1 = "http/1.1";
 *     byte[] HTTP1_1_BYTES = HTTP1_1.getBytes(StandardCharsets.UTF_8);
 *
 *     byte[] HUK_UN_I_BYTES = new byte[] {
 *         (byte) 0xab, (byte) 0xcd,
 *         (byte) 0xab, (byte) 0xce,
 *         (byte) 0xab, (byte) 0xcf};
 *
 *     if ((Arrays.compare(bytes, HTTP1_1_BYTES) == 0 )
 *             || Arrays.compare(bytes, HUK_UN_I_BYTES) == 0) {
 *        ...
 *     }
 *
 *     //
 *     // Alternatively match using string.equals() if we know the ALPN value
 *     // was encoded from a {@code String} using a certain character set,
 *     // for example {@code UTF-8}.  The ALPN value must first be properly
 *     // decoded to a Unicode {@code String} before use.
 *     //
 *     String unicodeString = new String(bytes, StandardCharsets.UTF_8);
 *     if (unicodeString.equals(HTTP1_1)
 *             || unicodeString.equals("\u005cuabcd\u005cuabce\u005cuabcf")) {
 *         ...
 *     }
 * </pre></blockquote>
 *
 * @apiNote
 * When the connection is no longer needed, the client and server
 * applications should each close both sides of their respective connection.
 * For {@code SSLSocket} objects, for example, an application can call
 * {@link Socket#shutdownOutput()} or {@link java.io.OutputStream#close()}
 * for output stream close and call {@link Socket#shutdownInput()} or
 * {@link java.io.InputStream#close()} for input stream close.  Note that
 * in some cases, closing the input stream may depend on the peer's output
 * stream being closed first.  If the connection is not closed in an orderly
 * manner (for example {@link Socket#shutdownInput()} is called before the
 * peer's write closure notification has been received), exceptions may
 * be raised to indicate that an error has occurred.  Once an
 * {@code SSLSocket} is closed, it is not reusable: a new {@code SSLSocket}
 * must be created.
 *
 * @see java.net.Socket
 * @see SSLServerSocket
 * @see SSLSocketFactory
 *
 * @since 1.4
 * @author David Brownell
 */
public abstract class SSLSocket extends Socket
{
    /**
     * Used only by subclasses.
     * Constructs an uninitialized, unconnected TCP socket.
     */
    protected SSLSocket()
        { super(); }


    /**
     * Used only by subclasses.
     * Constructs a TCP connection to a named host at a specified port.
     * This acts as the SSL client.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host name of the host with which to connect, or
     *        <code>null</code> for the loopback address.
     * @param port number of the server's port
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws UnknownHostException if the host is not known
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see SecurityManager#checkConnect
     */
    protected SSLSocket(String host, int port)
    throws IOException, UnknownHostException
        { super(host, port); }


    /**
     * Used only by subclasses.
     * Constructs a TCP connection to a server at a specified address
     * and port.  This acts as the SSL client.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param address the server's host
     * @param port its port
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @throws NullPointerException if <code>address</code> is null.
     * @see SecurityManager#checkConnect
     */
    protected SSLSocket(InetAddress address, int port)
    throws IOException
        { super(address, port); }


    /**
     * Used only by subclasses.
     * Constructs an SSL connection to a named host at a specified port,
     * binding the client side of the connection a given address and port.
     * This acts as the SSL client.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host name of the host with which to connect, or
     *        <code>null</code> for the loopback address.
     * @param port number of the server's port
     * @param clientAddress the client's address the socket is bound to, or
     *        <code>null</code> for the <code>anyLocal</code> address.
     * @param clientPort the client's port the socket is bound to, or
     *        <code>zero</code> for a system selected free port.
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws UnknownHostException if the host is not known
     * @throws IllegalArgumentException if the port parameter or clientPort
     *         parameter is outside the specified range of valid port values,
     *         which is between 0 and 65535, inclusive.
     * @see SecurityManager#checkConnect
     */
    protected SSLSocket(String host, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException, UnknownHostException
        { super(host, port, clientAddress, clientPort); }


    /**
     * Used only by subclasses.
     * Constructs an SSL connection to a server at a specified address
     * and TCP port, binding the client side of the connection a given
     * address and port.  This acts as the SSL client.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param address the server's host
     * @param port its port
     * @param clientAddress the client's address the socket is bound to, or
     *        <code>null</code> for the <code>anyLocal</code> address.
     * @param clientPort the client's port the socket is bound to, or
     *        <code>zero</code> for a system selected free port.
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter or clientPort
     *         parameter is outside the specified range of valid port values,
     *         which is between 0 and 65535, inclusive.
     * @throws NullPointerException if <code>address</code> is null.
     * @see SecurityManager#checkConnect
     */
    protected SSLSocket(InetAddress address, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException
        { super(address, port, clientAddress, clientPort); }


    /**
     * Returns the names of the cipher suites which could be enabled for use
     * on this connection.  Normally, only a subset of these will actually
     * be enabled by default, since this list may include cipher suites which
     * do not meet quality of service requirements for those defaults.  Such
     * cipher suites might be useful in specialized applications.
     * <P>
     * The returned array includes cipher suites from the list of standard
     * cipher suite names in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation, and may also
     * include other cipher suites that the provider supports.
     *
     * @return an array of cipher suite names
     * @see #getEnabledCipherSuites()
     * @see #setEnabledCipherSuites(String [])
     */
    public abstract String [] getSupportedCipherSuites();


    /**
     * Returns the names of the SSL cipher suites which are currently
     * enabled for use on this connection.  When an SSLSocket is first
     * created, all enabled cipher suites support a minimum quality of
     * service.  Thus, in some environments this value might be empty.
     * <P>
     * Note that even if a suite is enabled, it may never be used. This
     * can occur if the peer does not support it, or its use is restricted,
     * or the requisite certificates (and private keys) for the suite are
     * not available, or an anonymous suite is enabled but authentication
     * is required.
     * <P>
     * The returned array includes cipher suites from the list of standard
     * cipher suite names in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation, and may also
     * include other cipher suites that the provider supports.
     *
     * @return an array of cipher suite names
     * @see #getSupportedCipherSuites()
     * @see #setEnabledCipherSuites(String [])
     */
    public abstract String [] getEnabledCipherSuites();


    /**
     * Sets the cipher suites enabled for use on this connection.
     * <P>
     * Each cipher suite in the <code>suites</code> parameter must have
     * been listed by getSupportedCipherSuites(), or the method will
     * fail.  Following a successful call to this method, only suites
     * listed in the <code>suites</code> parameter are enabled for use.
     * <P>
     * Note that the standard list of cipher suite names may be found in the
     * <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation.  Providers
     * may support cipher suite names not found in this list or might not
     * use the recommended name for a certain cipher suite.
     * <P>
     * See {@link #getEnabledCipherSuites()} for more information
     * on why a specific ciphersuite may never be used on a connection.
     *
     * @param suites Names of all the cipher suites to enable
     * @throws IllegalArgumentException when one or more of the ciphers
     *          named by the parameter is not supported, or when the
     *          parameter is null.
     * @see #getSupportedCipherSuites()
     * @see #getEnabledCipherSuites()
     */
    public abstract void setEnabledCipherSuites(String suites []);


    /**
     * Returns the names of the protocols which could be enabled for use
     * on an SSL connection.
     *
     * @return an array of protocols supported
     */
    public abstract String [] getSupportedProtocols();


    /**
     * Returns the names of the protocol versions which are currently
     * enabled for use on this connection.
     * <P>
     * Note that even if a protocol is enabled, it may never be used.
     * This can occur if the peer does not support the protocol, or its
     * use is restricted, or there are no enabled cipher suites supported
     * by the protocol.
     *
     * @see #setEnabledProtocols(String [])
     * @return an array of protocols
     */
    public abstract String [] getEnabledProtocols();


    /**
     * Sets the protocol versions enabled for use on this connection.
     * <P>
     * The protocols must have been listed by
     * <code>getSupportedProtocols()</code> as being supported.
     * Following a successful call to this method, only protocols listed
     * in the <code>protocols</code> parameter are enabled for use.
     *
     * @param protocols Names of all the protocols to enable.
     * @throws IllegalArgumentException when one or more of
     *            the protocols named by the parameter is not supported or
     *            when the protocols parameter is null.
     * @see #getEnabledProtocols()
     */
    public abstract void setEnabledProtocols(String protocols[]);


    /**
     * Returns the SSL Session in use by this connection.  These can
     * be long lived, and frequently correspond to an entire login session
     * for some user.  The session specifies a particular cipher suite
     * which is being actively used by all connections in that session,
     * as well as the identities of the session's client and server.
     * <P>
     * This method will initiate the initial handshake if
     * necessary and then block until the handshake has been
     * established.
     * <P>
     * If an error occurs during the initial handshake, this method
     * returns an invalid session object which reports an invalid
     * cipher suite of "SSL_NULL_WITH_NULL_NULL".
     *
     * @return the <code>SSLSession</code>
     */
    public abstract SSLSession getSession();


    /**
     * Returns the {@code SSLSession} being constructed during a SSL/TLS
     * handshake.
     * <p>
     * TLS protocols may negotiate parameters that are needed when using
     * an instance of this class, but before the {@code SSLSession} has
     * been completely initialized and made available via {@code getSession}.
     * For example, the list of valid signature algorithms may restrict
     * the type of certificates that can be used during TrustManager
     * decisions, or the maximum TLS fragment packet sizes can be
     * resized to better support the network environment.
     * <p>
     * This method provides early access to the {@code SSLSession} being
     * constructed.  Depending on how far the handshake has progressed,
     * some data may not yet be available for use.  For example, if a
     * remote server will be sending a Certificate chain, but that chain
     * has yet not been processed, the {@code getPeerCertificates}
     * method of {@code SSLSession} will throw a
     * SSLPeerUnverifiedException.  Once that chain has been processed,
     * {@code getPeerCertificates} will return the proper value.
     * <p>
     * Unlike {@link #getSession()}, this method does not initiate the
     * initial handshake and does not block until handshaking is
     * complete.
     *
     * @see SSLEngine
     * @see SSLSession
     * @see ExtendedSSLSession
     * @see X509ExtendedKeyManager
     * @see X509ExtendedTrustManager
     *
     * @return null if this instance is not currently handshaking, or
     *         if the current handshake has not progressed far enough to
     *         create a basic SSLSession.  Otherwise, this method returns the
     *         {@code SSLSession} currently being negotiated.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     *
     * @since 1.7
     */
    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException();
    }


    /**
     * Registers an event listener to receive notifications that an
     * SSL handshake has completed on this connection.
     *
     * @param listener the HandShake Completed event listener
     * @see #startHandshake()
     * @see #removeHandshakeCompletedListener(HandshakeCompletedListener)
     * @throws IllegalArgumentException if the argument is null.
     */
    public abstract void addHandshakeCompletedListener(
        HandshakeCompletedListener listener);


    /**
     * Removes a previously registered handshake completion listener.
     *
     * @param listener the HandShake Completed event listener
     * @throws IllegalArgumentException if the listener is not registered,
     * or the argument is null.
     * @see #addHandshakeCompletedListener(HandshakeCompletedListener)
     */
    public abstract void removeHandshakeCompletedListener(
        HandshakeCompletedListener listener);


    /**
     * Starts an SSL handshake on this connection.  Common reasons include
     * a need to use new encryption keys, to change cipher suites, or to
     * initiate a new session.  To force complete reauthentication, the
     * current session could be invalidated before starting this handshake.
     *
     * <P> If data has already been sent on the connection, it continues
     * to flow during this handshake.  When the handshake completes, this
     * will be signaled with an event.
     *
     * This method is synchronous for the initial handshake on a connection
     * and returns when the negotiated handshake is complete. Some
     * protocols may not support multiple handshakes on an existing socket
     * and may throw an IOException.
     *
     * @throws IOException on a network level error
     * @see #addHandshakeCompletedListener(HandshakeCompletedListener)
     */
    public abstract void startHandshake() throws IOException;


    /**
     * Configures the socket to use client (or server) mode when
     * handshaking.
     * <P>
     * This method must be called before any handshaking occurs.
     * Once handshaking has begun, the mode can not be reset for the
     * life of this socket.
     * <P>
     * Servers normally authenticate themselves, and clients
     * are not required to do so.
     *
     * @param mode true if the socket should start its handshaking
     *          in "client" mode
     * @throws IllegalArgumentException if a mode change is attempted
     *          after the initial handshake has begun.
     * @see #getUseClientMode()
     */
    public abstract void setUseClientMode(boolean mode);


    /**
     * Returns true if the socket is set to use client mode when
     * handshaking.
     *
     * @return true if the socket should do handshaking
     *          in "client" mode
     * @see #setUseClientMode(boolean)
     */
    public abstract boolean getUseClientMode();


    /**
     * Configures the socket to <i>require</i> client authentication.  This
     * option is only useful for sockets in the server mode.
     * <P>
     * A socket's client authentication setting is one of the following:
     * <ul>
     * <li> client authentication required
     * <li> client authentication requested
     * <li> no client authentication desired
     * </ul>
     * <P>
     * Unlike {@link #setWantClientAuth(boolean)}, if this option is set and
     * the client chooses not to provide authentication information
     * about itself, <i>the negotiations will stop and the connection
     * will be dropped</i>.
     * <P>
     * Calling this method overrides any previous setting made by
     * this method or {@link #setWantClientAuth(boolean)}.
     *
     * @param   need set to true if client authentication is required,
     *          or false if no client authentication is desired.
     * @see #getNeedClientAuth()
     * @see #setWantClientAuth(boolean)
     * @see #getWantClientAuth()
     * @see #setUseClientMode(boolean)
     */
    public abstract void setNeedClientAuth(boolean need);


    /**
     * Returns true if the socket will <i>require</i> client authentication.
     * This option is only useful to sockets in the server mode.
     *
     * @return  true if client authentication is required,
     *          or false if no client authentication is desired.
     * @see #setNeedClientAuth(boolean)
     * @see #setWantClientAuth(boolean)
     * @see #getWantClientAuth()
     * @see #setUseClientMode(boolean)
     */
    public abstract boolean getNeedClientAuth();


    /**
     * Configures the socket to <i>request</i> client authentication.
     * This option is only useful for sockets in the server mode.
     * <P>
     * A socket's client authentication setting is one of the following:
     * <ul>
     * <li> client authentication required
     * <li> client authentication requested
     * <li> no client authentication desired
     * </ul>
     * <P>
     * Unlike {@link #setNeedClientAuth(boolean)}, if this option is set and
     * the client chooses not to provide authentication information
     * about itself, <i>the negotiations will continue</i>.
     * <P>
     * Calling this method overrides any previous setting made by
     * this method or {@link #setNeedClientAuth(boolean)}.
     *
     * @param   want set to true if client authentication is requested,
     *          or false if no client authentication is desired.
     * @see #getWantClientAuth()
     * @see #setNeedClientAuth(boolean)
     * @see #getNeedClientAuth()
     * @see #setUseClientMode(boolean)
     */
    public abstract void setWantClientAuth(boolean want);


    /**
     * Returns true if the socket will <i>request</i> client authentication.
     * This option is only useful for sockets in the server mode.
     *
     * @return  true if client authentication is requested,
     *          or false if no client authentication is desired.
     * @see #setNeedClientAuth(boolean)
     * @see #getNeedClientAuth()
     * @see #setWantClientAuth(boolean)
     * @see #setUseClientMode(boolean)
     */
    public abstract boolean getWantClientAuth();


    /**
     * Controls whether new SSL sessions may be established by this socket.
     * If session creations are not allowed, and there are no
     * existing sessions to resume, there will be no successful
     * handshaking.
     *
     * @param flag true indicates that sessions may be created; this
     *          is the default.  false indicates that an existing session
     *          must be resumed
     * @see #getEnableSessionCreation()
     */
    public abstract void setEnableSessionCreation(boolean flag);


    /**
     * Returns true if new SSL sessions may be established by this socket.
     *
     * @return true indicates that sessions may be created; this
     *          is the default.  false indicates that an existing session
     *          must be resumed
     * @see #setEnableSessionCreation(boolean)
     */
    public abstract boolean getEnableSessionCreation();

    /**
     * Returns the SSLParameters in effect for this SSLSocket.
     * The ciphersuites and protocols of the returned SSLParameters
     * are always non-null.
     *
     * @return the SSLParameters in effect for this SSLSocket.
     * @since 1.6
     */
    public SSLParameters getSSLParameters() {
        SSLParameters params = new SSLParameters();
        params.setCipherSuites(getEnabledCipherSuites());
        params.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            params.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            params.setWantClientAuth(true);
        }
        return params;
    }

    /**
     * Applies SSLParameters to this socket.
     *
     * <p>This means:
     * <ul>
     * <li>If {@code params.getCipherSuites()} is non-null,
     *   {@code setEnabledCipherSuites()} is called with that value.</li>
     * <li>If {@code params.getProtocols()} is non-null,
     *   {@code setEnabledProtocols()} is called with that value.</li>
     * <li>If {@code params.getNeedClientAuth()} or
     *   {@code params.getWantClientAuth()} return {@code true},
     *   {@code setNeedClientAuth(true)} and
     *   {@code setWantClientAuth(true)} are called, respectively;
     *   otherwise {@code setWantClientAuth(false)} is called.</li>
     * <li>If {@code params.getServerNames()} is non-null, the socket will
     *   configure its server names with that value.</li>
     * <li>If {@code params.getSNIMatchers()} is non-null, the socket will
     *   configure its SNI matchers with that value.</li>
     * </ul>
     *
     * @param params the parameters
     * @throws IllegalArgumentException if the setEnabledCipherSuites() or
     *    the setEnabledProtocols() call fails
     * @since 1.6
     */
    public void setSSLParameters(SSLParameters params) {
        String[] s;
        s = params.getCipherSuites();
        if (s != null) {
            setEnabledCipherSuites(s);
        }
        s = params.getProtocols();
        if (s != null) {
            setEnabledProtocols(s);
        }
        if (params.getNeedClientAuth()) {
            setNeedClientAuth(true);
        } else if (params.getWantClientAuth()) {
            setWantClientAuth(true);
        } else {
            setWantClientAuth(false);
        }
    }

    /**
     * Returns the most recent application protocol value negotiated for this
     * connection.
     * <p>
     * If supported by the underlying SSL/TLS/DTLS implementation,
     * application name negotiation mechanisms such as <a
     * href="http://www.ietf.org/rfc/rfc7301.txt"> RFC 7301 </a>, the
     * Application-Layer Protocol Negotiation (ALPN), can negotiate
     * application-level values between peers.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @return null if it has not yet been determined if application
     *         protocols might be used for this connection, an empty
     *         {@code String} if application protocols values will not
     *         be used, or a non-empty application protocol {@code String}
     *         if a value was successfully negotiated.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public String getApplicationProtocol() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the application protocol value negotiated on a SSL/TLS
     * handshake currently in progress.
     * <p>
     * Like {@link #getHandshakeSession()},
     * a connection may be in the middle of a handshake. The
     * application protocol may or may not yet be available.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @return null if it has not yet been determined if application
     *         protocols might be used for this handshake, an empty
     *         {@code String} if application protocols values will not
     *         be used, or a non-empty application protocol {@code String}
     *         if a value was successfully negotiated.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public String getHandshakeApplicationProtocol() {
        throw new UnsupportedOperationException();
    }


    /**
     * Registers a callback function that selects an application protocol
     * value for a SSL/TLS/DTLS handshake.
     * The function overrides any values supplied using
     * {@link SSLParameters#setApplicationProtocols
     * SSLParameters.setApplicationProtocols} and it supports the following
     * type parameters:
     * <blockquote>
     * <dl>
     * <dt> {@code SSLSocket}
     * <dd> The function's first argument allows the current {@code SSLSocket}
     *      to be inspected, including the handshake session and configuration
     *      settings.
     * <dt> {@code List<String>}
     * <dd> The function's second argument lists the application protocol names
     *      advertised by the TLS peer.
     * <dt> {@code String}
     * <dd> The function's result is an application protocol name, or null to
     *      indicate that none of the advertised names are acceptable.
     *      If the return value is an empty {@code String} then application
     *      protocol indications will not be used.
     *      If the return value is null (no value chosen) or is a value that
     *      was not advertised by the peer, the underlying protocol will
     *      determine what action to take. (For example, ALPN will send a
     *      "no_application_protocol" alert and terminate the connection.)
     * </dl>
     * </blockquote>
     *
     * For example, the following call registers a callback function that
     * examines the TLS handshake parameters and selects an application protocol
     * name:
     * <pre>{@code
     *     serverSocket.setHandshakeApplicationProtocolSelector(
     *         (serverSocket, clientProtocols) -> {
     *             SSLSession session = serverSocket.getHandshakeSession();
     *             return chooseApplicationProtocol(
     *                 serverSocket,
     *                 clientProtocols,
     *                 session.getProtocol(),
     *                 session.getCipherSuite());
     *         });
     * }</pre>
     *
     * @apiNote
     * This method should be called by TLS server applications before the TLS
     * handshake begins. Also, this {@code SSLSocket} should be configured with
     * parameters that are compatible with the application protocol selected by
     * the callback function. For example, enabling a poor choice of cipher
     * suites could result in no suitable application protocol.
     * See {@link SSLParameters}.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @param selector the callback function, or null to de-register.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLSocket, List<String>, String> selector) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the callback function that selects an application protocol
     * value during a SSL/TLS/DTLS handshake.
     * See {@link #setHandshakeApplicationProtocolSelector
     * setHandshakeApplicationProtocolSelector}
     * for the function's type parameters.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @return the callback function, or null if none has been set.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public BiFunction<SSLSocket, List<String>, String>
            getHandshakeApplicationProtocolSelector() {
        throw new UnsupportedOperationException();
    }
}
