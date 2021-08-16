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

package java.net;

import sun.net.www.protocol.http.AuthenticatorKeys;

/**
 * The class Authenticator represents an object that knows how to obtain
 * authentication for a network connection.  Usually, it will do this
 * by prompting the user for information.
 * <p>
 * Applications use this class by overriding {@link
 * #getPasswordAuthentication()} in a sub-class. This method will
 * typically use the various getXXX() accessor methods to get information
 * about the entity requesting authentication. It must then acquire a
 * username and password either by interacting with the user or through
 * some other non-interactive means. The credentials are then returned
 * as a {@link PasswordAuthentication} return value.
 * <p>
 * An instance of this concrete sub-class is then registered
 * with the system by calling {@link #setDefault(Authenticator)}.
 * When authentication is required, the system will invoke one of the
 * requestPasswordAuthentication() methods which in turn will call the
 * getPasswordAuthentication() method of the registered object.
 * <p>
 * All methods that request authentication have a default implementation
 * that fails.
 *
 * @see java.net.Authenticator#setDefault(java.net.Authenticator)
 * @see java.net.Authenticator#getPasswordAuthentication()
 *
 * @author  Bill Foote
 * @since   1.2
 */

// There are no abstract methods, but to be useful the user must
// subclass.
public abstract
class Authenticator {

    // The system-wide authenticator object.  See setDefault().
    private static volatile Authenticator theAuthenticator;

    private String requestingHost;
    private InetAddress requestingSite;
    private int requestingPort;
    private String requestingProtocol;
    private String requestingPrompt;
    private String requestingScheme;
    private URL requestingURL;
    private RequestorType requestingAuthType;
    private final String key = AuthenticatorKeys.computeKey(this);

    /**
     * Constructor for subclasses to call.
     */
    public Authenticator() {}

    /**
     * The type of the entity requesting authentication.
     *
     * @since 1.5
     */
    public enum RequestorType {
        /**
         * Entity requesting authentication is a HTTP proxy server.
         */
        PROXY,
        /**
         * Entity requesting authentication is a HTTP origin server.
         */
        SERVER
    }

    private void reset() {
        requestingHost = null;
        requestingSite = null;
        requestingPort = -1;
        requestingProtocol = null;
        requestingPrompt = null;
        requestingScheme = null;
        requestingURL = null;
        requestingAuthType = RequestorType.SERVER;
    }


