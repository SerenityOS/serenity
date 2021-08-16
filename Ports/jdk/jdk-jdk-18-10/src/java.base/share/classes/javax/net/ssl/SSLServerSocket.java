/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;


/**
 * This class extends <code>ServerSocket</code> and
 * provides secure server sockets using protocols such as the Secure
 * Sockets Layer (SSL) or Transport Layer Security (TLS) protocols.
 * <P>
 * Instances of this class are generally created using an
 * <code>SSLServerSocketFactory</code>.  The primary function
 * of an <code>SSLServerSocket</code>
 * is to create <code>SSLSocket</code>s by <code>accept</code>ing
 * connections.
 * <P>
 * An <code>SSLServerSocket</code> contains several pieces of state data
 * which are inherited by the <code>SSLSocket</code> at
 * socket creation.  These include the enabled cipher
 * suites and protocols, whether client
 * authentication is necessary, and whether created sockets should
 * begin handshaking in client or server mode.  The state
 * inherited by the created <code>SSLSocket</code> can be
 * overridden by calling the appropriate methods.
 *
 * @see java.net.ServerSocket
 * @see SSLSocket
 *
 * @since 1.4
 * @author David Brownell
 */
public abstract class SSLServerSocket extends ServerSocket {

    /**
     * Used only by subclasses.
     * <P>
     * Create an unbound TCP server socket using the default authentication
     * context.
     *
     * @throws IOException if an I/O error occurs when creating the socket
     */
    protected SSLServerSocket()
    throws IOException
        { super(); }


    /**
     * Used only by subclasses.
     * <P>
     * Create a TCP server socket on a port, using the default
     * authentication context.  The connection backlog defaults to
     * fifty connections queued up before the system starts to
     * reject new connection requests.
     * <P>
     * A port number of <code>0</code> creates a socket on any free port.
     * <P>
     * If there is a security manager, its <code>checkListen</code>
     * method is called with the <code>port</code> argument as its
     * argument to ensure the operation is allowed. This could result
     * in a SecurityException.
     *
     * @param port the port on which to listen
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkListen</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see    SecurityManager#checkListen
     */
    protected SSLServerSocket(int port)
    throws IOException
        { super(port); }


    /**
     * Used only by subclasses.
     * <P>
     * Create a TCP server socket on a port, using the default
     * authentication context and a specified backlog of connections.
     * <P>
     * A port number of <code>0</code> creates a socket on any free port.
     * <P>
     * The <code>backlog</code> argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogther. The value provided
     * should be greater than <code>0</code>. If it is less than or equal to
     * <code>0</code>, then an implementation specific default will be used.
     * <P>
     * If there is a security manager, its <code>checkListen</code>
     * method is called with the <code>port</code> argument as its
     * argument to ensure the operation is allowed. This could result
     * in a SecurityException.
     *
     * @param port the port on which to listen
     * @param backlog  requested maximum length of the queue of incoming
     *                  connections.
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkListen</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see    SecurityManager#checkListen
     */
    protected SSLServerSocket(int port, int backlog)
    throws IOException
        { super(port, backlog); }


    /**
     * Used only by subclasses.
     * <P>
     * Create a TCP server socket on a port, using the default
     * authentication context and a specified backlog of connections
     * as well as a particular specified network interface.  This
     * constructor is used on multihomed hosts, such as those used
     * for firewalls or as routers, to control through which interface
     * a network service is provided.
     * <P>
     * If there is a security manager, its <code>checkListen</code>
     * method is called with the <code>port</code> argument as its
     * argument to ensure the operation is allowed. This could result
     * in a SecurityException.
     * <P>
     * A port number of <code>0</code> creates a socket on any free port.
     * <P>
     * The <code>backlog</code> argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogther. The value provided
     * should be greater than <code>0</code>. If it is less than or equal to
     * <code>0</code>, then an implementation specific default will be used.
     * <P>
     * If <i>address</i> is null, it will default accepting connections
     * on any/all local addresses.
     *
     * @param port the port on which to listen
     * @param backlog  requested maximum length of the queue of incoming
     *                  connections.
     * @param address the address of the network interface through
     *          which connections will be accepted
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkListen</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see    SecurityManager#checkListen
     */
    protected SSLServerSocket(int port, int backlog, InetAddress address)
    throws IOException
        { super(port, backlog, address); }



