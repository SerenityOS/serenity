/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.security;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.management.remote.JMXAuthenticator;
import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;

/**
 * <p>This class represents a
 * <a href="{@docRoot}/../guide/security/jaas/JAASRefGuide.html">JAAS</a>
 * based implementation of the {@link JMXAuthenticator} interface.</p>
 *
 * <p>Authentication is performed by passing the supplied user's credentials
 * to one or more authentication mechanisms ({@link LoginModule}) for
 * verification. An authentication mechanism acquires the user's credentials
 * by calling {@link NameCallback} and/or {@link PasswordCallback}.
 * If authentication is successful then an authenticated {@link Subject}
 * filled in with a {@link Principal} is returned.  Authorization checks
 * will then be performed based on this <code>Subject</code>.</p>
 *
 * <p>By default, a single file-based authentication mechanism
 * {@link FileLoginModule} is configured (<code>FileLoginConfig</code>).</p>
 *
 * <p>To override the default configuration use the
 * <code>com.sun.management.jmxremote.login.config</code> management property
 * described in the JRE/conf/management/management.properties file.
 * Set this property to the name of a JAAS configuration entry and ensure that
 * the entry is loaded by the installed {@link Configuration}. In addition,
 * ensure that the authentication mechanisms specified in the entry acquire
 * the user's credentials by calling {@link NameCallback} and
 * {@link PasswordCallback} and that they return a {@link Subject} filled-in
 * with a {@link Principal}, for those users that are successfully
 * authenticated.</p>
 */
public final class JMXPluggableAuthenticator implements JMXAuthenticator {

    /**
     * Creates an instance of <code>JMXPluggableAuthenticator</code>
     * and initializes it with a {@link LoginContext}.
     *
     * @param env the environment containing configuration properties for the
     *            authenticator. Can be null, which is equivalent to an empty
     *            Map.
     * @exception SecurityException if the authentication mechanism cannot be
     *            initialized.
     */
    public JMXPluggableAuthenticator(Map<?, ?> env) {

        String loginConfigName = null;
        String passwordFile = null;
        String hashPasswords = null;

        if (env != null) {
            loginConfigName = (String) env.get(LOGIN_CONFIG_PROP);
            passwordFile = (String) env.get(PASSWORD_FILE_PROP);
            hashPasswords = (String) env.get(HASH_PASSWORDS);
        }

        try {

            if (loginConfigName != null) {
                // use the supplied JAAS login configuration
                loginContext =
                    new LoginContext(loginConfigName, new JMXCallbackHandler());

            } else {
                // use the default JAAS login configuration (file-based)
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkPermission(
                            new AuthPermission("createLoginContext." +
                                               LOGIN_CONFIG_NAME));
                }

                final String pf = passwordFile;
                final String hashPass = hashPasswords;
                try {
                    @SuppressWarnings("removal")
                    var tmp = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<LoginContext>() {
                            public LoginContext run() throws LoginException {
                                return new LoginContext(
                                                LOGIN_CONFIG_NAME,
                                                null,
                                                new JMXCallbackHandler(),
                                                new FileLoginConfig(pf, hashPass));
                            }
                        });
                    loginContext = tmp;
                } catch (PrivilegedActionException pae) {
                    throw (LoginException) pae.getException();
                }
            }

        } catch (LoginException le) {
            authenticationFailure("authenticate", le);

        } catch (SecurityException se) {
            authenticationFailure("authenticate", se);
        }
    }

    /**
     * Authenticate the <code>MBeanServerConnection</code> client
     * with the given client credentials.
     *
     * @param credentials the user-defined credentials to be passed in
     * to the server in order to authenticate the user before creating
     * the <code>MBeanServerConnection</code>.  This parameter must
     * be a two-element <code>String[]</code> containing the client's
     * username and password in that order.
     *
     * @return the authenticated subject containing a
     * <code>JMXPrincipal(username)</code>.
     *
     * @exception SecurityException if the server cannot authenticate the user
     * with the provided credentials.
     */
    public Subject authenticate(Object credentials) {
        // Verify that credentials is of type String[].
        //
        if (!(credentials instanceof String[])) {
            // Special case for null so we get a more informative message
            if (credentials == null)
                authenticationFailure("authenticate", "Credentials required");

            final String message =
                "Credentials should be String[] instead of " +
                 credentials.getClass().getName();
            authenticationFailure("authenticate", message);
        }
        // Verify that the array contains two elements.
        //
        final String[] aCredentials = (String[]) credentials;
        if (aCredentials.length != 2) {
            final String message =
                "Credentials should have 2 elements not " +
                aCredentials.length;
            authenticationFailure("authenticate", message);
        }
        // Verify that username exists and the associated
        // password matches the one supplied by the client.
        //
        username = aCredentials[0];
        password = aCredentials[1];
        if (username == null || password == null) {
            final String message = "Username or password is null";
            authenticationFailure("authenticate", message);
        }

        // Perform authentication
        try {
            loginContext.login();
            final Subject subject = loginContext.getSubject();
            @SuppressWarnings("removal")
            var dummy = AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        subject.setReadOnly();
                        return null;
                    }
                });

            return subject;

        } catch (LoginException le) {
            authenticationFailure("authenticate", le);
        }
        return null;
    }

    private static void authenticationFailure(String method, String message)
        throws SecurityException {
        final String msg = "Authentication failed! " + message;
        final SecurityException e = new SecurityException(msg);
        logException(method, msg, e);
        throw e;
    }

    private static void authenticationFailure(String method,
                                              Exception exception)
        throws SecurityException {
        String msg;
        SecurityException se;
        if (exception instanceof SecurityException) {
            msg = exception.getMessage();
            se = (SecurityException) exception;
        } else {
            msg = "Authentication failed! " + exception.getMessage();
            final SecurityException e = new SecurityException(msg);
            EnvHelp.initCause(e, exception);
            se = e;
        }
        logException(method, msg, se);
        throw se;
    }

    private static void logException(String method,
                                     String message,
                                     Exception e) {
        if (logger.traceOn()) {
            logger.trace(method, message);
        }
        if (logger.debugOn()) {
            logger.debug(method, e);
        }
    }

    private LoginContext loginContext;
    private String username;
    private String password;
    private static final String LOGIN_CONFIG_PROP =
        "jmx.remote.x.login.config";
    private static final String LOGIN_CONFIG_NAME = "JMXPluggableAuthenticator";
    private static final String PASSWORD_FILE_PROP =
        "jmx.remote.x.password.file";
    private static final String HASH_PASSWORDS =
        "jmx.remote.x.password.toHashes";
    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc", LOGIN_CONFIG_NAME);

