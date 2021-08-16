/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.auth.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.security.auth.Destroyable;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Subject;
import javax.security.auth.x500.*;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import sun.security.util.Password;
import static sun.security.util.ResourcesMgr.getAuthResourceString;

/**
 * Provides a JAAS login module that prompts for a key store alias and
 * populates the subject with the alias's principal and credentials. Stores
 * an {@code X500Principal} for the subject distinguished name of the
 * first certificate in the alias's credentials in the subject's principals,
 * the alias's certificate path in the subject's public credentials, and a
 * {@code X500PrivateCredential} whose certificate is the first
 * certificate in the alias's certificate path and whose private key is the
 * alias's private key in the subject's private credentials. <p>
 *
 * Recognizes the following options in the configuration file:
 * <dl>
 *
 * <dt> {@code keyStoreURL} </dt>
 * <dd> A URL that specifies the location of the key store.  Defaults to
 *      a URL pointing to the .keystore file in the directory specified by the
 *      {@code user.home} system property.  The input stream from this
 *      URL is passed to the {@code KeyStore.load} method.
 *      "NONE" may be specified if a {@code null} stream must be
 *      passed to the {@code KeyStore.load} method.
 *      "NONE" should be specified if the KeyStore resides
 *      on a hardware token device, for example.</dd>
 *
 * <dt> {@code keyStoreType} </dt>
 * <dd> The key store type.  If not specified, defaults to the result of
 *      calling {@code KeyStore.getDefaultType()}.
 *      If the type is "PKCS11", then keyStoreURL must be "NONE"
 *      and privateKeyPasswordURL must not be specified.</dd>
 *
 * <dt> {@code keyStoreProvider} </dt>
 * <dd> The key store provider.  If not specified, uses the standard search
 *      order to find the provider. </dd>
 *
 * <dt> {@code keyStoreAlias} </dt>
 * <dd> The alias in the key store to login as.  Required when no callback
 *      handler is provided.  No default value. </dd>
 *
 * <dt> {@code keyStorePasswordURL} </dt>
 * <dd> A URL that specifies the location of the key store password.  Required
 *      when no callback handler is provided and
 *      {@code protected} is false.
 *      No default value. </dd>
 *
 * <dt> {@code privateKeyPasswordURL} </dt>
 * <dd> A URL that specifies the location of the specific private key password
 *      needed to access the private key for this alias.
 *      The keystore password
 *      is used if this value is needed and not specified. </dd>
 *
 * <dt> {@code protected} </dt>
 * <dd> This value should be set to "true" if the KeyStore
 *      has a separate, protected authentication path
 *      (for example, a dedicated PIN-pad attached to a smart card).
 *      Defaults to "false". If "true" keyStorePasswordURL and
 *      privateKeyPasswordURL must not be specified.</dd>
 *
 * </dl>
 */
public class KeyStoreLoginModule implements LoginModule {

    /* -- Fields -- */

    private static final int UNINITIALIZED = 0;
    private static final int INITIALIZED = 1;
    private static final int AUTHENTICATED = 2;
    private static final int LOGGED_IN = 3;

    private static final int PROTECTED_PATH = 0;
    private static final int TOKEN = 1;
    private static final int NORMAL = 2;

    private static final String NONE = "NONE";
    private static final String P11KEYSTORE = "PKCS11";

    private static final TextOutputCallback bannerCallback =
                new TextOutputCallback
                        (TextOutputCallback.INFORMATION,
                        getAuthResourceString("Please.enter.keystore.information"));
    private final ConfirmationCallback confirmationCallback =
                new ConfirmationCallback
                        (ConfirmationCallback.INFORMATION,
                        ConfirmationCallback.OK_CANCEL_OPTION,
                        ConfirmationCallback.OK);

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private Map<String, ?> options;

    private char[] keyStorePassword;
    private char[] privateKeyPassword;
    private KeyStore keyStore;

