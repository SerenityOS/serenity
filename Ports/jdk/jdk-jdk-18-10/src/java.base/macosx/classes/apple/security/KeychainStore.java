/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package apple.security;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import javax.security.auth.x500.*;

import sun.security.pkcs.*;
import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.util.*;
import sun.security.x509.*;

/**
 * This class provides the keystore implementation referred to as "KeychainStore".
 * It uses the current user's keychain as its backing storage, and does NOT support
 * a file-based implementation.
 */

public final class KeychainStore extends KeyStoreSpi {

    // Private keys and their supporting certificate chains
    // If a key came from the keychain it has a SecKeyRef and one or more
    // SecCertificateRef.  When we delete the key we have to delete all of the corresponding
    // native objects.
    static class KeyEntry {
        Date date; // the creation date of this entry
        byte[] protectedPrivKey;
        char[] password;
        long keyRef;  // SecKeyRef for this key
        Certificate chain[];
        long chainRefs[];  // SecCertificateRefs for this key's chain.
    };

    // Trusted certificates
    static class TrustedCertEntry {
        Date date; // the creation date of this entry

        Certificate cert;
        long certRef;  // SecCertificateRef for this key
    };

    /**
     * Entries that have been deleted.  When something calls engineStore we'll
     * remove them from the keychain.
     */
    private Hashtable<String, Object> deletedEntries = new Hashtable<>();

    /**
     * Entries that have been added.  When something calls engineStore we'll
     * add them to the keychain.
     */
    private Hashtable<String, Object> addedEntries = new Hashtable<>();

    /**
     * Private keys and certificates are stored in a hashtable.
     * Hash entries are keyed by alias names.
     */
    private Hashtable<String, Object> entries = new Hashtable<>();

    /**
     * Algorithm identifiers and corresponding OIDs for the contents of the
     * PKCS12 bag we get from the Keychain.
     */
    private static ObjectIdentifier PKCS8ShroudedKeyBag_OID =
            ObjectIdentifier.of(KnownOIDs.PKCS8ShroudedKeyBag);
    private static ObjectIdentifier pbeWithSHAAnd3KeyTripleDESCBC_OID =
            ObjectIdentifier.of(KnownOIDs.PBEWithSHA1AndDESede);

    /**
     * Constnats used in PBE decryption.
     */
    private static final int iterationCount = 1024;
    private static final int SALT_LEN = 20;

    private static final Debug debug = Debug.getInstance("keystore");

    static {
        jdk.internal.loader.BootLoader.loadLibrary("osxsecurity");
    }

    private static void permissionCheck() {
        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();

        if (sec != null) {
            sec.checkPermission(new RuntimePermission("useKeychainStore"));
        }
    }


    /**
     * Verify the Apple provider in the constructor.
     *
     * @exception SecurityException if fails to verify
     * its own integrity
     */
    public KeychainStore() { }

    /**
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     * @param password the password for recovering the key. This password is
     *        used internally as the key is exported in a PKCS12 format.
     *
     * @return the requested key, or null if the given alias does not exist
     * or does not identify a <i>key entry</i>.
     *
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     * key cannot be found
     * @exception UnrecoverableKeyException if the key cannot be recovered
     * (e.g., the given password is wrong).
     */
    public Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
        permissionCheck();

        // An empty password is rejected by MacOS API, no private key data
        // is exported. If no password is passed (as is the case when
        // this implementation is used as browser keystore in various
        // deployment scenarios like Webstart, JFX and applets), create
        // a dummy password so MacOS API is happy.
        if (password == null || password.length == 0) {
            // Must not be a char array with only a 0, as this is an empty
            // string.
            if (random == null) {
                random = new SecureRandom();
            }
            password = Long.toString(random.nextLong()).toCharArray();
        }

        Object entry = entries.get(alias.toLowerCase());

        if (entry == null || !(entry instanceof KeyEntry)) {
            return null;
        }

