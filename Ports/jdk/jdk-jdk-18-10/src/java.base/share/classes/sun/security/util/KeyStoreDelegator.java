/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.util.*;

import sun.security.util.Debug;

/**
 * This class delegates to a primary or secondary keystore implementation.
 *
 * @since 9
 */

public class KeyStoreDelegator extends KeyStoreSpi {

    private static final String KEYSTORE_TYPE_COMPAT = "keystore.type.compat";
    private static final Debug debug = Debug.getInstance("keystore");

    private String primaryType;   // the primary keystore's type
    private String secondaryType; // the secondary keystore's type
    private Class<? extends KeyStoreSpi> primaryKeyStore;
                                  // the primary keystore's class
    private Class<? extends KeyStoreSpi> secondaryKeyStore;
                                  // the secondary keystore's class
    private String type; // the delegate's type
    private KeyStoreSpi keystore; // the delegate
    private boolean compatModeEnabled = true;

    public KeyStoreDelegator(
        String primaryType,
        Class<? extends KeyStoreSpi> primaryKeyStore,
        String secondaryType,
        Class<? extends KeyStoreSpi> secondaryKeyStore) {

        // Check whether compatibility mode has been disabled
        @SuppressWarnings("removal")
        var prop = AccessController.doPrivileged((PrivilegedAction<String>) () ->
                        Security.getProperty(KEYSTORE_TYPE_COMPAT));
        compatModeEnabled = "true".equalsIgnoreCase(prop);

        if (compatModeEnabled) {
            this.primaryType = primaryType;
            this.secondaryType = secondaryType;
            this.primaryKeyStore = primaryKeyStore;
            this.secondaryKeyStore = secondaryKeyStore;
        } else {
            this.primaryType = primaryType;
            this.secondaryType = null;
            this.primaryKeyStore = primaryKeyStore;
            this.secondaryKeyStore = null;

            if (debug != null) {
                debug.println("WARNING: compatibility mode disabled for " +
                    primaryType + " and " + secondaryType + " keystore types");
            }
        }
    }

    @Override
    public Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException {
        return keystore.engineGetKey(alias, password);
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
        return keystore.engineGetCertificateChain(alias);
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
        return keystore.engineGetCertificate(alias);
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        return keystore.engineGetCreationDate(alias);
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password,
        Certificate[] chain) throws KeyStoreException {
        keystore.engineSetKeyEntry(alias, key, password, chain);
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
        throws KeyStoreException {
        keystore.engineSetKeyEntry(alias, key, chain);
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException {
        keystore.engineSetCertificateEntry(alias, cert);
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        keystore.engineDeleteEntry(alias);
    }

    @Override
    public Enumeration<String> engineAliases() {
        return keystore.engineAliases();
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        return keystore.engineContainsAlias(alias);
    }

    @Override
    public int engineSize() {
        return keystore.engineSize();
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        return keystore.engineIsKeyEntry(alias);
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        return keystore.engineIsCertificateEntry(alias);
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        return keystore.engineGetCertificateAlias(cert);
    }

    @Override
    public KeyStore.Entry engineGetEntry(String alias,
        KeyStore.ProtectionParameter protParam)
            throws KeyStoreException, NoSuchAlgorithmException,
                UnrecoverableEntryException {
        return keystore.engineGetEntry(alias, protParam);
    }

    @Override
    public void engineSetEntry(String alias, KeyStore.Entry entry,
        KeyStore.ProtectionParameter protParam)
            throws KeyStoreException {
        keystore.engineSetEntry(alias, entry, protParam);
    }

    @Override
    public boolean engineEntryInstanceOf(String alias,
        Class<? extends KeyStore.Entry> entryClass) {
        return keystore.engineEntryInstanceOf(alias, entryClass);
    }

    @Override
    public void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException {

        if (debug != null) {
            debug.println("Storing keystore in " + type + " format");
        }
        keystore.engineStore(stream, password);
    }

    @Override
    public void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException {

        // A new keystore is always created in the primary keystore format
        if (stream == null) {
            try {
                @SuppressWarnings("deprecation")
                KeyStoreSpi tmp = primaryKeyStore.newInstance();
                keystore = tmp;
            } catch (InstantiationException | IllegalAccessException e) {
                // can safely ignore
            }
            type = primaryType;

            if (debug != null) {
                debug.println("Creating a new keystore in " + type + " format");
            }
            keystore.engineLoad(stream, password);

        } else {
            // First try the primary keystore then try the secondary keystore
            InputStream bufferedStream = new BufferedInputStream(stream);
            bufferedStream.mark(Integer.MAX_VALUE);

            try {
                @SuppressWarnings("deprecation")
                KeyStoreSpi tmp = primaryKeyStore.newInstance();
                tmp.engineLoad(bufferedStream, password);
                keystore = tmp;
                type = primaryType;

            } catch (Exception e) {

                // incorrect password
                if (e instanceof IOException &&
                    e.getCause() instanceof UnrecoverableKeyException) {
                    throw (IOException)e;
                }

                try {
                    // Ignore secondary keystore when no compatibility mode
                    if (!compatModeEnabled) {
                        throw e;
                    }

                    @SuppressWarnings("deprecation")
                    KeyStoreSpi tmp = secondaryKeyStore.newInstance();
                    bufferedStream.reset();
                    tmp.engineLoad(bufferedStream, password);
                    keystore = tmp;
                    type = secondaryType;

                    if (debug != null) {
                        debug.println("WARNING: switching from " +
                          primaryType + " to " + secondaryType +
                          " keystore file format has altered the " +
                          "keystore security level");
                    }

                } catch (InstantiationException |
                    IllegalAccessException e2) {
                    // can safely ignore

                } catch (IOException |
                    NoSuchAlgorithmException |
                    CertificateException e3) {

                    // incorrect password
                    if (e3 instanceof IOException &&
                        e3.getCause() instanceof UnrecoverableKeyException) {
                        throw (IOException)e3;
                    }
                    // rethrow the outer exception
                    if (e instanceof IOException) {
                        throw (IOException)e;
                    } else if (e instanceof CertificateException) {
                        throw (CertificateException)e;
                    } else if (e instanceof NoSuchAlgorithmException) {
                        throw (NoSuchAlgorithmException)e;
                    } else if (e instanceof RuntimeException){
                        throw (RuntimeException)e;
                    }
                }
            }

            if (debug != null) {
                debug.println("Loaded a keystore in " + type + " format");
            }
        }
    }

    /**
     * Probe the first few bytes of the keystore data stream for a valid
     * keystore encoding. Only the primary keystore implementation is probed.
     */
    @Override
    public boolean engineProbe(InputStream stream) throws IOException {

        boolean result = false;

        try {
            @SuppressWarnings("deprecation")
            KeyStoreSpi tmp = primaryKeyStore.newInstance();
            keystore = tmp;
            type = primaryType;
            result = keystore.engineProbe(stream);

        } catch (Exception e) {
            throw new IOException(e);

        } finally {
            // reset
            if (result == false) {
                type = null;
                keystore = null;
            }
        }

        return result;
    }
}
