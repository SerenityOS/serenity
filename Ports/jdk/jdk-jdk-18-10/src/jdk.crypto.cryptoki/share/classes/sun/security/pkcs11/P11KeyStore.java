/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.math.BigInteger;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import java.security.*;
import java.security.KeyStore.*;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;

import java.security.interfaces.*;
import java.security.spec.*;

import javax.crypto.SecretKey;
import javax.crypto.interfaces.*;

import javax.security.auth.x500.X500Principal;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import sun.security.util.Debug;
import sun.security.util.DerValue;
import sun.security.util.ECUtil;

import sun.security.pkcs11.Secmod.*;
import static sun.security.pkcs11.P11Util.*;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.PKCS11Exception.*;

import sun.security.rsa.RSAKeyFactory;

final class P11KeyStore extends KeyStoreSpi {

    private static final CK_ATTRIBUTE ATTR_CLASS_CERT =
                        new CK_ATTRIBUTE(CKA_CLASS, CKO_CERTIFICATE);
    private static final CK_ATTRIBUTE ATTR_CLASS_PKEY =
                        new CK_ATTRIBUTE(CKA_CLASS, CKO_PRIVATE_KEY);
    private static final CK_ATTRIBUTE ATTR_CLASS_SKEY =
                        new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY);

    private static final CK_ATTRIBUTE ATTR_X509_CERT_TYPE =
                        new CK_ATTRIBUTE(CKA_CERTIFICATE_TYPE, CKC_X_509);

    private static final CK_ATTRIBUTE ATTR_TOKEN_TRUE =
                        new CK_ATTRIBUTE(CKA_TOKEN, true);

    // XXX for testing purposes only
    //  - NSS doesn't support persistent secret keys
    //    (key type gets mangled if secret key is a token key)
    //  - if debug is turned on, then this is set to false
    private static CK_ATTRIBUTE ATTR_SKEY_TOKEN_TRUE = ATTR_TOKEN_TRUE;

    private static final CK_ATTRIBUTE ATTR_TRUSTED_TRUE =
                        new CK_ATTRIBUTE(CKA_TRUSTED, true);
    private static final CK_ATTRIBUTE ATTR_PRIVATE_TRUE =
                        new CK_ATTRIBUTE(CKA_PRIVATE, true);

    private static final long NO_HANDLE = -1;
    private static final long FINDOBJECTS_MAX = 100;
    private static final String ALIAS_SEP = "/";

    private static final boolean NSS_TEST = false;
    private static final Debug debug =
                        Debug.getInstance("pkcs11keystore");
    private static boolean CKA_TRUSTED_SUPPORTED = true;

    private final Token token;

    // If multiple certs are found to share the same CKA_LABEL
    // at load time (NSS-style keystore), then the keystore is read
    // and the unique keystore aliases are mapped to the entries.
    // However, write capabilities are disabled.
    private boolean writeDisabled = false;

    // Map of unique keystore aliases to entries in the token
    private HashMap<String, AliasInfo> aliasMap;

    // whether to use NSS Secmod info for trust attributes
    private final boolean useSecmodTrust;

    // if useSecmodTrust == true, which type of trust we are interested in
    private Secmod.TrustType nssTrustType;

    /**
     * The underlying token may contain multiple certs belonging to the
     * same "personality" (for example, a signing cert and encryption cert),
     * all sharing the same CKA_LABEL.  These must be resolved
     * into unique keystore aliases.
     *
     * In addition, private keys and certs may not have a CKA_LABEL.
     * It is assumed that a private key and corresponding certificate
     * share the same CKA_ID, and that the CKA_ID is unique across the token.
     * The CKA_ID may not be human-readable.
     * These pairs must be resolved into unique keystore aliases.
     *
     * Furthermore, secret keys are assumed to have a CKA_LABEL
     * unique across the entire token.
     *
     * When the KeyStore is loaded, instances of this class are
     * created to represent the private keys/secret keys/certs
     * that reside on the token.
     */
    private static class AliasInfo {

        // CKA_CLASS - entry type
        private CK_ATTRIBUTE type = null;

        // CKA_LABEL of cert and secret key
        private String label = null;

        // CKA_ID of the private key/cert pair
        private byte[] id = null;

        // CKA_TRUSTED - true if cert is trusted
        private boolean trusted = false;

        // either end-entity cert or trusted cert depending on 'type'
        private X509Certificate cert = null;

        // chain
        private X509Certificate[] chain = null;

        // true if CKA_ID for private key and cert match up
        private boolean matched = false;

        // SecretKeyEntry
        public AliasInfo(String label) {
            this.type = ATTR_CLASS_SKEY;
            this.label = label;
        }

        // PrivateKeyEntry
        public AliasInfo(String label,
                        byte[] id,
                        boolean trusted,
                        X509Certificate cert) {
            this.type = ATTR_CLASS_PKEY;
            this.label = label;
            this.id = id;
            this.trusted = trusted;
            this.cert = cert;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (type == ATTR_CLASS_PKEY) {
                sb.append("\ttype=[private key]\n");
            } else if (type == ATTR_CLASS_SKEY) {
                sb.append("\ttype=[secret key]\n");
            } else if (type == ATTR_CLASS_CERT) {
                sb.append("\ttype=[trusted cert]\n");
            }
            sb.append("\tlabel=[" + label + "]\n");
            if (id == null) {
                sb.append("\tid=[null]\n");
            } else {
                sb.append("\tid=" + P11KeyStore.getID(id) + "\n");
            }
            sb.append("\ttrusted=[" + trusted + "]\n");
            sb.append("\tmatched=[" + matched + "]\n");
            if (cert == null) {
                sb.append("\tcert=[null]\n");
            } else {
                sb.append("\tcert=[\tsubject: " +
                        cert.getSubjectX500Principal() +
                        "\n\t\tissuer: " +
                        cert.getIssuerX500Principal() +
                        "\n\t\tserialNum: " +
                        cert.getSerialNumber().toString() +
                        "]");
            }
            return sb.toString();
        }
    }

    /**
     * callback handler for passing password to Provider.login method
     */
    private static class PasswordCallbackHandler implements CallbackHandler {

        private char[] password;

        private PasswordCallbackHandler(char[] password) {
            if (password != null) {
                this.password = password.clone();
            }
        }

        public void handle(Callback[] callbacks)
                throws IOException, UnsupportedCallbackException {
            if (!(callbacks[0] instanceof PasswordCallback)) {
                throw new UnsupportedCallbackException(callbacks[0]);
            }
            PasswordCallback pc = (PasswordCallback)callbacks[0];
            pc.setPassword(password);  // this clones the password if not null
        }

        @SuppressWarnings("deprecation")
        protected void finalize() throws Throwable {
            if (password != null) {
                Arrays.fill(password, ' ');
            }
            super.finalize();
        }
    }

    /**
     * getTokenObject return value.
     *
     * if object is not found, type is set to null.
     * otherwise, type is set to the requested type.
     */
    private static class THandle {
        private final long handle;              // token object handle
        private final CK_ATTRIBUTE type;        // CKA_CLASS

        private THandle(long handle, CK_ATTRIBUTE type) {
            this.handle = handle;
            this.type = type;
        }
    }

    P11KeyStore(Token token) {
        this.token = token;
        this.useSecmodTrust = token.provider.nssUseSecmodTrust;
    }

    /**
     * Returns the key associated with the given alias.
     * The key must have been associated with
     * the alias by a call to <code>setKeyEntry</code>,
     * or by a call to <code>setEntry</code> with a
     * <code>PrivateKeyEntry</code> or <code>SecretKeyEntry</code>.
     *
     * @param alias the alias name
     * @param password the password, which must be <code>null</code>
     *
     * @return the requested key, or null if the given alias does not exist
     * or does not identify a key-related entry.
     *
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     * key cannot be found
     * @exception UnrecoverableKeyException if the key cannot be recovered
     */
    public synchronized Key engineGetKey(String alias, char[] password)
                throws NoSuchAlgorithmException, UnrecoverableKeyException {

        token.ensureValid();
        if (password != null && !token.config.getKeyStoreCompatibilityMode()) {
            throw new NoSuchAlgorithmException("password must be null");
        }

        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo == null || aliasInfo.type == ATTR_CLASS_CERT) {
            return null;
        }

        Session session = null;
        try {
            session = token.getOpSession();

            if (aliasInfo.type == ATTR_CLASS_PKEY) {
                THandle h = getTokenObject(session,
                                        aliasInfo.type,
                                        aliasInfo.id,
                                        null);
                if (h.type == ATTR_CLASS_PKEY) {
                    return loadPkey(session, h.handle);
                }
            } else {
                THandle h = getTokenObject(session,
                                        ATTR_CLASS_SKEY,
                                        null,
                                        alias);
                if (h.type == ATTR_CLASS_SKEY) {
                    return loadSkey(session, h.handle);
                }
            }

            // did not find anything
            return null;
        } catch (PKCS11Exception | KeyStoreException e) {
            throw new ProviderException(e);
        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * Returns the certificate chain associated with the given alias.
     * The certificate chain must have been associated with the alias
     * by a call to <code>setKeyEntry</code>,
     * or by a call to <code>setEntry</code> with a
     * <code>PrivateKeyEntry</code>.
     *
     * @param alias the alias name
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last), or null if the given alias
     * does not exist or does not contain a certificate chain
     */
    public synchronized Certificate[] engineGetCertificateChain(String alias) {

        token.ensureValid();

        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo == null || aliasInfo.type != ATTR_CLASS_PKEY) {
            return null;
        }
        return aliasInfo.chain;
    }

    /**
     * Returns the certificate associated with the given alias.
     *
     * <p> If the given alias name identifies an entry
     * created by a call to <code>setCertificateEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>TrustedCertificateEntry</code>,
     * then the trusted certificate contained in that entry is returned.
     *
     * <p> If the given alias name identifies an entry
     * created by a call to <code>setKeyEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>PrivateKeyEntry</code>,
     * then the first element of the certificate chain in that entry
     * (if a chain exists) is returned.
     *
     * @param alias the alias name
     *
     * @return the certificate, or null if the given alias does not exist or
     * does not contain a certificate.
     */
    public synchronized Certificate engineGetCertificate(String alias) {
        token.ensureValid();

        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo == null) {
            return null;
        }
        return aliasInfo.cert;
    }

    /**
     * Returns the creation date of the entry identified by the given alias.
     *
     * @param alias the alias name
     *
     * @return the creation date of this entry, or null if the given alias does
     * not exist
     */
    public Date engineGetCreationDate(String alias) {
        token.ensureValid();
        throw new ProviderException(new UnsupportedOperationException());
    }

    /**
     * Assigns the given key to the given alias, protecting it with the given
     * password.
     *
     * <p>If the given key is of type <code>java.security.PrivateKey</code>,
     * it must be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key to be associated with the alias
     * @param password the password to protect the key
     * @param chain the certificate chain for the corresponding public
     * key (only required if the given key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if the given key cannot be protected, or
     * this operation fails for some other reason
     */
    public synchronized void engineSetKeyEntry(String alias, Key key,
                                   char[] password,
                                   Certificate[] chain)
                throws KeyStoreException {

        token.ensureValid();
        checkWrite();

        if (!(key instanceof PrivateKey) && !(key instanceof SecretKey)) {
            throw new KeyStoreException("key must be PrivateKey or SecretKey");
        } else if (key instanceof PrivateKey && chain == null) {
            throw new KeyStoreException
                ("PrivateKey must be accompanied by non-null chain");
        } else if (key instanceof SecretKey && chain != null) {
            throw new KeyStoreException
                ("SecretKey must be accompanied by null chain");
        } else if (password != null &&
                    !token.config.getKeyStoreCompatibilityMode()) {
            throw new KeyStoreException("Password must be null");
        }

        KeyStore.Entry entry = null;
        try {
            if (key instanceof PrivateKey) {
                entry = new KeyStore.PrivateKeyEntry((PrivateKey)key, chain);
            } else if (key instanceof SecretKey) {
                entry = new KeyStore.SecretKeyEntry((SecretKey)key);
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new KeyStoreException(e);
        }
        engineSetEntry(alias, entry, new KeyStore.PasswordProtection(password));
    }

    /**
     * Assigns the given key (that has already been protected) to the given
     * alias.
     *
     * <p>If the protected key is of type
     * <code>java.security.PrivateKey</code>,
     * it must be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key (in protected format) to be associated with the alias
     * @param chain the certificate chain for the corresponding public
     * key (only useful if the protected key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if this operation fails.
     */
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
                throws KeyStoreException {
        token.ensureValid();
        throw new ProviderException(new UnsupportedOperationException());
    }

    /**
     * Assigns the given certificate to the given alias.
     *
     * <p> If the given alias identifies an existing entry
     * created by a call to <code>setCertificateEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>TrustedCertificateEntry</code>,
     * the trusted certificate in the existing entry
     * is overridden by the given certificate.
     *
     * @param alias the alias name
     * @param cert the certificate
     *
     * @exception KeyStoreException if the given alias already exists and does
     * not identify an entry containing a trusted certificate,
     * or this operation fails for some other reason.
     */
    public synchronized void engineSetCertificateEntry
        (String alias, Certificate cert) throws KeyStoreException {

        token.ensureValid();
        checkWrite();

        if (cert == null) {
            throw new KeyStoreException("invalid null certificate");
        }

        KeyStore.Entry entry = null;
        entry = new KeyStore.TrustedCertificateEntry(cert);
        engineSetEntry(alias, entry, null);
    }

    /**
     * Deletes the entry identified by the given alias from this keystore.
     *
     * @param alias the alias name
     *
     * @exception KeyStoreException if the entry cannot be removed.
     */
    public synchronized void engineDeleteEntry(String alias)
                throws KeyStoreException {
        token.ensureValid();

        if (token.isWriteProtected()) {
            throw new KeyStoreException("token write-protected");
        }
        checkWrite();
        deleteEntry(alias);
    }

    /**
     * XXX - not sure whether to keep this
     */
    private boolean deleteEntry(String alias) throws KeyStoreException {
        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo != null) {

            aliasMap.remove(alias);

            try {
                if (aliasInfo.type == ATTR_CLASS_CERT) {
                    // trusted certificate entry
                    return destroyCert(aliasInfo.id);
                } else if (aliasInfo.type == ATTR_CLASS_PKEY) {
                    // private key entry
                    return destroyPkey(aliasInfo.id) &&
                                destroyChain(aliasInfo.id);
                } else if (aliasInfo.type == ATTR_CLASS_SKEY) {
                    // secret key entry
                    return destroySkey(alias);
                } else {
                    throw new KeyStoreException("unexpected entry type");
                }
            } catch (PKCS11Exception | CertificateException e) {
                throw new KeyStoreException(e);
            }
        }
        return false;
    }

    /**
     * Lists all the alias names of this keystore.
     *
     * @return enumeration of the alias names
     */
    public synchronized Enumeration<String> engineAliases() {
        token.ensureValid();

        // don't want returned enumeration to iterate off actual keySet -
        // otherwise applications that iterate and modify the keystore
        // may run into concurrent modification problems
        return Collections.enumeration(new HashSet<String>(aliasMap.keySet()));
    }

    /**
     * Checks if the given alias exists in this keystore.
     *
     * @param alias the alias name
     *
     * @return true if the alias exists, false otherwise
     */
    public synchronized boolean engineContainsAlias(String alias) {
        token.ensureValid();
        return aliasMap.containsKey(alias);
    }

    /**
     * Retrieves the number of entries in this keystore.
     *
     * @return the number of entries in this keystore
     */
    public synchronized int engineSize() {
        token.ensureValid();
        return aliasMap.size();
    }

    /**
     * Returns true if the entry identified by the given alias
     * was created by a call to <code>setKeyEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>PrivateKeyEntry</code> or a <code>SecretKeyEntry</code>.
     *
     * @param alias the alias for the keystore entry to be checked
     *
     * @return true if the entry identified by the given alias is a
     * key-related, false otherwise.
     */
    public synchronized boolean engineIsKeyEntry(String alias) {
        token.ensureValid();

        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo == null || aliasInfo.type == ATTR_CLASS_CERT) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the entry identified by the given alias
     * was created by a call to <code>setCertificateEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>TrustedCertificateEntry</code>.
     *
     * @param alias the alias for the keystore entry to be checked
     *
     * @return true if the entry identified by the given alias contains a
     * trusted certificate, false otherwise.
     */
    public synchronized boolean engineIsCertificateEntry(String alias) {
        token.ensureValid();

        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo == null || aliasInfo.type != ATTR_CLASS_CERT) {
            return false;
        }
        return true;
    }

    /**
     * Returns the (alias) name of the first keystore entry whose certificate
     * matches the given certificate.
     *
     * <p>This method attempts to match the given certificate with each
     * keystore entry. If the entry being considered was
     * created by a call to <code>setCertificateEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>TrustedCertificateEntry</code>,
     * then the given certificate is compared to that entry's certificate.
     *
     * <p> If the entry being considered was
     * created by a call to <code>setKeyEntry</code>,
     * or created by a call to <code>setEntry</code> with a
     * <code>PrivateKeyEntry</code>,
     * then the given certificate is compared to the first
     * element of that entry's certificate chain.
     *
     * @param cert the certificate to match with.
     *
     * @return the alias name of the first entry with matching certificate,
     * or null if no such entry exists in this keystore.
     */
    public synchronized String engineGetCertificateAlias(Certificate cert) {
        token.ensureValid();
        Enumeration<String> e = engineAliases();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            Certificate tokenCert = engineGetCertificate(alias);
            if (tokenCert != null && tokenCert.equals(cert)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * engineStore currently is a No-op.
     * Entries are stored to the token during engineSetEntry
     *
     * @param stream this must be <code>null</code>
     * @param password this must be <code>null</code>
     */
    public synchronized void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException {
        token.ensureValid();
        if (stream != null && !token.config.getKeyStoreCompatibilityMode()) {
            throw new IOException("output stream must be null");
        }

        if (password != null && !token.config.getKeyStoreCompatibilityMode()) {
            throw new IOException("password must be null");
        }
    }

    /**
     * engineStore currently is a No-op.
     * Entries are stored to the token during engineSetEntry
     *
     * @param param this must be <code>null</code>
     *
     * @exception IllegalArgumentException if the given
     *          <code>KeyStore.LoadStoreParameter</code>
     *          input is not <code>null</code>
     */
    public synchronized void engineStore(KeyStore.LoadStoreParameter param)
        throws IOException, NoSuchAlgorithmException, CertificateException {
        token.ensureValid();
        if (param != null) {
            throw new IllegalArgumentException
                ("LoadStoreParameter must be null");
        }
    }

    /**
     * Loads the keystore.
     *
     * @param stream the input stream, which must be <code>null</code>
     * @param password the password used to unlock the keystore,
     *          or <code>null</code> if the token supports a
     *          CKF_PROTECTED_AUTHENTICATION_PATH
     *
     * @exception IOException if the given <code>stream</code> is not
     *          <code>null</code>, if the token supports a
     *          CKF_PROTECTED_AUTHENTICATION_PATH and a non-null
     *          password is given, of if the token login operation failed
     */
    public synchronized void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException {

        token.ensureValid();

        if (NSS_TEST) {
            ATTR_SKEY_TOKEN_TRUE = new CK_ATTRIBUTE(CKA_TOKEN, false);
        }

        if (stream != null && !token.config.getKeyStoreCompatibilityMode()) {
            throw new IOException("input stream must be null");
        }

        if (useSecmodTrust) {
            nssTrustType = Secmod.TrustType.ALL;
        }

        try {
            if (password == null) {
                login(null);
            } else {
                login(new PasswordCallbackHandler(password));
            }
        } catch(LoginException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PKCS11Exception) {
                PKCS11Exception pe = (PKCS11Exception) cause;
                if (pe.getErrorCode() == CKR_PIN_INCORRECT) {
                    // if password is wrong, the cause of the IOException
                    // should be an UnrecoverableKeyException
                    throw new IOException("load failed",
                            new UnrecoverableKeyException().initCause(e));
                }
            }
            throw new IOException("load failed", e);
        }

        try {
            if (mapLabels() == true) {
                // CKA_LABELs are shared by multiple certs
                writeDisabled = true;
            }
            if (debug != null) {
                dumpTokenMap();
                debug.println("P11KeyStore load. Entry count: " +
                        aliasMap.size());
            }
        } catch (KeyStoreException | PKCS11Exception e) {
            throw new IOException("load failed", e);
        }
    }

    /**
     * Loads the keystore using the given
     * <code>KeyStore.LoadStoreParameter</code>.
     *
     * <p> The <code>LoadStoreParameter.getProtectionParameter()</code>
     * method is expected to return a <code>KeyStore.PasswordProtection</code>
     * object.  The password is retrieved from that object and used
     * to unlock the PKCS#11 token.
     *
     * <p> If the token supports a CKF_PROTECTED_AUTHENTICATION_PATH
     * then the provided password must be <code>null</code>.
     *
     * @param param the <code>KeyStore.LoadStoreParameter</code>
     *
     * @exception IllegalArgumentException if the given
     *          <code>KeyStore.LoadStoreParameter</code> is <code>null</code>,
     *          or if that parameter returns a <code>null</code>
     *          <code>ProtectionParameter</code> object.
     *          input is not recognized
     * @exception IOException if the token supports a
     *          CKF_PROTECTED_AUTHENTICATION_PATH and the provided password
     *          is non-null, or if the token login operation fails
     */
    public synchronized void engineLoad(KeyStore.LoadStoreParameter param)
                throws IOException, NoSuchAlgorithmException,
                CertificateException {

        token.ensureValid();

        if (NSS_TEST) {
            ATTR_SKEY_TOKEN_TRUE = new CK_ATTRIBUTE(CKA_TOKEN, false);
        }

        // if caller wants to pass a NULL password,
        // force it to pass a non-NULL PasswordProtection that returns
        // a NULL password

        if (param == null) {
            throw new IllegalArgumentException
                        ("invalid null LoadStoreParameter");
        }
        if (useSecmodTrust) {
            if (param instanceof Secmod.KeyStoreLoadParameter) {
                nssTrustType = ((Secmod.KeyStoreLoadParameter)param).getTrustType();
            } else {
                nssTrustType = Secmod.TrustType.ALL;
            }
        }

        CallbackHandler handler;
        KeyStore.ProtectionParameter pp = param.getProtectionParameter();
        if (pp instanceof PasswordProtection) {
            char[] password = ((PasswordProtection)pp).getPassword();
            if (password == null) {
                handler = null;
            } else {
                handler = new PasswordCallbackHandler(password);
            }
        } else if (pp instanceof CallbackHandlerProtection) {
            handler = ((CallbackHandlerProtection)pp).getCallbackHandler();
        } else {
            throw new IllegalArgumentException
                        ("ProtectionParameter must be either " +
                        "PasswordProtection or CallbackHandlerProtection");
        }

        try {
            login(handler);
            if (mapLabels() == true) {
                // CKA_LABELs are shared by multiple certs
                writeDisabled = true;
            }
            if (debug != null) {
                dumpTokenMap();
            }
        } catch (LoginException | KeyStoreException | PKCS11Exception e) {
            throw new IOException("load failed", e);
        }
    }

    private void login(CallbackHandler handler) throws LoginException {
        if ((token.tokenInfo.flags & CKF_PROTECTED_AUTHENTICATION_PATH) == 0) {
            token.provider.login(null, handler);
        } else {
            // token supports protected authentication path
            // (external pin-pad, for example)
            if (handler != null &&
                !token.config.getKeyStoreCompatibilityMode()) {
                throw new LoginException("can not specify password if token " +
                                "supports protected authentication path");
            }

            // must rely on application-set or default handler
            // if one is necessary
            token.provider.login(null, null);
        }
    }

    /**
     * Get a <code>KeyStore.Entry</code> for the specified alias
     *
     * @param alias get the <code>KeyStore.Entry</code> for this alias
     * @param protParam this must be <code>null</code>
     *
     * @return the <code>KeyStore.Entry</code> for the specified alias,
     *          or <code>null</code> if there is no such entry
     *
     * @exception KeyStoreException if the operation failed
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     *          entry cannot be found
     * @exception UnrecoverableEntryException if the specified
     *          <code>protParam</code> were insufficient or invalid
     *
     * @since 1.5
     */
    public synchronized KeyStore.Entry engineGetEntry(String alias,
                        KeyStore.ProtectionParameter protParam)
                throws KeyStoreException, NoSuchAlgorithmException,
                UnrecoverableEntryException {

        token.ensureValid();

        if (protParam != null &&
            protParam instanceof KeyStore.PasswordProtection &&
            ((KeyStore.PasswordProtection)protParam).getPassword() != null &&
            !token.config.getKeyStoreCompatibilityMode()) {
            throw new KeyStoreException("ProtectionParameter must be null");
        }

        AliasInfo aliasInfo = aliasMap.get(alias);
        if (aliasInfo == null) {
            if (debug != null) {
                debug.println("engineGetEntry did not find alias [" +
                        alias +
                        "] in map");
            }
            return null;
        }

        Session session = null;
        try {
            session = token.getOpSession();

            if (aliasInfo.type == ATTR_CLASS_CERT) {
                // trusted certificate entry
                if (debug != null) {
                    debug.println("engineGetEntry found trusted cert entry");
                }
                return new KeyStore.TrustedCertificateEntry(aliasInfo.cert);
            } else if (aliasInfo.type == ATTR_CLASS_SKEY) {
                // secret key entry
                if (debug != null) {
                    debug.println("engineGetEntry found secret key entry");
                }

                THandle h = getTokenObject
                        (session, ATTR_CLASS_SKEY, null, aliasInfo.label);
                if (h.type != ATTR_CLASS_SKEY) {
                    throw new KeyStoreException
                        ("expected but could not find secret key");
                } else {
                    SecretKey skey = loadSkey(session, h.handle);
                    return new KeyStore.SecretKeyEntry(skey);
                }
            } else {
                // private key entry
                if (debug != null) {
                    debug.println("engineGetEntry found private key entry");
                }

                THandle h = getTokenObject
                        (session, ATTR_CLASS_PKEY, aliasInfo.id, null);
                if (h.type != ATTR_CLASS_PKEY) {
                    throw new KeyStoreException
                        ("expected but could not find private key");
                } else {
                    PrivateKey pkey = loadPkey(session, h.handle);
                    Certificate[] chain = aliasInfo.chain;
                    if ((pkey != null) && (chain != null)) {
                        return new KeyStore.PrivateKeyEntry(pkey, chain);
                    } else {
                        if (debug != null) {
                            debug.println
                                ("engineGetEntry got null cert chain or private key");
                        }
                    }
                }
            }
            return null;
        } catch (PKCS11Exception pe) {
            throw new KeyStoreException(pe);
        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * Save a <code>KeyStore.Entry</code> under the specified alias.
     *
     * <p> If an entry already exists for the specified alias,
     * it is overridden.
     *
     * <p> This KeyStore implementation only supports the standard
     * entry types, and only supports X509Certificates in
     * TrustedCertificateEntries.  Also, this implementation does not support
     * protecting entries using a different password
     * from the one used for token login.
     *
     * <p> Entries are immediately stored on the token.
     *
     * @param alias save the <code>KeyStore.Entry</code> under this alias
     * @param entry the <code>Entry</code> to save
     * @param protParam this must be <code>null</code>
     *
     * @exception KeyStoreException if this operation fails
     *
     * @since 1.5
     */
    public synchronized void engineSetEntry(String alias, KeyStore.Entry entry,
                        KeyStore.ProtectionParameter protParam)
                throws KeyStoreException {

        token.ensureValid();
        checkWrite();

        if (protParam != null &&
            protParam instanceof KeyStore.PasswordProtection &&
            ((KeyStore.PasswordProtection)protParam).getPassword() != null &&
            !token.config.getKeyStoreCompatibilityMode()) {
            throw new KeyStoreException(new UnsupportedOperationException
                                ("ProtectionParameter must be null"));
        }

        if (token.isWriteProtected()) {
            throw new KeyStoreException("token write-protected");
        }

        if (entry instanceof KeyStore.TrustedCertificateEntry) {

            if (useSecmodTrust == false) {
                // PKCS #11 does not allow app to modify trusted certs -
                throw new KeyStoreException(new UnsupportedOperationException
                                    ("trusted certificates may only be set by " +
                                    "token initialization application"));
            }
            Secmod.Module module = token.provider.nssModule;
            if ((module.type != ModuleType.KEYSTORE) && (module.type != ModuleType.FIPS)) {
                // XXX allow TRUSTANCHOR module
                throw new KeyStoreException("Trusted certificates can only be "
                    + "added to the NSS KeyStore module");
            }
            Certificate cert = ((TrustedCertificateEntry)entry).getTrustedCertificate();
            if (cert instanceof X509Certificate == false) {
                throw new KeyStoreException("Certificate must be an X509Certificate");
            }
            X509Certificate xcert = (X509Certificate)cert;
            AliasInfo info = aliasMap.get(alias);
            if (info != null) {
                // XXX try to update
                deleteEntry(alias);
            }
            try {
                storeCert(alias, xcert);
                module.setTrust(token, xcert);
                mapLabels();
            } catch (PKCS11Exception | CertificateException e) {
                throw new KeyStoreException(e);
            }

        } else {

            if (entry instanceof KeyStore.PrivateKeyEntry) {

                PrivateKey key =
                        ((KeyStore.PrivateKeyEntry)entry).getPrivateKey();
                if (!(key instanceof P11Key) &&
                    !(key instanceof RSAPrivateKey) &&
                    !(key instanceof DSAPrivateKey) &&
                    !(key instanceof DHPrivateKey) &&
                    !(key instanceof ECPrivateKey)) {
                    throw new KeyStoreException("unsupported key type: " +
                                                key.getClass().getName());
                }

                // only support X509Certificate chains
                Certificate[] chain =
                    ((KeyStore.PrivateKeyEntry)entry).getCertificateChain();
                if (!(chain instanceof X509Certificate[])) {
                    throw new KeyStoreException
                        (new UnsupportedOperationException
                                ("unsupported certificate array type: " +
                                chain.getClass().getName()));
                }

                try {
                    boolean updatedAlias = false;
                    Set<String> aliases = aliasMap.keySet();
                    for (String oldAlias : aliases) {

                        // see if there's an existing entry with the same info

                        AliasInfo aliasInfo = aliasMap.get(oldAlias);
                        if (aliasInfo.type == ATTR_CLASS_PKEY &&
                            aliasInfo.cert.getPublicKey().equals
                                        (chain[0].getPublicKey())) {

                            // found existing entry -
                            // caller is renaming entry or updating cert chain
                            //
                            // set new CKA_LABEL/CKA_ID
                            // and update certs if necessary

                            updatePkey(alias,
                                        aliasInfo.id,
                                        (X509Certificate[])chain,
                                        !aliasInfo.cert.equals(chain[0]));
                            updatedAlias = true;
                            break;
                        }
                    }

                    if (!updatedAlias) {
                        // caller adding new entry
                        engineDeleteEntry(alias);
                        storePkey(alias, (KeyStore.PrivateKeyEntry)entry);
                    }

                } catch (PKCS11Exception | CertificateException pe) {
                    throw new KeyStoreException(pe);
                }

            } else if (entry instanceof KeyStore.SecretKeyEntry) {

                KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry)entry;
                SecretKey skey = ske.getSecretKey();

                try {
                    // first check if the key already exists
                    AliasInfo aliasInfo = aliasMap.get(alias);

                    if (aliasInfo != null) {
                        engineDeleteEntry(alias);
                    }
                    storeSkey(alias, ske);

                } catch (PKCS11Exception pe) {
                    throw new KeyStoreException(pe);
                }

            } else {
                throw new KeyStoreException(new UnsupportedOperationException
                    ("unsupported entry type: " + entry.getClass().getName()));
            }

            try {

                // XXX  NSS does not write out the CKA_ID we pass to them
                //
                // therefore we must re-map labels
                // (can not simply update aliasMap)

                mapLabels();
                if (debug != null) {
                    dumpTokenMap();
                }
            } catch (PKCS11Exception | CertificateException pe) {
                throw new KeyStoreException(pe);
            }
        }

        if (debug != null) {
            debug.println
                ("engineSetEntry added new entry for [" +
                alias +
                "] to token");
        }
    }

    /**
     * Determines if the keystore <code>Entry</code> for the specified
     * <code>alias</code> is an instance or subclass of the specified
     * <code>entryClass</code>.
     *
     * @param alias the alias name
     * @param entryClass the entry class
     *
     * @return true if the keystore <code>Entry</code> for the specified
     *          <code>alias</code> is an instance or subclass of the
     *          specified <code>entryClass</code>, false otherwise
     */
    public synchronized boolean engineEntryInstanceOf
                (String alias, Class<? extends KeyStore.Entry> entryClass) {
        token.ensureValid();
        return super.engineEntryInstanceOf(alias, entryClass);
    }

    private X509Certificate loadCert(Session session, long oHandle)
                throws PKCS11Exception, CertificateException {

        CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[]
                        { new CK_ATTRIBUTE(CKA_VALUE) };
        token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);

        byte[] bytes = attrs[0].getByteArray();
        if (bytes == null) {
            throw new CertificateException
                        ("unexpectedly retrieved null byte array");
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)cf.generateCertificate
                        (new ByteArrayInputStream(bytes));
    }

    private X509Certificate[] loadChain(Session session,
                                        X509Certificate endCert)
                throws PKCS11Exception, CertificateException {

        ArrayList<X509Certificate> lChain = null;

        if (endCert.getSubjectX500Principal().equals
            (endCert.getIssuerX500Principal())) {
            // self signed
            return new X509Certificate[] { endCert };
        } else {
            lChain = new ArrayList<X509Certificate>();
            lChain.add(endCert);
        }

        // try loading remaining certs in chain by following
        // issuer->subject links

        X509Certificate next = endCert;
        while (true) {
            CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                        ATTR_TOKEN_TRUE,
                        ATTR_CLASS_CERT,
                        new CK_ATTRIBUTE(CKA_SUBJECT,
                                next.getIssuerX500Principal().getEncoded()) };
            long[] ch = findObjects(session, attrs);

            if (ch == null || ch.length == 0) {
                // done
                break;
            } else {
                // if more than one found, use first
                if (debug != null && ch.length > 1) {
                    debug.println("engineGetEntry found " +
                                ch.length +
                                " certificate entries for subject [" +
                                next.getIssuerX500Principal().toString() +
                                "] in token - using first entry");
                }

                next = loadCert(session, ch[0]);
                lChain.add(next);
                if (next.getSubjectX500Principal().equals
                    (next.getIssuerX500Principal())) {
                    // self signed
                    break;
                }
            }
        }

        return lChain.toArray(new X509Certificate[lChain.size()]);
    }

    private SecretKey loadSkey(Session session, long oHandle)
                throws PKCS11Exception {

        CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                        new CK_ATTRIBUTE(CKA_KEY_TYPE) };
        token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
        long kType = attrs[0].getLong();

        String keyType = null;
        int keyLength = -1;

        // XXX NSS mangles the stored key type for secret key token objects

        if (kType == CKK_DES || kType == CKK_DES3) {
            if (kType == CKK_DES) {
                keyType = "DES";
                keyLength = 64;
            } else if (kType == CKK_DES3) {
                keyType = "DESede";
                keyLength = 192;
            }
        } else {
            if (kType == CKK_AES) {
                keyType = "AES";
            } else if (kType == CKK_BLOWFISH) {
                keyType = "Blowfish";
            } else if (kType == CKK_RC4) {
                keyType = "ARCFOUR";
            } else {
                if (debug != null) {
                    debug.println("unknown key type [" +
                                kType +
                                "] - using 'Generic Secret'");
                }
                keyType = "Generic Secret";
            }

            // XXX NSS problem CKR_ATTRIBUTE_TYPE_INVALID?
            if (NSS_TEST) {
                keyLength = 128;
            } else {
                attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_VALUE_LEN) };
                token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
                keyLength = (int)attrs[0].getLong();
            }
        }

        return P11Key.secretKey(session, oHandle, keyType, keyLength, null);
    }

    private PrivateKey loadPkey(Session session, long oHandle)
        throws PKCS11Exception, KeyStoreException {

        CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                        new CK_ATTRIBUTE(CKA_KEY_TYPE) };
        token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
        long kType = attrs[0].getLong();
        String keyType = null;
        int keyLength = 0;

        if (kType == CKK_RSA) {

            keyType = "RSA";

            attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_MODULUS) };
            token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
            BigInteger modulus = attrs[0].getBigInteger();
            keyLength = modulus.bitLength();

            // This check will combine our "don't care" values here
            // with the system-wide min/max values.
            try {
                RSAKeyFactory.checkKeyLengths(keyLength, null,
                    -1, Integer.MAX_VALUE);
            } catch (InvalidKeyException e) {
                throw new KeyStoreException(e.getMessage());
            }

            return P11Key.privateKey(session,
                                oHandle,
                                keyType,
                                keyLength,
                                null);

        } else if (kType == CKK_DSA) {

            keyType = "DSA";

            attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_PRIME) };
            token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
            BigInteger prime = attrs[0].getBigInteger();
            keyLength = prime.bitLength();

            return P11Key.privateKey(session,
                                oHandle,
                                keyType,
                                keyLength,
                                null);

        } else if (kType == CKK_DH) {

            keyType = "DH";

            attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_PRIME) };
            token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
            BigInteger prime = attrs[0].getBigInteger();
            keyLength = prime.bitLength();

            return P11Key.privateKey(session,
                                oHandle,
                                keyType,
                                keyLength,
                                null);

        } else if (kType == CKK_EC) {

            attrs = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_EC_PARAMS),
            };
            token.p11.C_GetAttributeValue(session.id(), oHandle, attrs);
            byte[] encodedParams = attrs[0].getByteArray();
            try {
                ECParameterSpec params =
                    ECUtil.getECParameterSpec(null, encodedParams);
                keyLength = params.getCurve().getField().getFieldSize();
            } catch (IOException e) {
                // we do not want to accept key with unsupported parameters
                throw new KeyStoreException("Unsupported parameters", e);
            }

            return P11Key.privateKey(session, oHandle, "EC", keyLength, null);

        } else {
            if (debug != null) {
                debug.println("unknown key type [" + kType + "]");
            }
            throw new KeyStoreException("unknown key type");
        }
    }


    /**
     * XXX  On ibutton, when you C_SetAttribute(CKA_ID) for a private key
     *      it not only changes the CKA_ID of the private key,
     *      it changes the CKA_ID of the corresponding cert too.
     *      And vice versa.
     *
     * XXX  On ibutton, CKR_DEVICE_ERROR if you C_SetAttribute(CKA_ID)
     *      for a private key, and then try to delete the corresponding cert.
     *      So this code reverses the order.
     *      After the cert is first destroyed (if necessary),
     *      then the CKA_ID of the private key can be changed successfully.
     *
     * @param replaceCert if true, then caller is updating alias info for
     *                  existing cert (only update CKA_ID/CKA_LABEL).
     *                  if false, then caller is updating cert chain
     *                  (delete old end cert and add new chain).
     */
    private void updatePkey(String alias,
                        byte[] cka_id,
                        X509Certificate[] chain,
                        boolean replaceCert) throws
                KeyStoreException, CertificateException, PKCS11Exception {

        // XXX
        //
        // always set replaceCert to true
        //
        // NSS does not allow resetting of CKA_LABEL on an existing cert
        // (C_SetAttribute call succeeds, but is ignored)

        replaceCert = true;

        Session session = null;
        try {
            session = token.getOpSession();

            // first get private key object handle and hang onto it

            THandle h = getTokenObject(session, ATTR_CLASS_PKEY, cka_id, null);
            long pKeyHandle;
            if (h.type == ATTR_CLASS_PKEY) {
                pKeyHandle = h.handle;
            } else {
                throw new KeyStoreException
                        ("expected but could not find private key " +
                        "with CKA_ID " +
                        getID(cka_id));
            }

            // next find existing end entity cert

            h = getTokenObject(session, ATTR_CLASS_CERT, cka_id, null);
            if (h.type != ATTR_CLASS_CERT) {
                throw new KeyStoreException
                        ("expected but could not find certificate " +
                        "with CKA_ID " +
                        getID(cka_id));
            } else {
                if (replaceCert) {
                    // replacing existing cert and chain
                    destroyChain(cka_id);
                } else {
                    // renaming alias for existing cert
                    CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                        new CK_ATTRIBUTE(CKA_LABEL, alias),
                        new CK_ATTRIBUTE(CKA_ID, alias) };
                    token.p11.C_SetAttributeValue
                        (session.id(), h.handle, attrs);
                }
            }

            // add new chain

            if (replaceCert) {
                // add all certs in chain
                storeChain(alias, chain);
            } else {
                // already updated alias info for existing end cert -
                // just update CA certs
                storeCaCerts(chain, 1);
            }

            // finally update CKA_ID for private key
            //
            // ibutton may have already done this (that is ok)

            CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                                new CK_ATTRIBUTE(CKA_ID, alias) };
            token.p11.C_SetAttributeValue(session.id(), pKeyHandle, attrs);

            if (debug != null) {
                debug.println("updatePkey set new alias [" +
                                alias +
                                "] for private key entry");
            }
        } finally {
            token.releaseSession(session);
        }
    }

    // retrieves the native key handle and either update it directly or make a copy
    private void updateP11Pkey(String alias, CK_ATTRIBUTE attribute, P11Key key)
                throws PKCS11Exception {

        // if token key, update alias.
        // if session key, convert to token key.

        Session session = null;
        long keyID = key.getKeyID();
        try {
            session = token.getOpSession();
            if (key.tokenObject == true) {
                // token key - set new CKA_ID

                CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                                new CK_ATTRIBUTE(CKA_ID, alias) };
                token.p11.C_SetAttributeValue
                                (session.id(), keyID, attrs);
                if (debug != null) {
                    debug.println("updateP11Pkey set new alias [" +
                                alias +
                                "] for key entry");
                }
            } else {
                // session key - convert to token key and set CKA_ID

                CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                    ATTR_TOKEN_TRUE,
                    new CK_ATTRIBUTE(CKA_ID, alias),
                };
                if (attribute != null) {
                    attrs = addAttribute(attrs, attribute);
                }
                // creates a new token key with the desired CKA_ID
                token.p11.C_CopyObject(session.id(), keyID, attrs);
                if (debug != null) {
                    debug.println("updateP11Pkey copied private session key " +
                                "for [" +
                                alias +
                                "] to token entry");
                }
            }
        } finally {
            token.releaseSession(session);
            key.releaseKeyID();
        }
    }

    private void storeCert(String alias, X509Certificate cert)
                throws PKCS11Exception, CertificateException {

        ArrayList<CK_ATTRIBUTE> attrList = new ArrayList<CK_ATTRIBUTE>();
        attrList.add(ATTR_TOKEN_TRUE);
        attrList.add(ATTR_CLASS_CERT);
        attrList.add(ATTR_X509_CERT_TYPE);
        attrList.add(new CK_ATTRIBUTE(CKA_SUBJECT,
                                cert.getSubjectX500Principal().getEncoded()));
        attrList.add(new CK_ATTRIBUTE(CKA_ISSUER,
                                cert.getIssuerX500Principal().getEncoded()));
        attrList.add(new CK_ATTRIBUTE(CKA_SERIAL_NUMBER,
                                cert.getSerialNumber().toByteArray()));
        attrList.add(new CK_ATTRIBUTE(CKA_VALUE, cert.getEncoded()));

        if (alias != null) {
            attrList.add(new CK_ATTRIBUTE(CKA_LABEL, alias));
            attrList.add(new CK_ATTRIBUTE(CKA_ID, alias));
        } else {
            // ibutton requires something to be set
            // - alias must be unique
            attrList.add(new CK_ATTRIBUTE(CKA_ID,
                        getID(cert.getSubjectX500Principal().getName
                                        (X500Principal.CANONICAL), cert)));
        }

        Session session = null;
        try {
            session = token.getOpSession();
            token.p11.C_CreateObject(session.id(),
                        attrList.toArray(new CK_ATTRIBUTE[attrList.size()]));
        } finally {
            token.releaseSession(session);
        }
    }

    private void storeChain(String alias, X509Certificate[] chain)
                throws PKCS11Exception, CertificateException {

        // add new chain
        //
        // end cert has CKA_LABEL and CKA_ID set to alias.
        // other certs in chain have neither set.

        storeCert(alias, chain[0]);
        storeCaCerts(chain, 1);
    }

    private void storeCaCerts(X509Certificate[] chain, int start)
                throws PKCS11Exception, CertificateException {

        // do not add duplicate CA cert if already in token
        //
        // XXX   ibutton stores duplicate CA certs, NSS does not

        Session session = null;
        HashSet<X509Certificate> cacerts = new HashSet<X509Certificate>();
        try {
            session = token.getOpSession();
            CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                        ATTR_TOKEN_TRUE,
                        ATTR_CLASS_CERT };
            long[] handles = findObjects(session, attrs);

            // load certs currently on the token
            for (long handle : handles) {
                cacerts.add(loadCert(session, handle));
            }
        } finally {
            token.releaseSession(session);
        }

        for (int i = start; i < chain.length; i++) {
            if (!cacerts.contains(chain[i])) {
                storeCert(null, chain[i]);
            } else if (debug != null) {
                debug.println("ignoring duplicate CA cert for [" +
                        chain[i].getSubjectX500Principal() +
                        "]");
            }
        }
    }

    private void storeSkey(String alias, KeyStore.SecretKeyEntry ske)
                throws PKCS11Exception, KeyStoreException {

        SecretKey skey = ske.getSecretKey();
        // No need to specify CKA_CLASS, CKA_KEY_TYPE, CKA_VALUE since
        // they are handled in P11SecretKeyFactory.createKey() method.
        CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
            ATTR_SKEY_TOKEN_TRUE,
            ATTR_PRIVATE_TRUE,
            new CK_ATTRIBUTE(CKA_LABEL, alias),
        };
        try {
            P11SecretKeyFactory.convertKey(token, skey, null, attrs);
        } catch (InvalidKeyException ike) {
            // re-throw KeyStoreException to match javadoc
            throw new KeyStoreException("Cannot convert to PKCS11 keys", ike);
        }

        // update global alias map
        aliasMap.put(alias, new AliasInfo(alias));

        if (debug != null) {
            debug.println("storeSkey created token secret key for [" +
                          alias + "]");
        }
    }

    private static CK_ATTRIBUTE[] addAttribute(CK_ATTRIBUTE[] attrs, CK_ATTRIBUTE attr) {
        int n = attrs.length;
        CK_ATTRIBUTE[] newAttrs = new CK_ATTRIBUTE[n + 1];
        System.arraycopy(attrs, 0, newAttrs, 0, n);
        newAttrs[n] = attr;
        return newAttrs;
    }

    private void storePkey(String alias, KeyStore.PrivateKeyEntry pke)
        throws PKCS11Exception, CertificateException, KeyStoreException  {

        PrivateKey key = pke.getPrivateKey();
        CK_ATTRIBUTE[] attrs = null;

        // If the key is a token object on this token, update it instead
        // of creating a duplicate key object.
        // Otherwise, treat a P11Key like any other key, if it is extractable.
        if (key instanceof P11Key) {
            P11Key p11Key = (P11Key)key;
            if (p11Key.tokenObject && (p11Key.token == this.token)) {
                updateP11Pkey(alias, null, p11Key);
                storeChain(alias, (X509Certificate[])pke.getCertificateChain());
                return;
            }
        }

        boolean useNDB = token.config.getNssNetscapeDbWorkaround();
        PublicKey publicKey = pke.getCertificate().getPublicKey();

        if (key instanceof RSAPrivateKey) {

            X509Certificate cert = (X509Certificate)pke.getCertificate();
            attrs = getRsaPrivKeyAttrs
                (alias, (RSAPrivateKey)key, cert.getSubjectX500Principal());

        } else if (key instanceof DSAPrivateKey) {

            DSAPrivateKey dsaKey = (DSAPrivateKey)key;

            CK_ATTRIBUTE[] idAttrs = getIdAttributes(key, publicKey, false, useNDB);
            if (idAttrs[0] == null) {
                idAttrs[0] = new CK_ATTRIBUTE(CKA_ID, alias);
            }

            attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_PKEY,
                ATTR_PRIVATE_TRUE,
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_DSA),
                idAttrs[0],
                new CK_ATTRIBUTE(CKA_PRIME, dsaKey.getParams().getP()),
                new CK_ATTRIBUTE(CKA_SUBPRIME, dsaKey.getParams().getQ()),
                new CK_ATTRIBUTE(CKA_BASE, dsaKey.getParams().getG()),
                new CK_ATTRIBUTE(CKA_VALUE, dsaKey.getX()),
            };
            if (idAttrs[1] != null) {
                attrs = addAttribute(attrs, idAttrs[1]);
            }

            attrs = token.getAttributes
                (TemplateManager.O_IMPORT, CKO_PRIVATE_KEY, CKK_DSA, attrs);

            if (debug != null) {
                debug.println("storePkey created DSA template");
            }

        } else if (key instanceof DHPrivateKey) {

            DHPrivateKey dhKey = (DHPrivateKey)key;

            CK_ATTRIBUTE[] idAttrs = getIdAttributes(key, publicKey, false, useNDB);
            if (idAttrs[0] == null) {
                idAttrs[0] = new CK_ATTRIBUTE(CKA_ID, alias);
            }

            attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_PKEY,
                ATTR_PRIVATE_TRUE,
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_DH),
                idAttrs[0],
                new CK_ATTRIBUTE(CKA_PRIME, dhKey.getParams().getP()),
                new CK_ATTRIBUTE(CKA_BASE, dhKey.getParams().getG()),
                new CK_ATTRIBUTE(CKA_VALUE, dhKey.getX()),
            };
            if (idAttrs[1] != null) {
                attrs = addAttribute(attrs, idAttrs[1]);
            }

            attrs = token.getAttributes
                (TemplateManager.O_IMPORT, CKO_PRIVATE_KEY, CKK_DH, attrs);

        } else if (key instanceof ECPrivateKey) {

            ECPrivateKey ecKey = (ECPrivateKey)key;

            CK_ATTRIBUTE[] idAttrs = getIdAttributes(key, publicKey, false, useNDB);
            if (idAttrs[0] == null) {
                idAttrs[0] = new CK_ATTRIBUTE(CKA_ID, alias);
            }

            byte[] encodedParams =
                ECUtil.encodeECParameterSpec(null, ecKey.getParams());
            attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_PKEY,
                ATTR_PRIVATE_TRUE,
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_EC),
                idAttrs[0],
                new CK_ATTRIBUTE(CKA_VALUE, ecKey.getS()),
                new CK_ATTRIBUTE(CKA_EC_PARAMS, encodedParams),
            };
            if (idAttrs[1] != null) {
                attrs = addAttribute(attrs, idAttrs[1]);
            }

            attrs = token.getAttributes
                (TemplateManager.O_IMPORT, CKO_PRIVATE_KEY, CKK_EC, attrs);

            if (debug != null) {
                debug.println("storePkey created EC template");
            }

        } else if (key instanceof P11Key) {
            // sensitive/non-extractable P11Key
            P11Key p11Key = (P11Key)key;
            if (p11Key.token != this.token) {
                throw new KeyStoreException
                    ("Cannot move sensitive keys across tokens");
            }
            CK_ATTRIBUTE netscapeDB = null;
            if (useNDB) {
                // Note that this currently fails due to an NSS bug.
                // They do not allow the CKA_NETSCAPE_DB attribute to be
                // specified during C_CopyObject() and fail with
                // CKR_ATTRIBUTE_READ_ONLY.
                // But if we did not specify it, they would fail with
                // CKA_TEMPLATE_INCOMPLETE, so leave this code in here.
                CK_ATTRIBUTE[] idAttrs = getIdAttributes(key, publicKey, false, true);
                netscapeDB = idAttrs[1];
            }
            // Update the key object.
            updateP11Pkey(alias, netscapeDB, p11Key);
            storeChain(alias, (X509Certificate[])pke.getCertificateChain());
            return;

        } else {
            throw new KeyStoreException("unsupported key type: " + key);
        }

        Session session = null;
        try {
            session = token.getOpSession();

            // create private key entry
            token.p11.C_CreateObject(session.id(), attrs);
            if (debug != null) {
                debug.println("storePkey created token key for [" +
                                alias +
                                "]");
            }
        } finally {
            token.releaseSession(session);
        }

        storeChain(alias, (X509Certificate[])pke.getCertificateChain());
    }

    private CK_ATTRIBUTE[] getRsaPrivKeyAttrs(String alias,
                                RSAPrivateKey key,
                                X500Principal subject) throws PKCS11Exception {

        // subject is currently ignored - could be used to set CKA_SUBJECT

        CK_ATTRIBUTE[] attrs = null;
        if (key instanceof RSAPrivateCrtKey) {

            if (debug != null) {
                debug.println("creating RSAPrivateCrtKey attrs");
            }

            RSAPrivateCrtKey rsaKey = (RSAPrivateCrtKey)key;

            attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_PKEY,
                ATTR_PRIVATE_TRUE,
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_RSA),
                new CK_ATTRIBUTE(CKA_ID, alias),
                new CK_ATTRIBUTE(CKA_MODULUS,
                                rsaKey.getModulus()),
                new CK_ATTRIBUTE(CKA_PRIVATE_EXPONENT,
                                rsaKey.getPrivateExponent()),
                new CK_ATTRIBUTE(CKA_PUBLIC_EXPONENT,
                                rsaKey.getPublicExponent()),
                new CK_ATTRIBUTE(CKA_PRIME_1,
                                rsaKey.getPrimeP()),
                new CK_ATTRIBUTE(CKA_PRIME_2,
                                rsaKey.getPrimeQ()),
                new CK_ATTRIBUTE(CKA_EXPONENT_1,
                                rsaKey.getPrimeExponentP()),
                new CK_ATTRIBUTE(CKA_EXPONENT_2,
                                rsaKey.getPrimeExponentQ()),
                new CK_ATTRIBUTE(CKA_COEFFICIENT,
                                rsaKey.getCrtCoefficient()) };
            attrs = token.getAttributes
                (TemplateManager.O_IMPORT, CKO_PRIVATE_KEY, CKK_RSA, attrs);

        } else {

            if (debug != null) {
                debug.println("creating RSAPrivateKey attrs");
            }

            RSAPrivateKey rsaKey = key;

            attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_PKEY,
                ATTR_PRIVATE_TRUE,
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_RSA),
                new CK_ATTRIBUTE(CKA_ID, alias),
                new CK_ATTRIBUTE(CKA_MODULUS,
                                rsaKey.getModulus()),
                new CK_ATTRIBUTE(CKA_PRIVATE_EXPONENT,
                                rsaKey.getPrivateExponent()) };
            attrs = token.getAttributes
                (TemplateManager.O_IMPORT, CKO_PRIVATE_KEY, CKK_RSA, attrs);
        }

        return attrs;
    }

    /**
     * Compute the CKA_ID and/or CKA_NETSCAPE_DB attributes that should be
     * used for this private key. It uses the same algorithm to calculate the
     * values as NSS. The public and private keys MUST match for the result to
     * be correct.
     *
     * It returns a 2 element array with CKA_ID at index 0 and CKA_NETSCAPE_DB
     * at index 1. The boolean flags determine what is to be calculated.
     * If false or if we could not calculate the value, that element is null.
     *
     * NOTE that we currently do not use the CKA_ID value calculated by this
     * method.
     */
    private CK_ATTRIBUTE[] getIdAttributes(PrivateKey privateKey,
            PublicKey publicKey, boolean id, boolean netscapeDb) {
        CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[2];
        if ((id || netscapeDb) == false) {
            return attrs;
        }
        String alg = privateKey.getAlgorithm();
        if (alg.equals("RSA") && (publicKey instanceof RSAPublicKey)) {
            if (id) {
                BigInteger n = ((RSAPublicKey)publicKey).getModulus();
                attrs[0] = new CK_ATTRIBUTE(CKA_ID, sha1(getMagnitude(n)));
            }
            // CKA_NETSCAPE_DB not needed for RSA public keys
        } else if (alg.equals("DSA") && (publicKey instanceof DSAPublicKey)) {
            BigInteger y = ((DSAPublicKey)publicKey).getY();
            if (id) {
                attrs[0] = new CK_ATTRIBUTE(CKA_ID, sha1(getMagnitude(y)));
            }
            if (netscapeDb) {
                attrs[1] = new CK_ATTRIBUTE(CKA_NETSCAPE_DB, y);
            }
        } else if (alg.equals("DH") && (publicKey instanceof DHPublicKey)) {
            BigInteger y = ((DHPublicKey)publicKey).getY();
            if (id) {
                attrs[0] = new CK_ATTRIBUTE(CKA_ID, sha1(getMagnitude(y)));
            }
            if (netscapeDb) {
                attrs[1] = new CK_ATTRIBUTE(CKA_NETSCAPE_DB, y);
            }
        } else if (alg.equals("EC") && (publicKey instanceof ECPublicKey)) {
            ECPublicKey ecPub = (ECPublicKey)publicKey;
            ECPoint point = ecPub.getW();
            ECParameterSpec params = ecPub.getParams();
            byte[] encodedPoint = ECUtil.encodePoint(point, params.getCurve());
            if (id) {
                attrs[0] = new CK_ATTRIBUTE(CKA_ID, sha1(encodedPoint));
            }
            if (netscapeDb) {
                attrs[1] = new CK_ATTRIBUTE(CKA_NETSCAPE_DB, encodedPoint);
            }
        } else {
            throw new RuntimeException("Unknown key algorithm " + alg);
        }
        return attrs;
    }

    /**
     * return true if cert destroyed
     */
    private boolean destroyCert(byte[] cka_id)
                throws PKCS11Exception, KeyStoreException {
        Session session = null;
        try {
            session = token.getOpSession();
            THandle h = getTokenObject(session, ATTR_CLASS_CERT, cka_id, null);
            if (h.type != ATTR_CLASS_CERT) {
                return false;
            }

            token.p11.C_DestroyObject(session.id(), h.handle);
            if (debug != null) {
                debug.println("destroyCert destroyed cert with CKA_ID [" +
                                                getID(cka_id) +
                                                "]");
            }
            return true;
        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * return true if chain destroyed
     */
    private boolean destroyChain(byte[] cka_id)
        throws PKCS11Exception, CertificateException, KeyStoreException {

        Session session = null;
        try {
            session = token.getOpSession();

            THandle h = getTokenObject(session, ATTR_CLASS_CERT, cka_id, null);
            if (h.type != ATTR_CLASS_CERT) {
                if (debug != null) {
                    debug.println("destroyChain could not find " +
                        "end entity cert with CKA_ID [0x" +
                        Functions.toHexString(cka_id) +
                        "]");
                }
                return false;
            }

            X509Certificate endCert = loadCert(session, h.handle);
            token.p11.C_DestroyObject(session.id(), h.handle);
            if (debug != null) {
                debug.println("destroyChain destroyed end entity cert " +
                        "with CKA_ID [" +
                        getID(cka_id) +
                        "]");
            }

            // build chain following issuer->subject links

            X509Certificate next = endCert;
            while (true) {

                if (next.getSubjectX500Principal().equals
                    (next.getIssuerX500Principal())) {
                    // self signed - done
                    break;
                }

                CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                        ATTR_TOKEN_TRUE,
                        ATTR_CLASS_CERT,
                        new CK_ATTRIBUTE(CKA_SUBJECT,
                                  next.getIssuerX500Principal().getEncoded()) };
                long[] ch = findObjects(session, attrs);

                if (ch == null || ch.length == 0) {
                    // done
                    break;
                } else {
                    // if more than one found, use first
                    if (debug != null && ch.length > 1) {
                        debug.println("destroyChain found " +
                                ch.length +
                                " certificate entries for subject [" +
                                next.getIssuerX500Principal() +
                                "] in token - using first entry");
                    }

                    next = loadCert(session, ch[0]);

                    // only delete if not part of any other chain

                    attrs = new CK_ATTRIBUTE[] {
                        ATTR_TOKEN_TRUE,
                        ATTR_CLASS_CERT,
                        new CK_ATTRIBUTE(CKA_ISSUER,
                                next.getSubjectX500Principal().getEncoded()) };
                    long[] issuers = findObjects(session, attrs);

                    boolean destroyIt = false;
                    if (issuers == null || issuers.length == 0) {
                        // no other certs with this issuer -
                        // destroy it
                        destroyIt = true;
                    } else if (issuers.length == 1) {
                        X509Certificate iCert = loadCert(session, issuers[0]);
                        if (next.equals(iCert)) {
                            // only cert with issuer is itself (self-signed) -
                            // destroy it
                            destroyIt = true;
                        }
                    }

                    if (destroyIt) {
                        token.p11.C_DestroyObject(session.id(), ch[0]);
                        if (debug != null) {
                            debug.println
                                ("destroyChain destroyed cert in chain " +
                                "with subject [" +
                                next.getSubjectX500Principal() + "]");
                        }
                    } else {
                        if (debug != null) {
                            debug.println("destroyChain did not destroy " +
                                "shared cert in chain with subject [" +
                                next.getSubjectX500Principal() + "]");
                        }
                    }
                }
            }

            return true;

        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * return true if secret key destroyed
     */
    private boolean destroySkey(String alias)
                throws PKCS11Exception, KeyStoreException {
        Session session = null;
        try {
            session = token.getOpSession();

            THandle h = getTokenObject(session, ATTR_CLASS_SKEY, null, alias);
            if (h.type != ATTR_CLASS_SKEY) {
                if (debug != null) {
                    debug.println("destroySkey did not find secret key " +
                        "with CKA_LABEL [" +
                        alias +
                        "]");
                }
                return false;
            }
            token.p11.C_DestroyObject(session.id(), h.handle);
            return true;
        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * return true if private key destroyed
     */
    private boolean destroyPkey(byte[] cka_id)
                throws PKCS11Exception, KeyStoreException {
        Session session = null;
        try {
            session = token.getOpSession();

            THandle h = getTokenObject(session, ATTR_CLASS_PKEY, cka_id, null);
            if (h.type != ATTR_CLASS_PKEY) {
                if (debug != null) {
                    debug.println
                        ("destroyPkey did not find private key with CKA_ID [" +
                        getID(cka_id) +
                        "]");
                }
                return false;
            }
            token.p11.C_DestroyObject(session.id(), h.handle);
            return true;
        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * build [alias + issuer + serialNumber] string from a cert
     */
    private String getID(String alias, X509Certificate cert) {
        X500Principal issuer = cert.getIssuerX500Principal();
        BigInteger serialNum = cert.getSerialNumber();

        return alias +
                ALIAS_SEP +
                issuer.getName(X500Principal.CANONICAL) +
                ALIAS_SEP +
                serialNum.toString();
    }

    /**
     * build CKA_ID string from bytes
     */
    private static String getID(byte[] bytes) {
        boolean printable = true;
        for (int i = 0; i < bytes.length; i++) {
            if (!DerValue.isPrintableStringChar((char)bytes[i])) {
                printable = false;
                break;
            }
        }

        if (!printable) {
            return "0x" + Functions.toHexString(bytes);
        } else {
            return new String(bytes, UTF_8);
        }
    }

    /**
     * find an object on the token
     *
     * @param type either ATTR_CLASS_CERT, ATTR_CLASS_PKEY, or ATTR_CLASS_SKEY
     * @param cka_id the CKA_ID if type is ATTR_CLASS_CERT or ATTR_CLASS_PKEY
     * @param cka_label the CKA_LABEL if type is ATTR_CLASS_SKEY
     */
    private THandle getTokenObject(Session session,
                                CK_ATTRIBUTE type,
                                byte[] cka_id,
                                String cka_label)
                throws PKCS11Exception, KeyStoreException {

        CK_ATTRIBUTE[] attrs;
        if (type == ATTR_CLASS_SKEY) {
            attrs = new CK_ATTRIBUTE[] {
                        ATTR_SKEY_TOKEN_TRUE,
                        new CK_ATTRIBUTE(CKA_LABEL, cka_label),
                        type };
        } else {
            attrs = new CK_ATTRIBUTE[] {
                        ATTR_TOKEN_TRUE,
                        new CK_ATTRIBUTE(CKA_ID, cka_id),
                        type };
        }
        long[] h = findObjects(session, attrs);
        if (h.length == 0) {
            if (debug != null) {
                if (type == ATTR_CLASS_SKEY) {
                    debug.println("getTokenObject did not find secret key " +
                                "with CKA_LABEL [" +
                                cka_label +
                                "]");
                } else if (type == ATTR_CLASS_CERT) {
                    debug.println
                        ("getTokenObject did not find cert with CKA_ID [" +
                        getID(cka_id) +
                        "]");
                } else {
                    debug.println("getTokenObject did not find private key " +
                        "with CKA_ID [" +
                        getID(cka_id) +
                        "]");
                }
            }
        } else if (h.length == 1) {

            // found object handle - return it
            return new THandle(h[0], type);

        } else {

            // found multiple object handles -
            // see if token ignored CKA_LABEL during search (e.g. NSS)

            if (type == ATTR_CLASS_SKEY) {

                ArrayList<THandle> list = new ArrayList<THandle>(h.length);
                for (int i = 0; i < h.length; i++) {

                    CK_ATTRIBUTE[] label = new CK_ATTRIBUTE[]
                                        { new CK_ATTRIBUTE(CKA_LABEL) };
                    token.p11.C_GetAttributeValue(session.id(), h[i], label);
                    if (label[0].pValue != null &&
                        cka_label.equals(new String(label[0].getCharArray()))) {
                        list.add(new THandle(h[i], ATTR_CLASS_SKEY));
                    }
                }
                if (list.size() == 1) {
                    // yes, there was only one CKA_LABEL that matched
                    return list.get(0);
                } else {
                    throw new KeyStoreException("invalid KeyStore state: " +
                        "found " +
                        list.size() +
                        " secret keys sharing CKA_LABEL [" +
                        cka_label +
                        "]");
                }
            } else if (type == ATTR_CLASS_CERT) {
                throw new KeyStoreException("invalid KeyStore state: " +
                        "found " +
                        h.length +
                        " certificates sharing CKA_ID " +
                        getID(cka_id));
            } else {
                throw new KeyStoreException("invalid KeyStore state: " +
                        "found " +
                        h.length +
                        " private keys sharing CKA_ID " +
                        getID(cka_id));
            }
        }
        return new THandle(NO_HANDLE, null);
    }

    /**
     * Create a mapping of all key pairs, trusted certs, and secret keys
     * on the token into logical KeyStore entries unambiguously
     * accessible via an alias.
     *
     * If the token is removed, the map may contain stale values.
     * KeyStore.load should be called to re-create the map.
     *
     * Assume all private keys and matching certs share a unique CKA_ID.
     *
     * Assume all secret keys have a unique CKA_LABEL.
     *
     * @return true if multiple certs found sharing the same CKA_LABEL
     *          (if so, write capabilities are disabled)
     */
    private boolean mapLabels() throws
                PKCS11Exception, CertificateException, KeyStoreException {

        CK_ATTRIBUTE[] trustedAttr = new CK_ATTRIBUTE[] {
                                new CK_ATTRIBUTE(CKA_TRUSTED) };

        Session session = null;
        try {
            session = token.getOpSession();

            // get all private key CKA_IDs

            ArrayList<byte[]> pkeyIDs = new ArrayList<byte[]>();
            CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_PKEY,
            };
            long[] handles = findObjects(session, attrs);

            for (long handle : handles) {
                attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_ID) };
                token.p11.C_GetAttributeValue(session.id(), handle, attrs);

                if (attrs[0].pValue != null) {
                    pkeyIDs.add(attrs[0].getByteArray());
                }
            }

            // Get all certificates
            //
            // If cert does not have a CKA_LABEL nor CKA_ID, it is ignored.
            //
            // Get the CKA_LABEL for each cert
            // (if the cert does not have a CKA_LABEL, use the CKA_ID).
            //
            // Map each cert to the its CKA_LABEL
            // (multiple certs may be mapped to a single CKA_LABEL)

            HashMap<String, HashSet<AliasInfo>> certMap =
                                new HashMap<String, HashSet<AliasInfo>>();

            attrs = new CK_ATTRIBUTE[] {
                ATTR_TOKEN_TRUE,
                ATTR_CLASS_CERT,
            };
            handles = findObjects(session, attrs);

            for (long handle : handles) {
                attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_LABEL) };

                String cka_label = null;
                byte[] cka_id = null;
                try {
                    token.p11.C_GetAttributeValue(session.id(), handle, attrs);
                    if (attrs[0].pValue != null) {
                        // there is a CKA_LABEL
                        cka_label = new String(attrs[0].getCharArray());
                    }
                } catch (PKCS11Exception pe) {
                    if (pe.getErrorCode() != CKR_ATTRIBUTE_TYPE_INVALID) {
                        throw pe;
                    }

                    // GetAttributeValue for CKA_LABEL not supported
                    //
                    // XXX SCA1000
                }

                // get CKA_ID

                attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_ID) };
                token.p11.C_GetAttributeValue(session.id(), handle, attrs);
                if (attrs[0].pValue == null) {
                    if (cka_label == null) {
                        // no cka_label nor cka_id - ignore
                        continue;
                    }
                } else {
                    if (cka_label == null) {
                        // use CKA_ID as CKA_LABEL
                        cka_label = getID(attrs[0].getByteArray());
                    }
                    cka_id = attrs[0].getByteArray();
                }

                X509Certificate cert = loadCert(session, handle);

                // get CKA_TRUSTED

                boolean cka_trusted = false;

                if (useSecmodTrust) {
                    cka_trusted = Secmod.getInstance().isTrusted(cert, nssTrustType);
                } else {
                    if (CKA_TRUSTED_SUPPORTED) {
                        try {
                            token.p11.C_GetAttributeValue
                                    (session.id(), handle, trustedAttr);
                            cka_trusted = trustedAttr[0].getBoolean();
                        } catch (PKCS11Exception pe) {
                            if (pe.getErrorCode() == CKR_ATTRIBUTE_TYPE_INVALID) {
                                // XXX  NSS, ibutton, sca1000
                                CKA_TRUSTED_SUPPORTED = false;
                                if (debug != null) {
                                    debug.println
                                            ("CKA_TRUSTED attribute not supported");
                                }
                            }
                        }
                    }
                }

                HashSet<AliasInfo> infoSet = certMap.get(cka_label);
                if (infoSet == null) {
                    infoSet = new HashSet<AliasInfo>(2);
                    certMap.put(cka_label, infoSet);
                }

                // initially create private key entry AliasInfo entries -
                // these entries will get resolved into their true
                // entry types later

                infoSet.add(new AliasInfo
                                (cka_label,
                                cka_id,
                                cka_trusted,
                                cert));
            }

            // create list secret key CKA_LABELS -
            // if there are duplicates (either between secret keys,
            // or between a secret key and another object),
            // throw an exception
            HashMap<String, AliasInfo> sKeyMap =
                    new HashMap<String, AliasInfo>();

            attrs = new CK_ATTRIBUTE[] {
                ATTR_SKEY_TOKEN_TRUE,
                ATTR_CLASS_SKEY,
            };
            handles = findObjects(session, attrs);

            for (long handle : handles) {
                attrs = new CK_ATTRIBUTE[] { new CK_ATTRIBUTE(CKA_LABEL) };
                token.p11.C_GetAttributeValue(session.id(), handle, attrs);
                if (attrs[0].pValue != null) {

                    // there is a CKA_LABEL
                    String cka_label = new String(attrs[0].getCharArray());
                    if (sKeyMap.get(cka_label) == null) {
                        sKeyMap.put(cka_label, new AliasInfo(cka_label));
                    } else {
                        throw new KeyStoreException("invalid KeyStore state: " +
                                "found multiple secret keys sharing same " +
                                "CKA_LABEL [" +
                                cka_label +
                                "]");
                    }
                }
            }

            // update global aliasMap with alias mappings
            ArrayList<AliasInfo> matchedCerts =
                                mapPrivateKeys(pkeyIDs, certMap);
            boolean sharedLabel = mapCerts(matchedCerts, certMap);
            mapSecretKeys(sKeyMap);

            return sharedLabel;

        } finally {
            token.releaseSession(session);
        }
    }

    /**
     * for each private key CKA_ID, find corresponding cert with same CKA_ID.
     * if found cert, see if cert CKA_LABEL is unique.
     *     if CKA_LABEL unique, map private key/cert alias to that CKA_LABEL.
     *     if CKA_LABEL not unique, map private key/cert alias to:
     *                   CKA_LABEL + ALIAS_SEP + ISSUER + ALIAS_SEP + SERIAL
     * if cert not found, ignore private key
     * (don't support private key entries without a cert chain yet)
     *
     * @return a list of AliasInfo entries that represents all matches
     */
    private ArrayList<AliasInfo> mapPrivateKeys(ArrayList<byte[]> pkeyIDs,
                        HashMap<String, HashSet<AliasInfo>> certMap)
                throws PKCS11Exception, CertificateException {

        // reset global alias map
        aliasMap = new HashMap<String, AliasInfo>();

        // list of matched certs that we will return
        ArrayList<AliasInfo> matchedCerts = new ArrayList<AliasInfo>();

        for (byte[] pkeyID : pkeyIDs) {

            // try to find a matching CKA_ID in a certificate

            boolean foundMatch = false;
            Set<String> certLabels = certMap.keySet();
            for (String certLabel : certLabels) {

                // get cert CKA_IDs (if present) for each cert

                HashSet<AliasInfo> infoSet = certMap.get(certLabel);
                for (AliasInfo aliasInfo : infoSet) {
                    if (Arrays.equals(pkeyID, aliasInfo.id)) {

                        // found private key with matching cert

                        if (infoSet.size() == 1) {
                            // unique CKA_LABEL - use certLabel as alias
                            aliasInfo.matched = true;
                            aliasMap.put(certLabel, aliasInfo);
                        } else {
                            // create new alias
                            aliasInfo.matched = true;
                            aliasMap.put(getID(certLabel, aliasInfo.cert),
                                        aliasInfo);
                        }
                        matchedCerts.add(aliasInfo);
                        foundMatch = true;
                        break;
                    }
                }
                if (foundMatch) {
                    break;
                }
            }

            if (!foundMatch) {
                if (debug != null) {
                    debug.println
                        ("did not find match for private key with CKA_ID [" +
                        getID(pkeyID) +
                        "] (ignoring entry)");
                }
            }
        }

        return matchedCerts;
    }

    /**
     * for each cert not matched with a private key but is CKA_TRUSTED:
     *     if CKA_LABEL unique, map cert to CKA_LABEL.
     *     if CKA_LABEL not unique, map cert to [label+issuer+serialNum]
     *
     * if CKA_TRUSTED not supported, treat all certs not part of a chain
     * as trusted
     *
     * @return true if multiple certs found sharing the same CKA_LABEL
     */
    private boolean mapCerts(ArrayList<AliasInfo> matchedCerts,
                        HashMap<String, HashSet<AliasInfo>> certMap)
                throws PKCS11Exception, CertificateException {

        // load all cert chains
        for (AliasInfo aliasInfo : matchedCerts) {
            Session session = null;
            try {
                session = token.getOpSession();
                aliasInfo.chain = loadChain(session, aliasInfo.cert);
            } finally {
                token.releaseSession(session);
            }
        }

        // find all certs in certMap not part of a cert chain
        // - these are trusted

        boolean sharedLabel = false;

        Set<String> certLabels = certMap.keySet();
        for (String certLabel : certLabels) {
            HashSet<AliasInfo> infoSet = certMap.get(certLabel);
            for (AliasInfo aliasInfo : infoSet) {

                if (aliasInfo.matched == true) {
                    // already found a private key match for this cert -
                    // just continue
                    aliasInfo.trusted = false;
                    continue;
                }

                // cert in this aliasInfo is not matched yet
                //
                // if CKA_TRUSTED_SUPPORTED == true,
                // then check if cert is trusted

                if (CKA_TRUSTED_SUPPORTED) {
                    if (aliasInfo.trusted) {
                        // trusted certificate
                        if (mapTrustedCert
                                (certLabel, aliasInfo, infoSet) == true) {
                            sharedLabel = true;
                        }
                    }
                    continue;
                }

                // CKA_TRUSTED_SUPPORTED == false
                //
                // XXX treat all certs not part of a chain as trusted
                // XXX
                // XXX Unsupported
                //
                // boolean partOfChain = false;
                // for (AliasInfo matchedInfo : matchedCerts) {
                //     for (int i = 0; i < matchedInfo.chain.length; i++) {
                //      if (matchedInfo.chain[i].equals(aliasInfo.cert)) {
                //          partOfChain = true;
                //          break;
                //      }
                //     }
                //     if (partOfChain) {
                //      break;
                //     }
                // }
                //
                // if (!partOfChain) {
                //     if (mapTrustedCert(certLabel,aliasInfo,infoSet) == true){
                //      sharedLabel = true;
                //     }
                // } else {
                //    if (debug != null) {
                //      debug.println("ignoring unmatched/untrusted cert " +
                //          "that is part of cert chain - cert subject is [" +
                //          aliasInfo.cert.getSubjectX500Principal().getName
                //                              (X500Principal.CANONICAL) +
                //          "]");
                //     }
                // }
            }
        }

        return sharedLabel;
    }

    private boolean mapTrustedCert(String certLabel,
                                AliasInfo aliasInfo,
                                HashSet<AliasInfo> infoSet) {

        boolean sharedLabel = false;

        aliasInfo.type = ATTR_CLASS_CERT;
        aliasInfo.trusted = true;
        if (infoSet.size() == 1) {
            // unique CKA_LABEL - use certLabel as alias
            aliasMap.put(certLabel, aliasInfo);
        } else {
            // create new alias
            sharedLabel = true;
            aliasMap.put(getID(certLabel, aliasInfo.cert), aliasInfo);
        }

        return sharedLabel;
    }

    /**
     * If the secret key shares a CKA_LABEL with another entry,
     * throw an exception
     */
    private void mapSecretKeys(HashMap<String, AliasInfo> sKeyMap)
                throws KeyStoreException {
        for (String label : sKeyMap.keySet()) {
            if (aliasMap.containsKey(label)) {
                throw new KeyStoreException("invalid KeyStore state: " +
                        "found secret key sharing CKA_LABEL [" +
                        label +
                        "] with another token object");
            }
        }
        aliasMap.putAll(sKeyMap);
    }

    private void dumpTokenMap() {
        Set<String> aliases = aliasMap.keySet();
        System.out.println("Token Alias Map:");
        if (aliases.isEmpty()) {
            System.out.println("  [empty]");
        } else {
            for (String s : aliases) {
                System.out.println("  " + s + aliasMap.get(s));
            }
        }
    }

    private void checkWrite() throws KeyStoreException {
        if (writeDisabled) {
            throw new KeyStoreException
                ("This PKCS11KeyStore does not support write capabilities");
        }
    }

    private static final long[] LONG0 = new long[0];

    private static long[] findObjects(Session session, CK_ATTRIBUTE[] attrs)
            throws PKCS11Exception {
        Token token = session.token;
        long[] handles = LONG0;
        token.p11.C_FindObjectsInit(session.id(), attrs);
        while (true) {
            long[] h = token.p11.C_FindObjects(session.id(), FINDOBJECTS_MAX);
            if (h.length == 0) {
                break;
            }
            handles = P11Util.concat(handles, h);
        }
        token.p11.C_FindObjectsFinal(session.id());
        return handles;
    }

}