        // This call gives us a PKCS12 bag, with the key inside it.
        byte[] exportedKeyInfo = _getEncodedKeyData(((KeyEntry)entry).keyRef, password);
        if (exportedKeyInfo == null) {
            return null;
        }

        PrivateKey returnValue = null;

        try {
            byte[] pkcs8KeyData = fetchPrivateKeyFromBag(exportedKeyInfo);
            byte[] encryptedKey;
            AlgorithmParameters algParams;
            ObjectIdentifier algOid;
            try {
                // get the encrypted private key
                EncryptedPrivateKeyInfo encrInfo = new EncryptedPrivateKeyInfo(pkcs8KeyData);
                encryptedKey = encrInfo.getEncryptedData();

                // parse Algorithm parameters
                DerValue val = new DerValue(encrInfo.getAlgorithm().encode());
                DerInputStream in = val.toDerInputStream();
                algOid = in.getOID();
                algParams = parseAlgParameters(in);

            } catch (IOException ioe) {
                UnrecoverableKeyException uke =
                new UnrecoverableKeyException("Private key not stored as "
                                              + "PKCS#8 EncryptedPrivateKeyInfo: " + ioe);
                uke.initCause(ioe);
                throw uke;
            }

            // Use JCE to decrypt the data using the supplied password.
            SecretKey skey = getPBEKey(password);
            Cipher cipher = Cipher.getInstance(algOid.toString());
            cipher.init(Cipher.DECRYPT_MODE, skey, algParams);
            byte[] decryptedPrivateKey = cipher.doFinal(encryptedKey);
            PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(decryptedPrivateKey);

             // Parse the key algorithm and then use a JCA key factory to create the private key.
            DerValue val = new DerValue(decryptedPrivateKey);
            DerInputStream in = val.toDerInputStream();

            // Ignore this -- version should be 0.
            int i = in.getInteger();

            // Get the Algorithm ID next
            DerValue[] value = in.getSequence(2);
            if (value.length < 1 || value.length > 2) {
                throw new IOException("Invalid length for AlgorithmIdentifier");
            }
            AlgorithmId algId = new AlgorithmId(value[0].getOID());
            String algName = algId.getName();

            // Get a key factory for this algorithm.  It's likely to be 'RSA'.
            KeyFactory kfac = KeyFactory.getInstance(algName);
            returnValue = kfac.generatePrivate(kspec);
        } catch (Exception e) {
            UnrecoverableKeyException uke =
            new UnrecoverableKeyException("Get Key failed: " +
                                          e.getMessage());
            uke.initCause(e);
            throw uke;
        }