/**
 * This callback handler supplies the username and password (which was
 * originally supplied by the JMX user) to the JAAS login module performing
 * the authentication. No interactive user prompting is required because the
 * credentials are already available to this class (via its enclosing class).
 */
private final class JMXCallbackHandler implements CallbackHandler {

    /**
     * Sets the username and password in the appropriate Callback object.
     */
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                ((NameCallback)callbacks[i]).setName(username);

            } else if (callbacks[i] instanceof PasswordCallback) {
                ((PasswordCallback)callbacks[i])
                    .setPassword(password.toCharArray());

            } else {
                throw new UnsupportedCallbackException
                    (callbacks[i], "Unrecognized Callback");
            }
        }
    }
}

/**
 * This class defines the JAAS configuration for file-based authentication.
 * It is equivalent to the following textual configuration entry:
 * <pre>
 *     JMXPluggableAuthenticator {
 *         com.sun.jmx.remote.security.FileLoginModule required;
 *     };
 * </pre>
 */
private static class FileLoginConfig extends Configuration {

    // The JAAS configuration for file-based authentication
    private AppConfigurationEntry[] entries;

    // The classname of the login module for file-based authentication
    private static final String FILE_LOGIN_MODULE =
        FileLoginModule.class.getName();

    // The option that identifies the password file to use
    private static final String PASSWORD_FILE_OPTION = "passwordFile";
    private static final String HASH_PASSWORDS = "hashPasswords";

    /**
     * Creates an instance of <code>FileLoginConfig</code>
     *
     * @param passwordFile A filepath that identifies the password file to use.
     *                     If null then the default password file is used.
     * @param hashPasswords Flag to indicate if the password file needs to be hashed
     */
    public FileLoginConfig(String passwordFile, String hashPasswords) {

        Map<String, String> options;
        if (passwordFile != null) {
            options = new HashMap<String, String>(1);
            options.put(PASSWORD_FILE_OPTION, passwordFile);
            options.put(HASH_PASSWORDS, hashPasswords);
        } else {
            options = Collections.emptyMap();
        }

        entries = new AppConfigurationEntry[] {
            new AppConfigurationEntry(FILE_LOGIN_MODULE,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options)
        };
    }

    /**
     * Gets the JAAS configuration for file-based authentication
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {

        return name.equals(LOGIN_CONFIG_NAME) ? entries : null;
    }

    /**
     * Refreshes the configuration.
     */
    public void refresh() {
        // the configuration is fixed
    }
}

}