    private String keyStoreURL;
    private String keyStoreType;
    private String keyStoreProvider;
    private String keyStoreAlias;
    private String keyStorePasswordURL;
    private String privateKeyPasswordURL;
    private boolean debug;
    private javax.security.auth.x500.X500Principal principal;
    private Certificate[] fromKeyStore;
    private java.security.cert.CertPath certP = null;
    private X500PrivateCredential privateCredential;
    private int status = UNINITIALIZED;
    private boolean nullStream = false;
    private boolean token = false;
    private boolean protectedPath = false;

    /**
     * Creates a {@code KeyStoreLoginModule}.
     */
    public KeyStoreLoginModule() {}

    /* -- Methods -- */

    /**
     * Initialize this {@code LoginModule}.
     *
     * @param subject the {@code Subject} to be authenticated.
     *
     * @param callbackHandler a {@code CallbackHandler} for communicating
     *                  with the end user (prompting for usernames and
     *                  passwords, for example),
     *                  which may be {@code null}.
     *
     * @param sharedState shared {@code LoginModule} state.
     *
     * @param options options specified in the login
     *                  {@code Configuration} for this particular
     *                  {@code LoginModule}.
     */
    // Unchecked warning from (Map<String, Object>)sharedState is safe
    // since javax.security.auth.login.LoginContext passes a raw HashMap.
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map<String,?> sharedState,
                           Map<String,?> options)
    {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = (Map<String, Object>)sharedState;
        this.options = options;

        processOptions();
        status = INITIALIZED;
    }

    private void processOptions() {
        keyStoreURL = (String) options.get("keyStoreURL");
        if (keyStoreURL == null) {
            keyStoreURL =
                "file:" +
                System.getProperty("user.home").replace(
                    File.separatorChar, '/') +
                '/' + ".keystore";
        } else if (NONE.equals(keyStoreURL)) {
            nullStream = true;
        }
        keyStoreType = (String) options.get("keyStoreType");
        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        if (P11KEYSTORE.equalsIgnoreCase(keyStoreType)) {
            token = true;
        }

        keyStoreProvider = (String) options.get("keyStoreProvider");

        keyStoreAlias = (String) options.get("keyStoreAlias");

        keyStorePasswordURL = (String) options.get("keyStorePasswordURL");

        privateKeyPasswordURL = (String) options.get("privateKeyPasswordURL");

        protectedPath = "true".equalsIgnoreCase((String)options.get
                                        ("protected"));

        debug = "true".equalsIgnoreCase((String) options.get("debug"));
        if (debug) {
            debugPrint(null);
            debugPrint("keyStoreURL=" + keyStoreURL);
            debugPrint("keyStoreType=" + keyStoreType);
            debugPrint("keyStoreProvider=" + keyStoreProvider);
            debugPrint("keyStoreAlias=" + keyStoreAlias);
            debugPrint("keyStorePasswordURL=" + keyStorePasswordURL);
            debugPrint("privateKeyPasswordURL=" + privateKeyPasswordURL);
            debugPrint("protectedPath=" + protectedPath);
            debugPrint(null);
        }
    }

    /**
     * Authenticate the user.
     *
     * <p> Get the Keystore alias and relevant passwords.
     * Retrieve the alias's principal and credentials from the Keystore.
     *
     * @exception FailedLoginException if the authentication fails.
     *
     * @return true in all cases (this {@code LoginModule}
     *          should not be ignored).
     */

    public boolean login() throws LoginException {
        switch (status) {
        case UNINITIALIZED:
        default:
            throw new LoginException("The login module is not initialized");
        case INITIALIZED:
        case AUTHENTICATED:

            if (token && !nullStream) {
                throw new LoginException
                        ("if keyStoreType is " + P11KEYSTORE +
                        " then keyStoreURL must be " + NONE);
            }

            if (token && privateKeyPasswordURL != null) {
                throw new LoginException
                        ("if keyStoreType is " + P11KEYSTORE +
                        " then privateKeyPasswordURL must not be specified");
            }

            if (protectedPath &&
                (keyStorePasswordURL != null ||
                        privateKeyPasswordURL != null)) {
                throw new LoginException
                        ("if protected is true then keyStorePasswordURL and " +
                        "privateKeyPasswordURL must not be specified");
            }

            // get relevant alias and password info

            if (protectedPath) {
                getAliasAndPasswords(PROTECTED_PATH);
            } else if (token) {
                getAliasAndPasswords(TOKEN);
            } else {
                getAliasAndPasswords(NORMAL);
            }

            // log into KeyStore to retrieve data,
            // then clear passwords

            try {
                getKeyStoreInfo();
            } finally {
                if (privateKeyPassword != null &&
                    privateKeyPassword != keyStorePassword) {
                    Arrays.fill(privateKeyPassword, '\0');
                    privateKeyPassword = null;
                }
                if (keyStorePassword != null) {
                    Arrays.fill(keyStorePassword, '\0');
                    keyStorePassword = null;
                }
            }
            status = AUTHENTICATED;
            return true;
        case LOGGED_IN:
            return true;
        }
    }

    /** Get the alias and passwords to use for looking up in the KeyStore. */
    @SuppressWarnings("fallthrough")
    private void getAliasAndPasswords(int env) throws LoginException {
        if (callbackHandler == null) {

            // No callback handler - check for alias and password options

            switch (env) {
            case PROTECTED_PATH:
                checkAlias();
                break;
            case TOKEN:
                checkAlias();
                checkStorePass();
                break;
            case NORMAL:
                checkAlias();
                checkStorePass();
                checkKeyPass();
                break;
            }

        } else {

            // Callback handler available - prompt for alias and passwords

            NameCallback aliasCallback;
            if (keyStoreAlias == null || keyStoreAlias.length() == 0) {
                aliasCallback = new NameCallback(getAuthResourceString("Keystore.alias."));
            } else {
                aliasCallback =
                    new NameCallback(getAuthResourceString("Keystore.alias."),
                                     keyStoreAlias);
            }

            PasswordCallback storePassCallback = null;
            PasswordCallback keyPassCallback = null;

            switch (env) {
            case PROTECTED_PATH:
                break;
            case NORMAL:
                keyPassCallback = new PasswordCallback
                    (getAuthResourceString("Private.key.password.optional."), false);
                // fall thru
            case TOKEN:
                storePassCallback = new PasswordCallback
                    (getAuthResourceString("Keystore.password."), false);
                break;
            }
            prompt(aliasCallback, storePassCallback, keyPassCallback);
        }

        if (debug) {
            debugPrint("alias=" + keyStoreAlias);
        }
    }

    private void checkAlias() throws LoginException {
        if (keyStoreAlias == null) {
            throw new LoginException
                ("Need to specify an alias option to use " +
                "KeyStoreLoginModule non-interactively.");
        }
    }

    private void checkStorePass() throws LoginException {
        if (keyStorePasswordURL == null) {
            throw new LoginException
                ("Need to specify keyStorePasswordURL option to use " +
                "KeyStoreLoginModule non-interactively.");
        }
        InputStream in = null;
        try {
            in = new URL(keyStorePasswordURL).openStream();
            keyStorePassword = Password.readPassword(in);
        } catch (IOException e) {
            LoginException le = new LoginException
                ("Problem accessing keystore password \"" +
                keyStorePasswordURL + "\"");
            le.initCause(e);
            throw le;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    LoginException le = new LoginException(
                        "Problem closing the keystore password stream");
                    le.initCause(ioe);
                    throw le;
                }
            }
        }
    }

    private void checkKeyPass() throws LoginException {
        if (privateKeyPasswordURL == null) {
            privateKeyPassword = keyStorePassword;
        } else {
            InputStream in = null;
            try {
                in = new URL(privateKeyPasswordURL).openStream();
                privateKeyPassword = Password.readPassword(in);
            } catch (IOException e) {
                LoginException le = new LoginException
                        ("Problem accessing private key password \"" +
                        privateKeyPasswordURL + "\"");
                le.initCause(e);
                throw le;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                        LoginException le = new LoginException(
                            "Problem closing the private key password stream");
                        le.initCause(ioe);
                        throw le;
                    }
                }
            }
        }
    }

    private void prompt(NameCallback aliasCallback,
                        PasswordCallback storePassCallback,
                        PasswordCallback keyPassCallback)
                throws LoginException {

        if (storePassCallback == null) {

            // only prompt for alias

            try {
                callbackHandler.handle(
                    new Callback[] {
                        bannerCallback, aliasCallback, confirmationCallback
                    });
            } catch (IOException e) {
                LoginException le = new LoginException
                        ("Problem retrieving keystore alias");
                le.initCause(e);
                throw le;
            } catch (UnsupportedCallbackException e) {
                throw new LoginException(
                    "Error: " + e.getCallback().toString() +
                    " is not available to retrieve authentication " +
                    " information from the user");
            }

            int confirmationResult = confirmationCallback.getSelectedIndex();

            if (confirmationResult == ConfirmationCallback.CANCEL) {
                throw new LoginException("Login cancelled");
            }

            saveAlias(aliasCallback);

        } else if (keyPassCallback == null) {

            // prompt for alias and key store password

            try {
                callbackHandler.handle(
                    new Callback[] {
                        bannerCallback, aliasCallback,
                        storePassCallback, confirmationCallback
                    });
            } catch (IOException e) {
                LoginException le = new LoginException
                        ("Problem retrieving keystore alias and password");
                le.initCause(e);
                throw le;
            } catch (UnsupportedCallbackException e) {
                throw new LoginException(
                    "Error: " + e.getCallback().toString() +
                    " is not available to retrieve authentication " +
                    " information from the user");
            }

            int confirmationResult = confirmationCallback.getSelectedIndex();

            if (confirmationResult == ConfirmationCallback.CANCEL) {
                throw new LoginException("Login cancelled");
            }

            saveAlias(aliasCallback);
            saveStorePass(storePassCallback);

        } else {

            // prompt for alias, key store password, and key password

            try {
                callbackHandler.handle(
                    new Callback[] {
                        bannerCallback, aliasCallback,
                        storePassCallback, keyPassCallback,
                        confirmationCallback
                    });
            } catch (IOException e) {
                LoginException le = new LoginException
                        ("Problem retrieving keystore alias and passwords");
                le.initCause(e);
                throw le;
            } catch (UnsupportedCallbackException e) {
                throw new LoginException(
                    "Error: " + e.getCallback().toString() +
                    " is not available to retrieve authentication " +
                    " information from the user");
            }

            int confirmationResult = confirmationCallback.getSelectedIndex();

            if (confirmationResult == ConfirmationCallback.CANCEL) {
                throw new LoginException("Login cancelled");
            }

            saveAlias(aliasCallback);
            saveStorePass(storePassCallback);
            saveKeyPass(keyPassCallback);
        }
    }

    private void saveAlias(NameCallback cb) {
        keyStoreAlias = cb.getName();
    }

    private void saveStorePass(PasswordCallback c) {
        keyStorePassword = c.getPassword();
        if (keyStorePassword == null) {
            /* Treat a NULL password as an empty password */
            keyStorePassword = new char[0];
        }
        c.clearPassword();
    }

    private void saveKeyPass(PasswordCallback c) {
        privateKeyPassword = c.getPassword();
        if (privateKeyPassword == null || privateKeyPassword.length == 0) {
            /*
             * Use keystore password if no private key password is
             * specified.
             */
            privateKeyPassword = keyStorePassword;
        }
        c.clearPassword();
    }

    /** Get the credentials from the KeyStore. */
    private void getKeyStoreInfo() throws LoginException {

        /* Get KeyStore instance */
        try {
            if (keyStoreProvider == null) {
                keyStore = KeyStore.getInstance(keyStoreType);
            } else {
                keyStore =
                    KeyStore.getInstance(keyStoreType, keyStoreProvider);
            }
        } catch (KeyStoreException e) {
            LoginException le = new LoginException
                ("The specified keystore type was not available");
            le.initCause(e);
            throw le;
        } catch (NoSuchProviderException e) {
            LoginException le = new LoginException
                ("The specified keystore provider was not available");
            le.initCause(e);
            throw le;
        }

        /* Load KeyStore contents from file */
        InputStream in = null;
        try {
            if (nullStream) {
                // if using protected auth path, keyStorePassword will be null
                keyStore.load(null, keyStorePassword);
            } else {
                in = new URL(keyStoreURL).openStream();
                keyStore.load(in, keyStorePassword);
            }
        } catch (MalformedURLException e) {
            LoginException le = new LoginException
                                ("Incorrect keyStoreURL option");
            le.initCause(e);
            throw le;
        } catch (GeneralSecurityException e) {
            LoginException le = new LoginException
                                ("Error initializing keystore");
            le.initCause(e);
            throw le;
        } catch (IOException e) {
            LoginException le = new LoginException
                                ("Error initializing keystore");
            le.initCause(e);
            throw le;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    LoginException le = new LoginException
                                ("Error initializing keystore");
                    le.initCause(ioe);
                    throw le;
                }
            }
        }

        /* Get certificate chain and create a certificate path */
        try {
            fromKeyStore =
                keyStore.getCertificateChain(keyStoreAlias);
            if (fromKeyStore == null
                || fromKeyStore.length == 0
                || !(fromKeyStore[0] instanceof X509Certificate))
            {
                throw new FailedLoginException(
                    "Unable to find X.509 certificate chain in keystore");
            } else {
                LinkedList<Certificate> certList = new LinkedList<>();
                for (int i=0; i < fromKeyStore.length; i++) {
                    certList.add(fromKeyStore[i]);
                }
                CertificateFactory certF=
                    CertificateFactory.getInstance("X.509");
                certP =
                    certF.generateCertPath(certList);
            }
        } catch (KeyStoreException e) {
            LoginException le = new LoginException("Error using keystore");
            le.initCause(e);
            throw le;
        } catch (CertificateException ce) {
            LoginException le = new LoginException
                ("Error: X.509 Certificate type unavailable");
            le.initCause(ce);
            throw le;
        }

        /* Get principal and keys */
        try {
            X509Certificate certificate = (X509Certificate)fromKeyStore[0];
            principal = certificate.getSubjectX500Principal();

            // if token, privateKeyPassword will be null
            Key privateKey = keyStore.getKey(keyStoreAlias, privateKeyPassword);
            if (privateKey == null
                || !(privateKey instanceof PrivateKey))
            {
                throw new FailedLoginException(
                    "Unable to recover key from keystore");
            }

            privateCredential = new X500PrivateCredential(
                certificate, (PrivateKey) privateKey, keyStoreAlias);
        } catch (KeyStoreException e) {
            LoginException le = new LoginException("Error using keystore");
            le.initCause(e);
            throw le;
        } catch (NoSuchAlgorithmException e) {
            LoginException le = new LoginException("Error using keystore");
            le.initCause(e);
            throw le;
        } catch (UnrecoverableKeyException e) {
            FailedLoginException fle = new FailedLoginException
                                ("Unable to recover key from keystore");
            fle.initCause(e);
            throw fle;
        }
        if (debug) {
            debugPrint("principal=" + principal +
                       "\n certificate="
                       + privateCredential.getCertificate() +
                       "\n alias =" + privateCredential.getAlias());
        }
    }

    /**
     * Abstract method to commit the authentication process (phase 2).
     *
     * <p> This method is called if the LoginContext's
     * overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * {@code login} method), then this method associates a
     * {@code X500Principal} for the subject distinguished name of the
     * first certificate in the alias's credentials in the subject's
     * principals,the alias's certificate path in the subject's public
     * credentials, and a {@code X500PrivateCredential} whose certificate
     * is the first  certificate in the alias's certificate path and whose
     * private key is the alias's private key in the subject's private
     * credentials.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * @exception LoginException if the commit fails
     *
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */

    public boolean commit() throws LoginException {
        switch (status) {
        case UNINITIALIZED:
        default:
            throw new LoginException("The login module is not initialized");
        case INITIALIZED:
            logoutInternal();
            throw new LoginException("Authentication failed");
        case AUTHENTICATED:
            if (commitInternal()) {
                return true;
            } else {
                logoutInternal();
                throw new LoginException("Unable to retrieve certificates");
            }
        case LOGGED_IN:
            return true;
        }
    }

    private boolean commitInternal() throws LoginException {
        /* If the subject is not readonly add to the principal and credentials
         * set; otherwise just return true
         */
        if (subject.isReadOnly()) {
            throw new LoginException ("Subject is set readonly");
        } else {
            subject.getPrincipals().add(principal);
            subject.getPublicCredentials().add(certP);
            subject.getPrivateCredentials().add(privateCredential);
            status = LOGGED_IN;
            return true;
        }
    }

    /**
     * This method is called if the LoginContext's
     * overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * did not succeed).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * {@code login} and {@code commit} methods),
     * then this method cleans up any state that was originally saved.
     *
     * <p> If the loaded KeyStore's provider extends
     * {@code java.security.AuthProvider},
     * then the provider's {@code logout} method is invoked.
     *
     * @exception LoginException if the abort fails.
     *
     * @return false if this LoginModule's own login and/or commit attempts
     *          failed, and true otherwise.
     */

    public boolean abort() throws LoginException {
        switch (status) {
        case UNINITIALIZED:
        default:
            return false;
        case INITIALIZED:
            return false;
        case AUTHENTICATED:
            logoutInternal();
            return true;
        case LOGGED_IN:
            logoutInternal();
            return true;
        }
    }
    /**
     * Logout a user.
     *
     * <p> This method removes the Principals, public credentials and the
     * private credentials that were added by the {@code commit} method.
     *
     * <p> If the loaded KeyStore's provider extends
     * {@code java.security.AuthProvider},
     * then the provider's {@code logout} method is invoked.
     *
     * @exception LoginException if the logout fails.
     *
     * @return true in all cases since this {@code LoginModule}
     *          should not be ignored.
     */

    public boolean logout() throws LoginException {
        if (debug)
            debugPrint("Entering logout " + status);
        switch (status) {
        case UNINITIALIZED:
            throw new LoginException
                ("The login module is not initialized");
        case INITIALIZED:
        case AUTHENTICATED:
        default:
           // impossible for LoginModule to be in AUTHENTICATED
           // state
           // assert status != AUTHENTICATED;
            return false;
        case LOGGED_IN:
            logoutInternal();
            return true;
        }
    }

    private void logoutInternal() throws LoginException {
        if (debug) {
            debugPrint("Entering logoutInternal");
        }

        // assumption is that KeyStore.load did a login -
        // perform explicit logout if possible
        LoginException logoutException = null;
        Provider provider = keyStore.getProvider();
        if (provider instanceof AuthProvider) {
            AuthProvider ap = (AuthProvider)provider;
            try {
                ap.logout();
                if (debug) {
                    debugPrint("logged out of KeyStore AuthProvider");
                }
            } catch (LoginException le) {
                // save but continue below
                logoutException = le;
            }
        }

        if (subject.isReadOnly()) {
            // attempt to destroy the private credential
            // even if the Subject is read-only
            principal = null;
            certP = null;
            status = INITIALIZED;
            // destroy the private credential
            Iterator<Object> it = subject.getPrivateCredentials().iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (privateCredential.equals(obj)) {
                    privateCredential = null;
                    try {
                        ((Destroyable)obj).destroy();
                        if (debug)
                            debugPrint("Destroyed private credential, " +
                                       obj.getClass().getName());
                        break;
                    } catch (DestroyFailedException dfe) {
                        LoginException le = new LoginException
                            ("Unable to destroy private credential, "
                             + obj.getClass().getName());
                        le.initCause(dfe);
                        throw le;
                    }
                }
            }

            // throw an exception because we can not remove
            // the principal and public credential from this
            // read-only Subject
            throw new LoginException
                ("Unable to remove Principal ("
                 + "X500Principal "
                 + ") and public credential (certificatepath) "
                 + "from read-only Subject");
        }
        if (principal != null) {
            subject.getPrincipals().remove(principal);
            principal = null;
        }
        if (certP != null) {
            subject.getPublicCredentials().remove(certP);
            certP = null;
        }
        if (privateCredential != null) {
            subject.getPrivateCredentials().remove(privateCredential);
            privateCredential = null;
        }

        // throw pending logout exception if there is one
        if (logoutException != null) {
            throw logoutException;
        }
        status = INITIALIZED;
    }

    private void debugPrint(String message) {
        // we should switch to logging API
        if (message == null) {
            System.err.println();
        } else {
            System.err.println("Debug KeyStoreLoginModule: " + message);
        }
    }
}