    /**
     * Returns the list of cipher suites which are currently enabled
     * for use by newly accepted connections.
     * <P>
     * If this list has not been explicitly modified, a system-provided
     * default guarantees a minimum quality of service in all enabled
     * cipher suites.
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
     * @return an array of cipher suites enabled
     * @see #getSupportedCipherSuites()
     * @see #setEnabledCipherSuites(String [])
     */
    public abstract String [] getEnabledCipherSuites();


    /**
     * Sets the cipher suites enabled for use by accepted connections.
     * <P>
     * The cipher suites must have been listed by getSupportedCipherSuites()
     * as being supported.  Following a successful call to this method,
     * only suites listed in the <code>suites</code> parameter are enabled
     * for use.
     * <P>
     * Suites that require authentication information which is not available
     * in this ServerSocket's authentication context will not be used
     * in any case, even if they are enabled.
     * <P>
     * Note that the standard list of cipher suite names may be found in the
     * <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation.  Providers
     * may support cipher suite names not found in this list or might not
     * use the recommended name for a certain cipher suite.
     * <P>
     * <code>SSLSocket</code>s returned from <code>accept()</code>
     * inherit this setting.
     *
     * @param suites Names of all the cipher suites to enable
     * @exception IllegalArgumentException when one or more of ciphers
     *          named by the parameter is not supported, or when
     *          the parameter is null.
     * @see #getSupportedCipherSuites()
     * @see #getEnabledCipherSuites()
     */
    public abstract void setEnabledCipherSuites(String suites []);


    /**
     * Returns the names of the cipher suites which could be enabled for use
     * on an SSL connection.
     * <P>
     * Normally, only a subset of these will actually
     * be enabled by default, since this list may include cipher suites which
     * do not meet quality of service requirements for those defaults.  Such
     * cipher suites are useful in specialized applications.
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
     * Returns the names of the protocols which could be enabled for use.
     *
     * @return an array of protocol names supported
     * @see #getEnabledProtocols()
     * @see #setEnabledProtocols(String [])
     */
    public abstract String [] getSupportedProtocols();


    /**
     * Returns the names of the protocols which are currently
     * enabled for use by the newly accepted connections.
     * <P>
     * Note that even if a protocol is enabled, it may never be used.
     * This can occur if the peer does not support the protocol, or its
     * use is restricted, or there are no enabled cipher suites supported
     * by the protocol.
     *
     * @return an array of protocol names
     * @see #getSupportedProtocols()
     * @see #setEnabledProtocols(String [])
     */
    public abstract String [] getEnabledProtocols();


    /**
     * Controls which particular protocols are enabled for use by
     * accepted connections.
     * <P>
     * The protocols must have been listed by
     * getSupportedProtocols() as being supported.
     * Following a successful call to this method, only protocols listed
     * in the <code>protocols</code> parameter are enabled for use.
     * <P>
     * <code>SSLSocket</code>s returned from <code>accept()</code>
     * inherit this setting.
     *
     * @param protocols Names of all the protocols to enable.
     * @exception IllegalArgumentException when one or more of
     *            the protocols named by the parameter is not supported or
     *            when the protocols parameter is null.
     * @see #getEnabledProtocols()
     * @see #getSupportedProtocols()
     */
    public abstract void setEnabledProtocols(String protocols[]);


