/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import sun.security.jca.GetInstance;

/**
 * Instances of this class represent a secure socket protocol
 * implementation which acts as a factory for secure socket
 * factories or {@code SSLEngine}s. This class is initialized
 * with an optional set of key and trust managers and source of
 * secure random bytes.
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard {@code SSLContext} protocol:
 * <ul>
 * <li>{@code TLSv1.2}</li>
 * </ul>
 * This protocol is described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#sslcontext-algorithms">
 * SSLContext section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other protocols are supported.
 *
 * @since 1.4
 */
public class SSLContext {
    private final Provider provider;

    private final SSLContextSpi contextSpi;

    private final String protocol;

    private static volatile SSLContext defaultContext;

    private static final VarHandle VH_DEFAULT_CONTEXT;

    static {
        try {
            VH_DEFAULT_CONTEXT = MethodHandles.lookup()
                .findStaticVarHandle(
                    SSLContext.class, "defaultContext", SSLContext.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Creates an SSLContext object.
     *
     * @param contextSpi the delegate
     * @param provider the provider
     * @param protocol the protocol
     */
    protected SSLContext(SSLContextSpi contextSpi, Provider provider,
            String protocol) {
        this.contextSpi = contextSpi;
        this.provider = provider;
        this.protocol = protocol;
    }

    /**
     * Returns the default SSL context.
     *
     * <p>If a default context was set using the {@link #setDefault
     * SSLContext.setDefault()} method, it is returned. Otherwise, the first
     * call of this method triggers the call
     * {@code SSLContext.getInstance("Default")}.
     * If successful, that object is made the default SSL context and returned.
     *
     * <p>The default context is immediately
     * usable and does not require {@linkplain #init initialization}.
     *
     * @return the default SSL context
     * @throws NoSuchAlgorithmException if the
     *   {@link SSLContext#getInstance SSLContext.getInstance()} call fails
     * @since 1.6
     */
    public static SSLContext getDefault() throws NoSuchAlgorithmException {
        SSLContext temporaryContext = defaultContext;
        if (temporaryContext == null) {
            temporaryContext = SSLContext.getInstance("Default");
            if (!VH_DEFAULT_CONTEXT.compareAndSet(null, temporaryContext)) {
                temporaryContext = defaultContext;
            }
        }

        return temporaryContext;
    }

    /**
     * Sets the default SSL context. It will be returned by subsequent calls
     * to {@link #getDefault}. The default context must be immediately usable
     * and not require {@linkplain #init initialization}.
     *
     * @param context the SSLContext
     * @throws  NullPointerException if context is null
     * @throws  SecurityException if a security manager exists and its
     *          {@code checkPermission} method does not allow
     *          {@code SSLPermission("setDefaultSSLContext")}
     * @since 1.6
     */
    public static void setDefault(SSLContext context) {
        if (context == null) {
            throw new NullPointerException();
        }
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SSLPermission("setDefaultSSLContext"));
        }

        defaultContext = context;
    }

    /**
     * Returns a {@code SSLContext} object that implements the
     * specified secure socket protocol.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new SSLContext object encapsulating the
     * SSLContextSpi implementation from the first
     * Provider that supports the specified protocol is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param protocol the standard name of the requested protocol.
     *          See the SSLContext section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#sslcontext-algorithms">
     *          Java Security Standard Algorithm Names Specification</a>
     *          for information about standard protocol names.
     *
     * @return the new {@code SSLContext} object
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code SSLContextSpi} implementation for the
     *         specified protocol
     *
     * @throws NullPointerException if {@code protocol} is {@code null}
     *
     * @see java.security.Provider
     */
    public static SSLContext getInstance(String protocol)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(protocol, "null protocol name");
        GetInstance.Instance instance = GetInstance.getInstance
                ("SSLContext", SSLContextSpi.class, protocol);
        return new SSLContext((SSLContextSpi)instance.impl, instance.provider,
                protocol);
    }