    /**
     * Sets the authenticator that will be used by the networking code
     * when a proxy or an HTTP server asks for authentication.
     * <p>
     * First, if there is a security manager, its {@code checkPermission}
     * method is called with a
     * {@code NetPermission("setDefaultAuthenticator")} permission.
     * This may result in a java.lang.SecurityException.
     *
     * @param   a       The authenticator to be set. If a is {@code null} then
     *                  any previously set authenticator is removed.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        setting the default authenticator.
     *
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     */
    public static synchronized void setDefault(Authenticator a) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission setDefaultPermission
                = new NetPermission("setDefaultAuthenticator");
            sm.checkPermission(setDefaultPermission);
        }

        theAuthenticator = a;
    }

    /**
     * Gets the default authenticator.
     * First, if there is a security manager, its {@code checkPermission}
     * method is called with a
     * {@code NetPermission("requestPasswordAuthentication")} permission.
     * This may result in a java.lang.SecurityException.
     * Then the default authenticator, if set, is returned.
     * Otherwise, {@code null} is returned.
     *
     * @return The default authenticator, if set, {@code null} otherwise.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        requesting password authentication.
     * @since 9
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     */
    public static Authenticator getDefault() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }
        return theAuthenticator;
    }

    /**
     * Ask the authenticator that has been registered with the system
     * for a password.
     * <p>
     * First, if there is a security manager, its {@code checkPermission}
     * method is called with a
     * {@code NetPermission("requestPasswordAuthentication")} permission.
     * This may result in a java.lang.SecurityException.
     *
     * @param addr The InetAddress of the site requesting authorization,
     *             or null if not known.
     * @param port the port for the requested connection
     * @param protocol The protocol that's requesting the connection
     *          ({@link java.net.Authenticator#getRequestingProtocol()})
     * @param prompt A prompt string for the user
     * @param scheme The authentication scheme
     *
     * @return The username/password, or null if one can't be gotten.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        the password authentication request.
     *
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     */
    public static PasswordAuthentication requestPasswordAuthentication(
                                            InetAddress addr,
                                            int port,
                                            String protocol,
                                            String prompt,
                                            String scheme) {

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = theAuthenticator;
        if (a == null) {
            return null;
        } else {
            synchronized(a) {
                a.reset();
                a.requestingSite = addr;
                a.requestingPort = port;
                a.requestingProtocol = protocol;
                a.requestingPrompt = prompt;
                a.requestingScheme = scheme;
                return a.getPasswordAuthentication();
            }
        }
    }

    /**
     * Ask the authenticator that has been registered with the system
     * for a password. This is the preferred method for requesting a password
     * because the hostname can be provided in cases where the InetAddress
     * is not available.
     * <p>
     * First, if there is a security manager, its {@code checkPermission}
     * method is called with a
     * {@code NetPermission("requestPasswordAuthentication")} permission.
     * This may result in a java.lang.SecurityException.
     *
     * @param host The hostname of the site requesting authentication.
     * @param addr The InetAddress of the site requesting authentication,
     *             or null if not known.
     * @param port the port for the requested connection.
     * @param protocol The protocol that's requesting the connection
     *          ({@link java.net.Authenticator#getRequestingProtocol()})
     * @param prompt A prompt string for the user which identifies the authentication realm.
     * @param scheme The authentication scheme
     *
     * @return The username/password, or null if one can't be gotten.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        the password authentication request.
     *
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     * @since 1.4
     */
    public static PasswordAuthentication requestPasswordAuthentication(
                                            String host,
                                            InetAddress addr,
                                            int port,
                                            String protocol,
                                            String prompt,
                                            String scheme) {

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = theAuthenticator;
        if (a == null) {
            return null;
        } else {
            synchronized(a) {
                a.reset();
                a.requestingHost = host;
                a.requestingSite = addr;
                a.requestingPort = port;
                a.requestingProtocol = protocol;
                a.requestingPrompt = prompt;
                a.requestingScheme = scheme;
                return a.getPasswordAuthentication();
            }
        }
    }

    /**
     * Ask the authenticator that has been registered with the system
     * for a password.
     * <p>
     * First, if there is a security manager, its {@code checkPermission}
     * method is called with a
     * {@code NetPermission("requestPasswordAuthentication")} permission.
     * This may result in a java.lang.SecurityException.
     *
     * @param host The hostname of the site requesting authentication.
     * @param addr The InetAddress of the site requesting authorization,
     *             or null if not known.
     * @param port the port for the requested connection
     * @param protocol The protocol that's requesting the connection
     *          ({@link java.net.Authenticator#getRequestingProtocol()})
     * @param prompt A prompt string for the user
     * @param scheme The authentication scheme
     * @param url The requesting URL that caused the authentication
     * @param reqType The type (server or proxy) of the entity requesting
     *              authentication.
     *
     * @return The username/password, or null if one can't be gotten.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        the password authentication request.
     *
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     *
     * @since 1.5
     */
    public static PasswordAuthentication requestPasswordAuthentication(
                                    String host,
                                    InetAddress addr,
                                    int port,
                                    String protocol,
                                    String prompt,
                                    String scheme,
                                    URL url,
                                    RequestorType reqType) {

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = theAuthenticator;
        if (a == null) {
            return null;
        } else {
            synchronized(a) {
                a.reset();
                a.requestingHost = host;
                a.requestingSite = addr;
                a.requestingPort = port;
                a.requestingProtocol = protocol;
                a.requestingPrompt = prompt;
                a.requestingScheme = scheme;
                a.requestingURL = url;
                a.requestingAuthType = reqType;
                return a.getPasswordAuthentication();
            }
        }
    }

    /**
     * Ask the given {@code authenticator} for a password. If the given
     * {@code authenticator} is null, the authenticator, if any, that has been
     * registered with the system using {@link #setDefault(java.net.Authenticator)
     * setDefault} is used.
     * <p>
     * First, if there is a security manager, its {@code checkPermission}
     * method is called with a
     * {@code NetPermission("requestPasswordAuthentication")} permission.
     * This may result in a java.lang.SecurityException.
     *
     * @param authenticator the authenticator, or {@code null}.
     * @param host The hostname of the site requesting authentication.
     * @param addr The InetAddress of the site requesting authorization,
     *             or null if not known.
     * @param port the port for the requested connection
     * @param protocol The protocol that's requesting the connection
     *          ({@link java.net.Authenticator#getRequestingProtocol()})
     * @param prompt A prompt string for the user
     * @param scheme The authentication scheme
     * @param url The requesting URL that caused the authentication
     * @param reqType The type (server or proxy) of the entity requesting
     *              authentication.
     *
     * @return The username/password, or {@code null} if one can't be gotten.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        the password authentication request.
     *
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     *
     * @since 9
     */
    public static PasswordAuthentication requestPasswordAuthentication(
                                    Authenticator authenticator,
                                    String host,
                                    InetAddress addr,
                                    int port,
                                    String protocol,
                                    String prompt,
                                    String scheme,
                                    URL url,
                                    RequestorType reqType) {

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = authenticator == null ? theAuthenticator : authenticator;
        if (a == null) {
            return null;
        } else {
            return a.requestPasswordAuthenticationInstance(host,
                                                           addr,
                                                           port,
                                                           protocol,
                                                           prompt,
                                                           scheme,
                                                           url,
                                                           reqType);
        }
    }

    /**
     * Ask this authenticator for a password.
     *
     * @param host The hostname of the site requesting authentication.
     * @param addr The InetAddress of the site requesting authorization,
     *             or null if not known.
     * @param port the port for the requested connection
     * @param protocol The protocol that's requesting the connection
     *          ({@link java.net.Authenticator#getRequestingProtocol()})
     * @param prompt A prompt string for the user
     * @param scheme The authentication scheme
     * @param url The requesting URL that caused the authentication
     * @param reqType The type (server or proxy) of the entity requesting
     *              authentication.
     *
     * @return The username/password, or null if one can't be gotten
     *
     * @since 9
     */
    public PasswordAuthentication
    requestPasswordAuthenticationInstance(String host,
                                          InetAddress addr,
                                          int port,
                                          String protocol,
                                          String prompt,
                                          String scheme,
                                          URL url,
                                          RequestorType reqType) {
        synchronized (this) {
            this.reset();
            this.requestingHost = host;
            this.requestingSite = addr;
            this.requestingPort = port;
            this.requestingProtocol = protocol;
            this.requestingPrompt = prompt;
            this.requestingScheme = scheme;
            this.requestingURL = url;
            this.requestingAuthType = reqType;
            return this.getPasswordAuthentication();
        }
    }

    /**
     * Gets the {@code hostname} of the
     * site or proxy requesting authentication, or {@code null}
     * if not available.
     *
     * @return the hostname of the connection requiring authentication, or null
     *          if it's not available.
     * @since 1.4
     */
    protected final String getRequestingHost() {
        return requestingHost;
    }

    /**
     * Gets the {@code InetAddress} of the
     * site requesting authorization, or {@code null}
     * if not available.
     *
     * @return the InetAddress of the site requesting authorization, or null
     *          if it's not available.
     */
    protected final InetAddress getRequestingSite() {
        return requestingSite;
    }

    /**
     * Gets the port number for the requested connection.
     * @return an {@code int} indicating the
     * port for the requested connection.
     */
    protected final int getRequestingPort() {
        return requestingPort;
    }

    /**
     * Give the protocol that's requesting the connection.  Often this
     * will be based on a URL, but in a future JDK it could be, for
     * example, "SOCKS" for a password-protected SOCKS5 firewall.
     *
     * @return the protocol, optionally followed by "/version", where
     *          version is a version number.
     *
     * @see java.net.URL#getProtocol()
     */
    protected final String getRequestingProtocol() {
        return requestingProtocol;
    }

    /**
     * Gets the prompt string given by the requestor.
     *
     * @return the prompt string given by the requestor (realm for
     *          http requests)
     */
    protected final String getRequestingPrompt() {
        return requestingPrompt;
    }

    /**
     * Gets the scheme of the requestor (the HTTP scheme
     * for an HTTP firewall, for example).
     *
     * @return the scheme of the requestor
     *
     */
    protected final String getRequestingScheme() {
        return requestingScheme;
    }

    /**
     * Called when password authorization is needed.  Subclasses should
     * override the default implementation, which returns null.
     * @return The PasswordAuthentication collected from the
     *          user, or null if none is provided.
     */
    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    /**
     * Returns the URL that resulted in this
     * request for authentication.
     *
     * @since 1.5
     *
     * @return the requesting URL
     *
     */
    protected URL getRequestingURL () {
        return requestingURL;
    }

    /**
     * Returns whether the requestor is a Proxy or a Server.
     *
     * @since 1.5
     *
     * @return the authentication type of the requestor
     *
     */
    protected RequestorType getRequestorType () {
        return requestingAuthType;
    }

    static String getKey(Authenticator a) {
        return a.key;
    }
    static {
        AuthenticatorKeys.setAuthenticatorKeyAccess(Authenticator::getKey);
    }
}