        return returnValue;
    }

    private native byte[] _getEncodedKeyData(long secKeyRef, char[] password);

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last), or null if the given alias
     * does not exist or does not contain a certificate chain (i.e., the given
     * alias identifies either a <i>trusted certificate entry</i> or a
     * <i>key entry</i> without a certificate chain).
     */
    public Certificate[] engineGetCertificateChain(String alias) {
        permissionCheck();

        Object entry = entries.get(alias.toLowerCase());

        if (entry != null && entry instanceof KeyEntry) {
            if (((KeyEntry)entry).chain == null) {
                return null;
            } else {
                return ((KeyEntry)entry).chain.clone();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the certificate associated with the given alias.
     *
     * <p>If the given alias name identifies a
     * <i>trusted certificate entry</i>, the certificate associated with that
     * entry is returned. If the given alias name identifies a
     * <i>key entry</i>, the first element of the certificate chain of that
     * entry is returned, or null if that entry does not have a certificate
     * chain.
     *
     * @param alias the alias name
     *
     * @return the certificate, or null if the given alias does not exist or
     * does not contain a certificate.
     */
    public Certificate engineGetCertificate(String alias) {
        permissionCheck();

        Object entry = entries.get(alias.toLowerCase());

        if (entry != null) {
            if (entry instanceof TrustedCertEntry) {
                return ((TrustedCertEntry)entry).cert;
            } else {
                KeyEntry ke = (KeyEntry)entry;
                if (ke.chain == null || ke.chain.length == 0) {
                    return null;
                }
                return ke.chain[0];
            }
        } else {
            return null;
        }
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
        permissionCheck();

        Object entry = entries.get(alias.toLowerCase());

        if (entry != null) {
            if (entry instanceof TrustedCertEntry) {
                return new Date(((TrustedCertEntry)entry).date.getTime());
            } else {
                return new Date(((KeyEntry)entry).date.getTime());
            }
        } else {
            return null;
        }
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
    public void engineSetKeyEntry(String alias, Key key, char[] password,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        permissionCheck();

        synchronized(entries) {
            try {
                KeyEntry entry = new KeyEntry();
                entry.date = new Date();

                if (key instanceof PrivateKey) {
                    if ((key.getFormat().equals("PKCS#8")) ||
                        (key.getFormat().equals("PKCS8"))) {
                        entry.protectedPrivKey = encryptPrivateKey(key.getEncoded(), password);
                        entry.password = password.clone();
                    } else {
                        throw new KeyStoreException("Private key is not encoded as PKCS#8");
                    }
                } else {
                    throw new KeyStoreException("Key is not a PrivateKey");
                }

                // clone the chain
                if (chain != null) {
                    if ((chain.length > 1) && !validateChain(chain)) {
                        throw new KeyStoreException("Certificate chain does not validate");
                    }

                    entry.chain = chain.clone();
                    entry.chainRefs = new long[entry.chain.length];
                }

                String lowerAlias = alias.toLowerCase();
                if (entries.get(lowerAlias) != null) {
                    deletedEntries.put(lowerAlias, entries.get(lowerAlias));
                }

                entries.put(lowerAlias, entry);
                addedEntries.put(lowerAlias, entry);
            } catch (Exception nsae) {
                KeyStoreException ke = new KeyStoreException("Key protection algorithm not found: " + nsae);
                ke.initCause(nsae);
                throw ke;
            }
        }
    }

    /**
     * Assigns the given key (that has already been protected) to the given
     * alias.
     *
     * <p>If the protected key is of type
     * <code>java.security.PrivateKey</code>, it must be accompanied by a
     * certificate chain certifying the corresponding public key. If the
     * underlying keystore implementation is of type <code>jks</code>,
     * <code>key</code> must be encoded as an
     * <code>EncryptedPrivateKeyInfo</code> as defined in the PKCS #8 standard.
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
    public void engineSetKeyEntry(String alias, byte[] key,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        permissionCheck();

        synchronized(entries) {
            // key must be encoded as EncryptedPrivateKeyInfo as defined in
            // PKCS#8
            KeyEntry entry = new KeyEntry();
            try {
                EncryptedPrivateKeyInfo privateKey = new EncryptedPrivateKeyInfo(key);
                entry.protectedPrivKey = privateKey.getEncoded();
            } catch (IOException ioe) {
                throw new KeyStoreException("key is not encoded as "
                                            + "EncryptedPrivateKeyInfo");
            }

            entry.date = new Date();

            if ((chain != null) &&
                (chain.length != 0)) {
                entry.chain = chain.clone();
                entry.chainRefs = new long[entry.chain.length];
            }

            String lowerAlias = alias.toLowerCase();
            if (entries.get(lowerAlias) != null) {
                deletedEntries.put(lowerAlias, entries.get(alias));
            }
            entries.put(lowerAlias, entry);
            addedEntries.put(lowerAlias, entry);
        }
    }

    /**
     * Assigns the given certificate to the given alias.
     *
     * <p>If the given alias already exists in this keystore and identifies a
     * <i>trusted certificate entry</i>, the certificate associated with it is
     * overridden by the given certificate.
     *
     * @param alias the alias name
     * @param cert the certificate
     *
     * @exception KeyStoreException if the given alias already exists and does
     * not identify a <i>trusted certificate entry</i>, or this operation
     * fails for some other reason.
     */
    public void engineSetCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException
    {
        permissionCheck();

        synchronized(entries) {

            Object entry = entries.get(alias.toLowerCase());
            if ((entry != null) && (entry instanceof KeyEntry)) {
                throw new KeyStoreException
                ("Cannot overwrite key entry with certificate");
            }

            // This will be slow, but necessary.  Enumerate the values and then see if the cert matches the one in the trusted cert entry.
            // Security framework doesn't support the same certificate twice in a keychain.
            Collection<Object> allValues = entries.values();

            for (Object value : allValues) {
                if (value instanceof TrustedCertEntry) {
                    TrustedCertEntry tce = (TrustedCertEntry)value;
                    if (tce.cert.equals(cert)) {
                        throw new KeyStoreException("Keychain does not support mulitple copies of same certificate.");
                    }
                }
            }

            TrustedCertEntry trustedCertEntry = new TrustedCertEntry();
            trustedCertEntry.cert = cert;
            trustedCertEntry.date = new Date();
            String lowerAlias = alias.toLowerCase();
            if (entries.get(lowerAlias) != null) {
                deletedEntries.put(lowerAlias, entries.get(lowerAlias));
            }
            entries.put(lowerAlias, trustedCertEntry);
            addedEntries.put(lowerAlias, trustedCertEntry);
        }
    }

    /**
     * Deletes the entry identified by the given alias from this keystore.
     *
     * @param alias the alias name
     *
     * @exception KeyStoreException if the entry cannot be removed.
     */
    public void engineDeleteEntry(String alias)
        throws KeyStoreException
    {
        permissionCheck();

        synchronized(entries) {
            Object entry = entries.remove(alias.toLowerCase());
            deletedEntries.put(alias.toLowerCase(), entry);
        }
    }

    /**
     * Lists all the alias names of this keystore.
     *
     * @return enumeration of the alias names
     */
    public Enumeration<String> engineAliases() {
        permissionCheck();
        return entries.keys();
    }

    /**
     * Checks if the given alias exists in this keystore.
     *
     * @param alias the alias name
     *
     * @return true if the alias exists, false otherwise
     */
    public boolean engineContainsAlias(String alias) {
        permissionCheck();
        return entries.containsKey(alias.toLowerCase());
    }

    /**
     * Retrieves the number of entries in this keystore.
     *
     * @return the number of entries in this keystore
     */
    public int engineSize() {
        permissionCheck();
        return entries.size();
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>key entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>key entry</i>, false otherwise.
     */
    public boolean engineIsKeyEntry(String alias) {
        permissionCheck();
        Object entry = entries.get(alias.toLowerCase());
        if ((entry != null) && (entry instanceof KeyEntry)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, false otherwise.
     */
    public boolean engineIsCertificateEntry(String alias) {
        permissionCheck();
        Object entry = entries.get(alias.toLowerCase());
        if ((entry != null) && (entry instanceof TrustedCertEntry)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the (alias) name of the first keystore entry whose certificate
     * matches the given certificate.
     *
     * <p>This method attempts to match the given certificate with each
     * keystore entry. If the entry being considered
     * is a <i>trusted certificate entry</i>, the given certificate is
     * compared to that entry's certificate. If the entry being considered is
     * a <i>key entry</i>, the given certificate is compared to the first
     * element of that entry's certificate chain (if a chain exists).
     *
     * @param cert the certificate to match with.
     *
     * @return the (alias) name of the first entry with matching certificate,
     * or null if no such entry exists in this keystore.
     */
    public String engineGetCertificateAlias(Certificate cert) {
        permissionCheck();
        Certificate certElem;

        for (Enumeration<String> e = entries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = entries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                certElem = ((TrustedCertEntry)entry).cert;
            } else {
                KeyEntry ke = (KeyEntry)entry;
                if (ke.chain == null || ke.chain.length == 0) {
                    continue;
                }
                certElem = ke.chain[0];
            }
            if (certElem.equals(cert)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Stores this keystore to the given output stream, and protects its
     * integrity with the given password.
     *
     * @param stream Ignored. the output stream to which this keystore is written.
     * @param password the password to generate the keystore integrity check
     *
     * @exception IOException if there was an I/O problem with data
     * @exception NoSuchAlgorithmException if the appropriate data integrity
     * algorithm could not be found
     * @exception CertificateException if any of the certificates included in
     * the keystore data could not be stored
     */
    public void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        permissionCheck();

        // Delete items that do have a keychain item ref.
        for (Enumeration<String> e = deletedEntries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = deletedEntries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                if (((TrustedCertEntry)entry).certRef != 0) {
                    _removeItemFromKeychain(((TrustedCertEntry)entry).certRef);
                    _releaseKeychainItemRef(((TrustedCertEntry)entry).certRef);
                }
            } else {
                Certificate certElem;
                KeyEntry keyEntry = (KeyEntry)entry;

                if (keyEntry.chain != null) {
                    for (int i = 0; i < keyEntry.chain.length; i++) {
                        if (keyEntry.chainRefs[i] != 0) {
                            _removeItemFromKeychain(keyEntry.chainRefs[i]);
                            _releaseKeychainItemRef(keyEntry.chainRefs[i]);
                        }
                    }

                    if (keyEntry.keyRef != 0) {
                        _removeItemFromKeychain(keyEntry.keyRef);
                        _releaseKeychainItemRef(keyEntry.keyRef);
                    }
                }
            }
        }

        // Add all of the certs or keys in the added entries.
        // No need to check for 0 refs, as they are in the added list.
        for (Enumeration<String> e = addedEntries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = addedEntries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                TrustedCertEntry tce = (TrustedCertEntry)entry;
                Certificate certElem;
                certElem = tce.cert;
                tce.certRef = addCertificateToKeychain(alias, certElem);
            } else {
                KeyEntry keyEntry = (KeyEntry)entry;

                if (keyEntry.chain != null) {
                    for (int i = 0; i < keyEntry.chain.length; i++) {
                        keyEntry.chainRefs[i] = addCertificateToKeychain(alias, keyEntry.chain[i]);
                    }

                    keyEntry.keyRef = _addItemToKeychain(alias, false, keyEntry.protectedPrivKey, keyEntry.password);
                }
            }
        }

        // Clear the added and deletedEntries hashtables here, now that we're done with the updates.
        // For the deleted entries, we freed up the native references above.
        deletedEntries.clear();
        addedEntries.clear();
    }

    private long addCertificateToKeychain(String alias, Certificate cert) {
        byte[] certblob = null;
        long returnValue = 0;

        try {
            certblob = cert.getEncoded();
            returnValue = _addItemToKeychain(alias, true, certblob, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    private native long _addItemToKeychain(String alias, boolean isCertificate, byte[] datablob, char[] password);
    private native int _removeItemFromKeychain(long certRef);
    private native void _releaseKeychainItemRef(long keychainItemRef);

    /**
     * Loads the keystore from the Keychain.
     *
     * @param stream Ignored - here for API compatibility.
     * @param password Ignored - if user needs to unlock keychain Security
     * framework will post any dialogs.
     *
     * @exception IOException if there is an I/O or format problem with the
     * keystore data
     * @exception NoSuchAlgorithmException if the algorithm used to check
     * the integrity of the keystore cannot be found
     * @exception CertificateException if any of the certificates in the
     * keystore could not be loaded
     */
    public void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        permissionCheck();

        // Release any stray keychain references before clearing out the entries.
        synchronized(entries) {
            for (Enumeration<String> e = entries.keys(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                Object entry = entries.get(alias);
                if (entry instanceof TrustedCertEntry) {
                    if (((TrustedCertEntry)entry).certRef != 0) {
                        _releaseKeychainItemRef(((TrustedCertEntry)entry).certRef);
                    }
                } else {
                    KeyEntry keyEntry = (KeyEntry)entry;

                    if (keyEntry.chain != null) {
                        for (int i = 0; i < keyEntry.chain.length; i++) {
                            if (keyEntry.chainRefs[i] != 0) {
                                _releaseKeychainItemRef(keyEntry.chainRefs[i]);
                            }
                        }

                        if (keyEntry.keyRef != 0) {
                            _releaseKeychainItemRef(keyEntry.keyRef);
                        }
                    }
                }
            }

            entries.clear();
            _scanKeychain();
            if (debug != null) {
                debug.println("KeychainStore load entry count: " +
                        entries.size());
            }
        }
    }

    private native void _scanKeychain();

    /**
     * Callback method from _scanKeychain.  If a trusted certificate is found, this method will be called.
     */
    private void createTrustedCertEntry(String alias, long keychainItemRef, long creationDate, byte[] derStream) {
        TrustedCertEntry tce = new TrustedCertEntry();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream input = new ByteArrayInputStream(derStream);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(input);
            input.close();
            tce.cert = cert;
            tce.certRef = keychainItemRef;

            // Make a creation date.
            if (creationDate != 0)
                tce.date = new Date(creationDate);
            else
                tce.date = new Date();

            int uniqueVal = 1;
            String originalAlias = alias;

            while (entries.containsKey(alias.toLowerCase())) {
                alias = originalAlias + " " + uniqueVal;
                uniqueVal++;
            }

            entries.put(alias.toLowerCase(), tce);
        } catch (Exception e) {
            // The certificate will be skipped.
            System.err.println("KeychainStore Ignored Exception: " + e);
        }
    }

    /**
     * Callback method from _scanKeychain.  If an identity is found, this method will be called to create Java certificate
     * and private key objects from the keychain data.
     */
    private void createKeyEntry(String alias, long creationDate, long secKeyRef,
                                long[] secCertificateRefs, byte[][] rawCertData) {
        KeyEntry ke = new KeyEntry();

        // First, store off the private key information.  This is the easy part.
        ke.protectedPrivKey = null;
        ke.keyRef = secKeyRef;

        // Make a creation date.
        if (creationDate != 0)
            ke.date = new Date(creationDate);
        else
            ke.date = new Date();

        // Next, create X.509 Certificate objects from the raw data.  This is complicated
        // because a certificate's public key may be too long for Java's default encryption strength.
        List<CertKeychainItemPair> createdCerts = new ArrayList<>();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (int i = 0; i < rawCertData.length; i++) {
                try {
                    InputStream input = new ByteArrayInputStream(rawCertData[i]);
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(input);
                    input.close();

                    // We successfully created the certificate, so track it and its corresponding SecCertificateRef.
                    createdCerts.add(new CertKeychainItemPair(secCertificateRefs[i], cert));
                } catch (CertificateException e) {
                    // The certificate will be skipped.
                    System.err.println("KeychainStore Ignored Exception: " + e);
                }
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();  // How would this happen?
        }

        // We have our certificates in the List, so now extract them into an array of
        // Certificates and SecCertificateRefs.
        CertKeychainItemPair[] objArray = createdCerts.toArray(new CertKeychainItemPair[0]);
        Certificate[] certArray = new Certificate[objArray.length];
        long[] certRefArray = new long[objArray.length];

        for (int i = 0; i < objArray.length; i++) {
            CertKeychainItemPair addedItem = objArray[i];
            certArray[i] = addedItem.mCert;
            certRefArray[i] = addedItem.mCertificateRef;
        }

        ke.chain = certArray;
        ke.chainRefs = certRefArray;

        // If we don't have already have an item with this item's alias
        // create a new one for it.
        int uniqueVal = 1;
        String originalAlias = alias;

        while (entries.containsKey(alias.toLowerCase())) {
            alias = originalAlias + " " + uniqueVal;
            uniqueVal++;
        }

        entries.put(alias.toLowerCase(), ke);
    }

    private static class CertKeychainItemPair {
        long mCertificateRef;
        Certificate mCert;

        CertKeychainItemPair(long inCertRef, Certificate cert) {
            mCertificateRef = inCertRef;
            mCert = cert;
        }
    }

    /*
     * Validate Certificate Chain
     */
    private boolean validateChain(Certificate[] certChain)
    {
        for (int i = 0; i < certChain.length-1; i++) {
            X500Principal issuerDN =
            ((X509Certificate)certChain[i]).getIssuerX500Principal();
            X500Principal subjectDN =
                ((X509Certificate)certChain[i+1]).getSubjectX500Principal();
            if (!(issuerDN.equals(subjectDN)))
                return false;
        }
        return true;
    }

    private byte[] fetchPrivateKeyFromBag(byte[] privateKeyInfo) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        byte[] returnValue = null;
        DerValue val = new DerValue(new ByteArrayInputStream(privateKeyInfo));
        DerInputStream s = val.toDerInputStream();
        int version = s.getInteger();

        if (version != 3) {
            throw new IOException("PKCS12 keystore not in version 3 format");
        }

        /*
         * Read the authSafe.
         */
        byte[] authSafeData;
        ContentInfo authSafe = new ContentInfo(s);
        ObjectIdentifier contentType = authSafe.getContentType();

        if (contentType.equals(ContentInfo.DATA_OID)) {
            authSafeData = authSafe.getData();
        } else /* signed data */ {
            throw new IOException("public key protected PKCS12 not supported");
        }

        DerInputStream as = new DerInputStream(authSafeData);
        DerValue[] safeContentsArray = as.getSequence(2);
        int count = safeContentsArray.length;

        /*
         * Spin over the ContentInfos.
         */
        for (int i = 0; i < count; i++) {
            byte[] safeContentsData;
            ContentInfo safeContents;
            DerInputStream sci;
            byte[] eAlgId = null;

            sci = new DerInputStream(safeContentsArray[i].toByteArray());
            safeContents = new ContentInfo(sci);
            contentType = safeContents.getContentType();
            safeContentsData = null;

            if (contentType.equals(ContentInfo.DATA_OID)) {
                safeContentsData = safeContents.getData();
            } else if (contentType.equals(ContentInfo.ENCRYPTED_DATA_OID)) {
                // The password was used to export the private key from the keychain.
                // The Keychain won't export the key with encrypted data, so we don't need
                // to worry about it.
                continue;
            } else {
                throw new IOException("public key protected PKCS12" +
                                      " not supported");
            }
            DerInputStream sc = new DerInputStream(safeContentsData);
            returnValue = extractKeyData(sc);
        }

        return returnValue;
    }

    private byte[] extractKeyData(DerInputStream stream)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        byte[] returnValue = null;
        DerValue[] safeBags = stream.getSequence(2);
        int count = safeBags.length;

        /*
         * Spin over the SafeBags.
         */
        for (int i = 0; i < count; i++) {
            ObjectIdentifier bagId;
            DerInputStream sbi;
            DerValue bagValue;
            Object bagItem = null;

            sbi = safeBags[i].toDerInputStream();
            bagId = sbi.getOID();
            bagValue = sbi.getDerValue();
            if (!bagValue.isContextSpecific((byte)0)) {
                throw new IOException("unsupported PKCS12 bag value type "
                                      + bagValue.tag);
            }
            bagValue = bagValue.data.getDerValue();
            if (bagId.equals(PKCS8ShroudedKeyBag_OID)) {
                // got what we were looking for.  Return it.
                returnValue = bagValue.toByteArray();
            } else {
                // log error message for "unsupported PKCS12 bag type"
                System.out.println("Unsupported bag type '" + bagId + "'");
            }
        }

        return returnValue;
    }

    /*
     * Generate PBE Algorithm Parameters
     */
    private AlgorithmParameters getAlgorithmParameters(String algorithm)
        throws IOException
    {
        AlgorithmParameters algParams = null;

        // create PBE parameters from salt and iteration count
        PBEParameterSpec paramSpec =
            new PBEParameterSpec(getSalt(), iterationCount);
        try {
            algParams = AlgorithmParameters.getInstance(algorithm);
            algParams.init(paramSpec);
        } catch (Exception e) {
            IOException ioe =
            new IOException("getAlgorithmParameters failed: " +
                            e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        return algParams;
    }

    // the source of randomness
    private SecureRandom random;

    /*
     * Generate random salt
     */
    private byte[] getSalt()
    {
        // Generate a random salt.
        byte[] salt = new byte[SALT_LEN];
        if (random == null) {
            random = new SecureRandom();
        }
        random.nextBytes(salt);
        return salt;
    }

    /*
     * parse Algorithm Parameters
     */
    private AlgorithmParameters parseAlgParameters(DerInputStream in)
        throws IOException
    {
        AlgorithmParameters algParams = null;
        try {
            DerValue params;
            if (in.available() == 0) {
                params = null;
            } else {
                params = in.getDerValue();
                if (params.tag == DerValue.tag_Null) {
                    params = null;
                }
            }
            if (params != null) {
                algParams = AlgorithmParameters.getInstance("PBE");
                algParams.init(params.toByteArray());
            }
        } catch (Exception e) {
            IOException ioe =
            new IOException("parseAlgParameters failed: " +
                            e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        return algParams;
    }

    /*
     * Generate PBE key
     */
    private SecretKey getPBEKey(char[] password) throws IOException
    {
        SecretKey skey = null;

        try {
            PBEKeySpec keySpec = new PBEKeySpec(password);
            SecretKeyFactory skFac = SecretKeyFactory.getInstance("PBE");
            skey = skFac.generateSecret(keySpec);
        } catch (Exception e) {
            IOException ioe = new IOException("getSecretKey failed: " +
                                              e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        return skey;
    }

    /*
     * Encrypt private key using Password-based encryption (PBE)
     * as defined in PKCS#5.
     *
     * NOTE: Currently pbeWithSHAAnd3-KeyTripleDES-CBC algorithmID is
     *       used to derive the key and IV.
     *
     * @return encrypted private key encoded as EncryptedPrivateKeyInfo
     */
    private byte[] encryptPrivateKey(byte[] data, char[] password)
        throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException
    {
        byte[] key = null;

        try {
            // create AlgorithmParameters
            AlgorithmParameters algParams =
            getAlgorithmParameters("PBEWithSHA1AndDESede");

            // Use JCE
            SecretKey skey = getPBEKey(password);
            Cipher cipher = Cipher.getInstance("PBEWithSHA1AndDESede");
            cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);
            byte[] encryptedKey = cipher.doFinal(data);

            // wrap encrypted private key in EncryptedPrivateKeyInfo
            // as defined in PKCS#8
            AlgorithmId algid =
                new AlgorithmId(pbeWithSHAAnd3KeyTripleDESCBC_OID, algParams);
            EncryptedPrivateKeyInfo encrInfo =
                new EncryptedPrivateKeyInfo(algid, encryptedKey);
            key = encrInfo.getEncoded();
        } catch (Exception e) {
            UnrecoverableKeyException uke =
            new UnrecoverableKeyException("Encrypt Private Key failed: "
                                          + e.getMessage());
            uke.initCause(e);
            throw uke;
        }

        return key;
    }


}