    /**
     * Returns a {@code SSLContext} object that implements the
     * specified secure socket protocol.
     *
     * <p> A new SSLContext object encapsulating the
     * SSLContextSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param protocol the standard name of the requested protocol.
     *          See the SSLContext section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#sslcontext-algorithms">
     *          Java Security Standard Algorithm Names Specification</a>
     *          for information about standard protocol names.
     *
     * @param provider the name of the provider.
     *
     * @return the new {@code SSLContext} object
     *
     * @throws IllegalArgumentException if the provider name is
     *         {@code null} or empty
     *
     * @throws NoSuchAlgorithmException if a {@code SSLContextSpi}
     *         implementation for the specified protocol is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code protocol} is {@code null}
     *
     * @see java.security.Provider
     */
    public static SSLContext getInstance(String protocol, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(protocol, "null protocol name");
        GetInstance.Instance instance = GetInstance.getInstance
                ("SSLContext", SSLContextSpi.class, protocol, provider);
        return new SSLContext((SSLContextSpi)instance.impl, instance.provider,
                protocol);
    }

    /**
     * Returns a {@code SSLContext} object that implements the
     * specified secure socket protocol.
     *
     * <p> A new SSLContext object encapsulating the
     * SSLContextSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param protocol the standard name of the requested protocol.
     *          See the SSLContext section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#sslcontext-algorithms">
     *          Java Security Standard Algorithm Names Specification</a>
     *          for information about standard protocol names.
     *
     * @param provider an instance of the provider.
     *
     * @return the new {@code SSLContext} object
     *
     * @throws IllegalArgumentException if the provider is {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code SSLContextSpi}
     *         implementation for the specified protocol is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code protocol} is {@code null}
     *
     * @see java.security.Provider
     */
    public static SSLContext getInstance(String protocol, Provider provider)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(protocol, "null protocol name");
        GetInstance.Instance instance = GetInstance.getInstance
                ("SSLContext", SSLContextSpi.class, protocol, provider);
        return new SSLContext((SSLContextSpi)instance.impl, instance.provider,
                protocol);
    }

    /**
     * Returns the protocol name of this {@code SSLContext} object.
     *
     * <p>This is the same name that was specified in one of the
     * {@code getInstance} calls that created this
     * {@code SSLContext} object.
     *
     * @return the protocol name of this {@code SSLContext} object.
     */
    public final String getProtocol() {
        return this.protocol;
    }

    /**
     * Returns the provider of this {@code SSLContext} object.
     *
     * @return the provider of this {@code SSLContext} object
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Initializes this context. Either of the first two parameters
     * may be null in which case the installed security providers will
     * be searched for the highest priority implementation of the
     * appropriate factory. Likewise, the secure random parameter may
     * be null in which case the default implementation will be used.
     * <P>
     * Only the first instance of a particular key and/or trust manager
     * implementation type in the array is used.  (For example, only
     * the first javax.net.ssl.X509KeyManager in the array will be used.)
     *
     * @param km the sources of authentication keys or null
     * @param tm the sources of peer authentication trust decisions or null
     * @param random the source of randomness for this generator or null
     * @throws KeyManagementException if this operation fails
     */
    public final void init(KeyManager[] km, TrustManager[] tm,
                                SecureRandom random)
        throws KeyManagementException {
        contextSpi.engineInit(km, tm, random);
    }

    /**
     * Returns a {@code SocketFactory} object for this
     * context.
     *
     * @return the {@code SocketFactory} object
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @throws IllegalStateException if the SSLContextImpl requires
     *         initialization and the {@code init()} has not been called
     */
    public final SSLSocketFactory getSocketFactory() {
        return contextSpi.engineGetSocketFactory();
    }

    /**
     * Returns a {@code ServerSocketFactory} object for
     * this context.
     *
     * @return the {@code ServerSocketFactory} object
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @throws IllegalStateException if the SSLContextImpl requires
     *         initialization and the {@code init()} has not been called
     */
    public final SSLServerSocketFactory getServerSocketFactory() {
        return contextSpi.engineGetServerSocketFactory();
    }

    /**
     * Creates a new {@code SSLEngine} using this context.
     * <P>
     * Applications using this factory method are providing no hints
     * for an internal session reuse strategy. If hints are desired,
     * {@link #createSSLEngine(String, int)} should be used
     * instead.
     * <P>
     * Some cipher suites (such as Kerberos) require remote hostname
     * information, in which case this factory method should not be used.
     *
     * @implNote
     * It is provider-specific if the returned SSLEngine uses client or
     * server mode by default for the (D)TLS connection. The JDK SunJSSE
     * provider implementation uses server mode by default.  However, it
     * is recommended to always set the desired mode explicitly by calling
     * {@link SSLEngine#setUseClientMode(boolean) SSLEngine.setUseClientMode()}
     * before invoking other methods of the SSLEngine.
     *
     * @return  the {@code SSLEngine} object
     * @throws  UnsupportedOperationException if the underlying provider
     *          does not implement the operation.
     * @throws  IllegalStateException if the SSLContextImpl requires
     *          initialization and the {@code init()} has not been called
     * @since   1.5
     */
    public final SSLEngine createSSLEngine() {
        try {
            return contextSpi.engineCreateSSLEngine();
        } catch (AbstractMethodError e) {
            UnsupportedOperationException unsup =
                new UnsupportedOperationException(
                    "Provider: " + getProvider() +
                    " doesn't support this operation");
            unsup.initCause(e);
            throw unsup;
        }
    }

    /**
     * Creates a new {@code SSLEngine} using this context using
     * advisory peer information.
     * <P>
     * Applications using this factory method are providing hints
     * for an internal session reuse strategy.
     * <P>
     * Some cipher suites (such as Kerberos) require remote hostname
     * information, in which case peerHost needs to be specified.
     *
     * @implNote
     * It is provider-specific if the returned SSLEngine uses client or
     * server mode by default for the (D)TLS connection. The JDK SunJSSE
     * provider implementation uses server mode by default.  However, it
     * is recommended to always set the desired mode explicitly by calling
     * {@link SSLEngine#setUseClientMode(boolean) SSLEngine.setUseClientMode()}
     * before invoking other methods of the SSLEngine.
     *
     * @param   peerHost the non-authoritative name of the host
     * @param   peerPort the non-authoritative port
     * @return  the new {@code SSLEngine} object
     * @throws  UnsupportedOperationException if the underlying provider
     *          does not implement the operation.
     * @throws  IllegalStateException if the SSLContextImpl requires
     *          initialization and the {@code init()} has not been called
     * @since   1.5
     */
    public final SSLEngine createSSLEngine(String peerHost, int peerPort) {
        try {
            return contextSpi.engineCreateSSLEngine(peerHost, peerPort);
        } catch (AbstractMethodError e) {
            UnsupportedOperationException unsup =
                new UnsupportedOperationException(
                    "Provider: " + getProvider() +
                    " does not support this operation");
            unsup.initCause(e);
            throw unsup;
        }
    }

    /**
     * Returns the server session context, which represents the set of
     * SSL sessions available for use during the handshake phase of
     * server-side SSL sockets.
     * <P>
     * This context may be unavailable in some environments, in which
     * case this method returns null. For example, when the underlying
     * SSL provider does not provide an implementation of SSLSessionContext
     * interface, this method returns null. A non-null session context
     * is returned otherwise.
     *
     * @return server session context bound to this SSL context
     */
    public final SSLSessionContext getServerSessionContext() {
        return contextSpi.engineGetServerSessionContext();
    }

    /**
     * Returns the client session context, which represents the set of
     * SSL sessions available for use during the handshake phase of
     * client-side SSL sockets.
     * <P>
     * This context may be unavailable in some environments, in which
     * case this method returns null. For example, when the underlying
     * SSL provider does not provide an implementation of SSLSessionContext
     * interface, this method returns null. A non-null session context
     * is returned otherwise.
     *
     * @return client session context bound to this SSL context
     */
    public final SSLSessionContext getClientSessionContext() {
        return contextSpi.engineGetClientSessionContext();
    }

    /**
     * Returns a copy of the SSLParameters indicating the default
     * settings for this SSL context.
     *
     * <p>The parameters will always have the ciphersuites and protocols
     * arrays set to non-null values.
     *
     * @return a copy of the SSLParameters object with the default settings
     * @throws UnsupportedOperationException if the default SSL parameters
     *   could not be obtained.
     * @since 1.6
     */
    public final SSLParameters getDefaultSSLParameters() {
        return contextSpi.engineGetDefaultSSLParameters();
    }

    /**
     * Returns a copy of the SSLParameters indicating the supported
     * settings for this SSL context.
     *
     * <p>The parameters will always have the ciphersuites and protocols
     * arrays set to non-null values.
     *
     * @return a copy of the SSLParameters object with the supported
     *   settings
     * @throws UnsupportedOperationException if the supported SSL parameters
     *   could not be obtained.
     * @since 1.6
     */
    public final SSLParameters getSupportedSSLParameters() {
        return contextSpi.engineGetSupportedSSLParameters();
    }

}
