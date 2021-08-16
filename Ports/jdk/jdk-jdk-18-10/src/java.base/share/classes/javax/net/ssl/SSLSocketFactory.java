/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.net.*;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.util.Locale;

import sun.security.action.GetPropertyAction;

/**
 * <code>SSLSocketFactory</code>s create <code>SSLSocket</code>s.
 *
 * @since 1.4
 * @see SSLSocket
 * @author David Brownell
 */
public abstract class SSLSocketFactory extends SocketFactory {
    static final boolean DEBUG;

    static {
        String s = GetPropertyAction.privilegedGetProperty(
                "javax.net.debug", "").toLowerCase(Locale.ENGLISH);
        DEBUG = s.contains("all") || s.contains("ssl");
    }

    /**
     * Constructor is used only by subclasses.
     */
    public SSLSocketFactory() {
        // blank
    }

    /**
     * Returns the default SSL socket factory.
     *
     * <p>The first time this method is called, the security property
     * "ssl.SocketFactory.provider" is examined. If it is non-null, a class by
     * that name is loaded and instantiated. If that is successful and the
     * object is an instance of SSLSocketFactory, it is made the default SSL
     * socket factory.
     *
     * <p>Otherwise, this method returns
     * <code>SSLContext.getDefault().getSocketFactory()</code>. If that
     * call fails, an inoperative factory is returned.
     *
     * @return the default <code>SocketFactory</code>
     * @see SSLContext#getDefault
     */
    public static SocketFactory getDefault() {
        if (DefaultFactoryHolder.defaultFactory != null) {
            return DefaultFactoryHolder.defaultFactory;
        }

        try {
            return SSLContext.getDefault().getSocketFactory();
        } catch (NoSuchAlgorithmException | UnsupportedOperationException e) {
            return new DefaultSSLSocketFactory(e);
        }
    }

    @SuppressWarnings("removal")
    static String getSecurityProperty(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public String run() {
                String s = java.security.Security.getProperty(name);
                if (s != null) {
                    s = s.trim();
                    if (s.isEmpty()) {
                        s = null;
                    }
                }
                return s;
            }
        });
    }

    /**
     * Returns the list of cipher suites which are enabled by default.
     * Unless a different list is enabled, handshaking on an SSL connection
     * will use one of these cipher suites.  The minimum quality of service
     * for these defaults requires confidentiality protection and server
     * authentication (that is, no anonymous cipher suites).
     * <P>
     * The returned array includes cipher suites from the list of standard
     * cipher suite names in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation, and may also
     * include other cipher suites that the provider supports.
     *
     * @see #getSupportedCipherSuites()
     * @return array of the cipher suites enabled by default
     */
    public abstract String [] getDefaultCipherSuites();

    /**
     * Returns the names of the cipher suites which could be enabled for use
     * on an SSL connection.  Normally, only a subset of these will actually
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
     * @see #getDefaultCipherSuites()
     * @return an array of cipher suite names
     */
    public abstract String [] getSupportedCipherSuites();

    /**
     * Returns a socket layered over an existing socket connected to the named
     * host, at the given port.  This constructor can be used when tunneling SSL
     * through a proxy or when negotiating the use of SSL over an existing
     * socket. The host and port refer to the logical peer destination.
     * This socket is configured using the socket options established for
     * this factory.
     *
     * @param s the existing socket
     * @param host the server host
     * @param port the server port
     * @param autoClose close the underlying socket when this socket is closed
     * @return a socket connected to the specified host and port
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws NullPointerException if the parameter s is null
     */
    public abstract Socket createSocket(Socket s, String host,
            int port, boolean autoClose) throws IOException;

    /**
     * Creates a server mode {@link Socket} layered over an
     * existing connected socket, and is able to read data which has
     * already been consumed/removed from the {@link Socket}'s
     * underlying {@link InputStream}.
     * <p>
     * This method can be used by a server application that needs to
     * observe the inbound data but still create valid SSL/TLS
     * connections: for example, inspection of Server Name Indication
     * (SNI) extensions (See section 3 of <A
     * HREF="http://www.ietf.org/rfc/rfc6066.txt">TLS Extensions
     * (RFC6066)</A>).  Data that has been already removed from the
     * underlying {@link InputStream} should be loaded into the
     * {@code consumed} stream before this method is called, perhaps
     * using a {@link java.io.ByteArrayInputStream}.  When this
     * {@link Socket} begins handshaking, it will read all of the data in
     * {@code consumed} until it reaches {@code EOF}, then all further
     * data is read from the underlying {@link InputStream} as
     * usual.
     * <p>
     * The returned socket is configured using the socket options
     * established for this factory, and is set to use server mode when
     * handshaking (see {@link SSLSocket#setUseClientMode(boolean)}).
     *
     * @param  s
     *         the existing socket
     * @param  consumed
     *         the consumed inbound network data that has already been
     *         removed from the existing {@link Socket}
     *         {@link InputStream}.  This parameter may be
     *         {@code null} if no data has been removed.
     * @param  autoClose close the underlying socket when this socket is closed.
     *
     * @return the {@link Socket} compliant with the socket options
     *         established for this factory
     *
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation
     * @throws NullPointerException if {@code s} is {@code null}
     *
     * @since 1.8
     */
    public Socket createSocket(Socket s, InputStream consumed,
            boolean autoClose) throws IOException {
        throw new UnsupportedOperationException();
    }

    // lazy initialization holder class idiom for static default factory
    //
    // See Effective Java Second Edition: Item 71.
    private static final class DefaultFactoryHolder {
        private static final SSLSocketFactory defaultFactory;

        static {
            SSLSocketFactory mediator = null;
            String clsName = getSecurityProperty("ssl.SocketFactory.provider");
            if (clsName != null) {
                log("setting up default SSLSocketFactory");
                try {
                    Class<?> cls = null;
                    try {
                        cls = Class.forName(clsName);
                    } catch (ClassNotFoundException e) {
                        ClassLoader cl = ClassLoader.getSystemClassLoader();
                        if (cl != null) {
                            cls = cl.loadClass(clsName);
                        }
                    }
                    log("class " + clsName + " is loaded");

                    mediator = (SSLSocketFactory)cls
                            .getDeclaredConstructor().newInstance();

                    log("instantiated an instance of class " + clsName);
                } catch (Exception e) {
                    log("SSLSocketFactory instantiation failed: " + e);
                    mediator = new DefaultSSLSocketFactory(e);
                }
            }

            defaultFactory = mediator;
        }

        private static void log(String msg) {
            if (DEBUG) {
                System.out.println(msg);
            }
        }
    }
}


// file private
class DefaultSSLSocketFactory extends SSLSocketFactory
{
    private Exception reason;

    DefaultSSLSocketFactory(Exception reason) {
        this.reason = reason;
    }

    private Socket throwException() throws SocketException {
        throw (SocketException)
            new SocketException(reason.toString()).initCause(reason);
    }

    @Override
    public Socket createSocket()
    throws IOException
    {
        return throwException();
    }

    @Override
    public Socket createSocket(String host, int port)
    throws IOException
    {
        return throwException();
    }

    @Override
    public Socket createSocket(Socket s, String host,
                                int port, boolean autoClose)
    throws IOException
    {
        return throwException();
    }

    @Override
    public Socket createSocket(InetAddress address, int port)
    throws IOException
    {
        return throwException();
    }

    @Override
    public Socket createSocket(String host, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException
    {
        return throwException();
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException
    {
        return throwException();
    }

    @Override
    public String [] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String [] getSupportedCipherSuites() {
        return new String[0];
    }
}