    /**
     * Controls whether <code>accept</code>ed server-mode
     * <code>SSLSockets</code> will be initially configured to
     * <i>require</i> client authentication.
     * <P>
     * A socket's client authentication setting is one of the following:
     * <ul>
     * <li> client authentication required
     * <li> client authentication requested
     * <li> no client authentication desired
     * </ul>
     * <P>
     * Unlike {@link #setWantClientAuth(boolean)}, if the accepted
     * socket's option is set and the client chooses not to provide
     * authentication information about itself, <i>the negotiations
     * will stop and the connection will be dropped</i>.
     * <P>
     * Calling this method overrides any previous setting made by
     * this method or {@link #setWantClientAuth(boolean)}.
     * <P>
     * The initial inherited setting may be overridden by calling
     * {@link SSLSocket#setNeedClientAuth(boolean)} or
     * {@link SSLSocket#setWantClientAuth(boolean)}.
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
     * Returns true if client authentication will be <i>required</i> on
     * newly <code>accept</code>ed server-mode <code>SSLSocket</code>s.
     * <P>
     * The initial inherited setting may be overridden by calling
     * {@link SSLSocket#setNeedClientAuth(boolean)} or
     * {@link SSLSocket#setWantClientAuth(boolean)}.
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
     * Controls whether <code>accept</code>ed server-mode
     * <code>SSLSockets</code> will be initially configured to
     * <i>request</i> client authentication.
     * <P>
     * A socket's client authentication setting is one of the following:
     * <ul>
     * <li> client authentication required
     * <li> client authentication requested
     * <li> no client authentication desired
     * </ul>
     * <P>
     * Unlike {@link #setNeedClientAuth(boolean)}, if the accepted
     * socket's option is set and the client chooses not to provide
     * authentication information about itself, <i>the negotiations
     * will continue</i>.
     * <P>
     * Calling this method overrides any previous setting made by
     * this method or {@link #setNeedClientAuth(boolean)}.
     * <P>
     * The initial inherited setting may be overridden by calling
     * {@link SSLSocket#setNeedClientAuth(boolean)} or
     * {@link SSLSocket#setWantClientAuth(boolean)}.
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
     * Returns true if client authentication will be <i>requested</i> on
     * newly accepted server-mode connections.
     * <P>
     * The initial inherited setting may be overridden by calling
     * {@link SSLSocket#setNeedClientAuth(boolean)} or
     * {@link SSLSocket#setWantClientAuth(boolean)}.
     *
     * @return  true if client authentication is requested,
     *          or false if no client authentication is desired.
     * @see #setWantClientAuth(boolean)
     * @see #setNeedClientAuth(boolean)
     * @see #getNeedClientAuth()
     * @see #setUseClientMode(boolean)
     */
    public abstract boolean getWantClientAuth();


    /**
     * Controls whether accepted connections are in the (default) SSL
     * server mode, or the SSL client mode.
     * <P>
     * Servers normally authenticate themselves, and clients are not
     * required to do so.
     * <P>
     * In rare cases, TCP servers
     * need to act in the SSL client mode on newly accepted
     * connections. For example, FTP clients acquire server sockets
     * and listen there for reverse connections from the server. An
     * FTP client would use an SSLServerSocket in "client" mode to
     * accept the reverse connection while the FTP server uses an
     * SSLSocket with "client" mode disabled to initiate the
     * connection. During the resulting handshake, existing SSL
     * sessions may be reused.
     * <P>
     * <code>SSLSocket</code>s returned from <code>accept()</code>
     * inherit this setting.
     *
     * @param mode true if newly accepted connections should use SSL
     *          client mode.
     * @see #getUseClientMode()
     */
    public abstract void setUseClientMode(boolean mode);


    /**
     * Returns true if accepted connections will be in SSL client mode.
     *
     * @see #setUseClientMode(boolean)
     * @return true if the connection should use SSL client mode.
     */
    public abstract boolean getUseClientMode();


    /**
     * Controls whether new SSL sessions may be established by the
     * sockets which are created from this server socket.
     * <P>
     * <code>SSLSocket</code>s returned from <code>accept()</code>
     * inherit this setting.
     *
     * @param flag true indicates that sessions may be created; this
     *          is the default. false indicates that an existing session
     *          must be resumed.
     * @see #getEnableSessionCreation()
     */
    public abstract void setEnableSessionCreation(boolean flag);


    /**
     * Returns true if new SSL sessions may be established by the
     * sockets which are created from this server socket.
     *
     * @return true indicates that sessions may be created; this
     *          is the default.  false indicates that an existing
     *          session must be resumed
     * @see #setEnableSessionCreation(boolean)
     */
    public abstract boolean getEnableSessionCreation();

    /**
     * Returns the SSLParameters in effect for newly accepted connections.
     * The ciphersuites and protocols of the returned SSLParameters
     * are always non-null.
     *
     * @return the SSLParameters in effect for newly accepted connections
     *
     * @see #setSSLParameters(SSLParameters)
     *
     * @since 1.7
     */
    public SSLParameters getSSLParameters() {
        SSLParameters parameters = new SSLParameters();

        parameters.setCipherSuites(getEnabledCipherSuites());
        parameters.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            parameters.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            parameters.setWantClientAuth(true);
        }

        return parameters;
    }

    /**
     * Applies SSLParameters to newly accepted connections.
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
     *
     * @see #getSSLParameters()
     *
     * @since 1.7
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

}
