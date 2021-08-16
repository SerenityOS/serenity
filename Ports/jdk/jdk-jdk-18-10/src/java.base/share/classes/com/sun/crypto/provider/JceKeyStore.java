/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import sun.security.util.Debug;
import sun.security.util.IOUtils;

import java.io.*;
import java.util.*;
import java.security.AccessController;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.KeyStoreSpi;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import javax.crypto.SealedObject;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class provides the keystore implementation referred to as "jceks".
 * This implementation strongly protects the keystore private keys using
 * triple-DES, where the triple-DES encryption/decryption key is derived from
 * the user's password.
 * The encrypted private keys are stored in the keystore in a standard format,
 * namely the <code>EncryptedPrivateKeyInfo</code> format defined in PKCS #8.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.KeyStoreSpi
 */

public final class JceKeyStore extends KeyStoreSpi {

    private static final Debug debug = Debug.getInstance("keystore");
    private static final int JCEKS_MAGIC = 0xcececece;
    private static final int JKS_MAGIC = 0xfeedfeed;
    private static final int VERSION_1 = 0x01;
    private static final int VERSION_2 = 0x02;

    // Private key and supporting certificate chain
    private static final class PrivateKeyEntry {
        Date date; // the creation date of this entry
        byte[] protectedKey;
        Certificate[] chain;
    };

    // Secret key
    private static final class SecretKeyEntry {
        Date date; // the creation date of this entry
        SealedObject sealedKey;

        // Maximum possible length of sealedKey. Used to detect malicious
        // input data. This field is set to the file length of the keystore
        // at loading. It is useless when creating a new SecretKeyEntry
        // to be store in a keystore.
        int maxLength;
    }

    // Trusted certificate
    private static final class TrustedCertEntry {
        Date date; // the creation date of this entry
        Certificate cert;
    };

    /**
     * Private keys and certificates are stored in a hashtable.
     * Hash entries are keyed by alias names.
     */
    private Hashtable<String, Object> entries = new Hashtable<String, Object>();

    /**
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     * @param password the password for recovering the key
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
        Key key = null;

        Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));

        if (!((entry instanceof PrivateKeyEntry) ||
              (entry instanceof SecretKeyEntry))) {
            return null;
        }

        KeyProtector keyProtector = new KeyProtector(password);

        if (entry instanceof PrivateKeyEntry) {
            byte[] encrBytes = ((PrivateKeyEntry)entry).protectedKey;
            EncryptedPrivateKeyInfo encrInfo;
            try {
                encrInfo = new EncryptedPrivateKeyInfo(encrBytes);
            } catch (IOException ioe) {
                throw new UnrecoverableKeyException("Private key not stored "
                                                    + "as PKCS #8 " +
                                                    "EncryptedPrivateKeyInfo");
            }
            key = keyProtector.recover(encrInfo);
        } else {
            SecretKeyEntry ske = ((SecretKeyEntry)entry);
            key = keyProtector.unseal(ske.sealedKey, ske.maxLength);
        }

        return key;
    }

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
    public Certificate[] engineGetCertificateChain(String alias)
    {
        Certificate[] chain = null;

        Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));

        if ((entry instanceof PrivateKeyEntry)
            && (((PrivateKeyEntry)entry).chain != null)) {
            chain = ((PrivateKeyEntry)entry).chain.clone();
        }

        return chain;
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
        Certificate cert = null;

        Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));

        if (entry != null) {
            if (entry instanceof TrustedCertEntry) {
                cert = ((TrustedCertEntry)entry).cert;
            } else if ((entry instanceof PrivateKeyEntry) &&
                       (((PrivateKeyEntry)entry).chain != null)) {
                cert = ((PrivateKeyEntry)entry).chain[0];
            }
        }

        return cert;
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
        Date date = null;

        Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));

        if (entry != null) {
            // We have to create a new instance of java.util.Date because
            // dates are not immutable
            if (entry instanceof TrustedCertEntry) {
                date = new Date(((TrustedCertEntry)entry).date.getTime());
            } else if (entry instanceof PrivateKeyEntry) {
                date = new Date(((PrivateKeyEntry)entry).date.getTime());
            } else {
                date = new Date(((SecretKeyEntry)entry).date.getTime());
            }
        }

        return date;
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
        synchronized(entries) {
            try {
                KeyProtector keyProtector = new KeyProtector(password);

                if (key instanceof PrivateKey) {
                    PrivateKeyEntry entry = new PrivateKeyEntry();
                    entry.date = new Date();

                    // protect the private key
                    entry.protectedKey = keyProtector.protect((PrivateKey)key);

                    // clone the chain
                    if ((chain != null) &&
                        (chain.length !=0)) {
                        entry.chain = chain.clone();
                    } else {
                        entry.chain = null;
                    }

                    // store the entry
                    entries.put(alias.toLowerCase(Locale.ENGLISH), entry);

                } else {
                    SecretKeyEntry entry = new SecretKeyEntry();
                    entry.date = new Date();

                    // seal and store the key
                    entry.sealedKey = keyProtector.seal(key);
                    entry.maxLength = Integer.MAX_VALUE;
                    entries.put(alias.toLowerCase(Locale.ENGLISH), entry);
                }

            } catch (Exception e) {
                throw new KeyStoreException(e.getMessage(), e);
            }
        }
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
    public void engineSetKeyEntry(String alias, byte[] key,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        synchronized(entries) {
            // We assume it's a private key, because there is no standard
            // (ASN.1) encoding format for wrapped secret keys
            PrivateKeyEntry entry = new PrivateKeyEntry();
            entry.date = new Date();

            entry.protectedKey = key.clone();
            if ((chain != null) &&
                (chain.length != 0)) {
                entry.chain = chain.clone();
            } else {
                entry.chain = null;
            }

            entries.put(alias.toLowerCase(Locale.ENGLISH), entry);
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
        synchronized(entries) {

            Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
            if (entry != null) {
                if (entry instanceof PrivateKeyEntry) {
                    throw new KeyStoreException("Cannot overwrite own "
                                                + "certificate");
                } else if (entry instanceof SecretKeyEntry) {
                    throw new KeyStoreException("Cannot overwrite secret key");
                }
            }

            TrustedCertEntry trustedCertEntry = new TrustedCertEntry();
            trustedCertEntry.cert = cert;
            trustedCertEntry.date = new Date();
            entries.put(alias.toLowerCase(Locale.ENGLISH), trustedCertEntry);
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
        synchronized(entries) {
            entries.remove(alias.toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * Lists all the alias names of this keystore.
     *
     * @return enumeration of the alias names
     */
    public Enumeration<String> engineAliases() {
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
        return entries.containsKey(alias.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Retrieves the number of entries in this keystore.
     *
     * @return the number of entries in this keystore
     */
    public int engineSize() {
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
        boolean isKey = false;

        Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if ((entry instanceof PrivateKeyEntry)
            || (entry instanceof SecretKeyEntry)) {
            isKey = true;
        }

        return isKey;
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, false otherwise.
     */
    public boolean engineIsCertificateEntry(String alias) {
        boolean isCert = false;
        Object entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry instanceof TrustedCertEntry) {
            isCert = true;
        }
        return isCert;
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
        Certificate certElem;

        Enumeration<String> e = entries.keys();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            Object entry = entries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                certElem = ((TrustedCertEntry)entry).cert;
            } else if ((entry instanceof PrivateKeyEntry) &&
                       (((PrivateKeyEntry)entry).chain != null)) {
                certElem = ((PrivateKeyEntry)entry).chain[0];
            } else {
                continue;
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
     * @param stream the output stream to which this keystore is written.
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
        synchronized(entries) {
            /*
             * KEYSTORE FORMAT:
             *
             * Magic number (big-endian integer),
             * Version of this file format (big-endian integer),
             *
             * Count (big-endian integer),
             * followed by "count" instances of either:
             *
             *     {
             *      tag=1 (big-endian integer)
             *      alias (UTF string)
             *      timestamp
             *      encrypted private-key info according to PKCS #8
             *          (integer length followed by encoding)
             *      cert chain (integer count followed by certs;
             *          for each cert: type UTF string, followed by integer
             *              length, followed by encoding)
             *     }
             *
             * or:
             *
             *     {
             *      tag=2 (big-endian integer)
             *      alias (UTF string)
             *      timestamp
             *      cert (type UTF string, followed by integer length,
             *          followed by encoding)
             *     }
             *
             * or:
             *
             *     {
             *      tag=3 (big-endian integer)
             *      alias (UTF string)
             *      timestamp
             *      sealed secret key (in serialized form)
             *     }
             *
             * ended by a keyed SHA1 hash (bytes only) of
             *     { password + extra data + preceding body }
             */

            // password is mandatory when storing
            if (password == null) {
                throw new IllegalArgumentException("password can't be null");
            }

            byte[] encoded; // the certificate encoding

            MessageDigest md = getPreKeyedHash(password);
            DataOutputStream dos
                = new DataOutputStream(new DigestOutputStream(stream, md));
            // NOTE: don't pass dos to oos at this point or it'll corrupt
            // the keystore!!!
            ObjectOutputStream oos = null;
            try {
                dos.writeInt(JCEKS_MAGIC);
                dos.writeInt(VERSION_2); // always write the latest version

                dos.writeInt(entries.size());

                Enumeration<String> e = entries.keys();
                while (e.hasMoreElements()) {

                    String alias = e.nextElement();
                    Object entry = entries.get(alias);

                    if (entry instanceof PrivateKeyEntry) {

                        PrivateKeyEntry pentry = (PrivateKeyEntry)entry;

                        // write PrivateKeyEntry tag
                        dos.writeInt(1);

                        // write the alias
                        dos.writeUTF(alias);

                        // write the (entry creation) date
                        dos.writeLong(pentry.date.getTime());

                        // write the protected private key
                        dos.writeInt(pentry.protectedKey.length);
                        dos.write(pentry.protectedKey);

                        // write the certificate chain
                        int chainLen;
                        if (pentry.chain == null) {
                            chainLen = 0;
                        } else {
                            chainLen = pentry.chain.length;
                        }
                        dos.writeInt(chainLen);
                        for (int i = 0; i < chainLen; i++) {
                            encoded = pentry.chain[i].getEncoded();
                            dos.writeUTF(pentry.chain[i].getType());
                            dos.writeInt(encoded.length);
                            dos.write(encoded);
                        }

                    } else if (entry instanceof TrustedCertEntry) {

                        // write TrustedCertEntry tag
                        dos.writeInt(2);

                        // write the alias
                        dos.writeUTF(alias);

                        // write the (entry creation) date
                        dos.writeLong(((TrustedCertEntry)entry).date.getTime());

                        // write the trusted certificate
                        encoded = ((TrustedCertEntry)entry).cert.getEncoded();
                        dos.writeUTF(((TrustedCertEntry)entry).cert.getType());
                        dos.writeInt(encoded.length);
                        dos.write(encoded);

                    } else {

                        // write SecretKeyEntry tag
                        dos.writeInt(3);

                        // write the alias
                        dos.writeUTF(alias);

                        // write the (entry creation) date
                        dos.writeLong(((SecretKeyEntry)entry).date.getTime());

                        // write the sealed key
                        oos = new ObjectOutputStream(dos);
                        oos.writeObject(((SecretKeyEntry)entry).sealedKey);
                        // NOTE: don't close oos here since we are still
                        // using dos!!!
                    }
                }

                /*
                 * Write the keyed hash which is used to detect tampering with
                 * the keystore (such as deleting or modifying key or
                 * certificate entries).
                 */
                byte digest[] = md.digest();

                dos.write(digest);
                dos.flush();
            } finally {
                if (oos != null) {
                    oos.close();
                } else {
                    dos.close();
                }
            }
        }
    }

    /**
     * Loads the keystore from the given input stream.
     *
     * <p>If a password is given, it is used to check the integrity of the
     * keystore data. Otherwise, the integrity of the keystore is not checked.
     *
     * @param stream the input stream from which the keystore is loaded
     * @param password the (optional) password used to check the integrity of
     * the keystore.
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
        synchronized(entries) {
            DataInputStream dis;
            MessageDigest md = null;
            CertificateFactory cf = null;
            Hashtable<String, CertificateFactory> cfs = null;
            ByteArrayInputStream bais = null;
            byte[] encoded = null;
            int trustedKeyCount = 0, privateKeyCount = 0, secretKeyCount = 0;

            if (stream == null)
                return;

            byte[] allData = stream.readAllBytes();
            int fullLength = allData.length;

            stream = new ByteArrayInputStream(allData);
            if (password != null) {
                md = getPreKeyedHash(password);
                dis = new DataInputStream(new DigestInputStream(stream, md));
            } else {
                dis = new DataInputStream(stream);
            }
            // NOTE: don't pass dis to ois at this point or it'll fail to load
            // the keystore!!!
            ObjectInputStream ois = null;

            try {
                // Body format: see store method

                int xMagic = dis.readInt();
                int xVersion = dis.readInt();

                // Accept the following keystore implementations:
                // - JCEKS (this implementation), versions 1 and 2
                // - JKS (Sun's keystore implementation in JDK 1.2),
                //   versions 1 and 2
                if (((xMagic != JCEKS_MAGIC) && (xMagic != JKS_MAGIC)) ||
                    ((xVersion != VERSION_1) && (xVersion != VERSION_2))) {
                    throw new IOException("Invalid keystore format");
                }

                if (xVersion == VERSION_1) {
                    cf = CertificateFactory.getInstance("X509");
                } else {
                    // version 2
                    cfs = new Hashtable<>(3);
                }

                entries.clear();
                int count = dis.readInt();

                for (int i = 0; i < count; i++) {
                    int tag;
                    String alias;

                    tag = dis.readInt();

                    if (tag == 1) { // private-key entry
                        privateKeyCount++;
                        PrivateKeyEntry entry = new PrivateKeyEntry();

                        // read the alias
                        alias = dis.readUTF();

                        // read the (entry creation) date
                        entry.date = new Date(dis.readLong());

                        // read the private key
                        entry.protectedKey = IOUtils.readExactlyNBytes(dis, dis.readInt());

                        // read the certificate chain
                        int numOfCerts = dis.readInt();
                        List<Certificate> tmpCerts = new ArrayList<>();
                        for (int j = 0; j < numOfCerts; j++) {
                            if (xVersion == 2) {
                                // read the certificate type, and instantiate a
                                // certificate factory of that type (reuse
                                // existing factory if possible)
                                String certType = dis.readUTF();
                                if (cfs.containsKey(certType)) {
                                    // reuse certificate factory
                                    cf = cfs.get(certType);
                                } else {
                                    // create new certificate factory
                                    cf = CertificateFactory.getInstance(
                                        certType);
                                    // store the certificate factory so we can
                                    // reuse it later
                                    cfs.put(certType, cf);
                                }
                            }
                            // instantiate the certificate
                            encoded = IOUtils.readExactlyNBytes(dis, dis.readInt());
                            bais = new ByteArrayInputStream(encoded);
                            tmpCerts.add(cf.generateCertificate(bais));
                        }
                        entry.chain = tmpCerts.toArray(
                                new Certificate[numOfCerts]);

                        // Add the entry to the list
                        entries.put(alias, entry);

                    } else if (tag == 2) { // trusted certificate entry
                        trustedKeyCount++;
                        TrustedCertEntry entry = new TrustedCertEntry();

                        // read the alias
                        alias = dis.readUTF();

                        // read the (entry creation) date
                        entry.date = new Date(dis.readLong());

                        // read the trusted certificate
                        if (xVersion == 2) {
                            // read the certificate type, and instantiate a
                            // certificate factory of that type (reuse
                            // existing factory if possible)
                            String certType = dis.readUTF();
                            if (cfs.containsKey(certType)) {
                                // reuse certificate factory
                                cf = cfs.get(certType);
                            } else {
                                // create new certificate factory
                                cf = CertificateFactory.getInstance(certType);
                                // store the certificate factory so we can
                                // reuse it later
                                cfs.put(certType, cf);
                            }
                        }
                        encoded = IOUtils.readExactlyNBytes(dis, dis.readInt());
                        bais = new ByteArrayInputStream(encoded);
                        entry.cert = cf.generateCertificate(bais);

                        // Add the entry to the list
                        entries.put(alias, entry);

                    } else if (tag == 3) { // secret-key entry
                        secretKeyCount++;
                        SecretKeyEntry entry = new SecretKeyEntry();

                        // read the alias
                        alias = dis.readUTF();

                        // read the (entry creation) date
                        entry.date = new Date(dis.readLong());

                        // read the sealed key
                        try {
                            ois = new ObjectInputStream(dis);
                            final ObjectInputStream ois2 = ois;
                            // Set a deserialization checker
                            @SuppressWarnings("removal")
                            var dummy = AccessController.doPrivileged(
                                (PrivilegedAction<Void>)() -> {
                                    ois2.setObjectInputFilter(
                                        new DeserializationChecker(fullLength));
                                    return null;
                            });
                            entry.sealedKey = (SealedObject)ois.readObject();
                            entry.maxLength = fullLength;
                            // NOTE: don't close ois here since we are still
                            // using dis!!!
                        } catch (ClassNotFoundException cnfe) {
                            throw new IOException(cnfe.getMessage());
                        } catch (InvalidClassException ice) {
                            throw new IOException("Invalid secret key format");
                        }

                        // Add the entry to the list
                        entries.put(alias, entry);

                    } else {
                        throw new IOException("Unrecognized keystore entry: " +
                                tag);
                    }
                }

                if (debug != null) {
                    debug.println("JceKeyStore load: private key count: " +
                        privateKeyCount + ". trusted key count: " +
                        trustedKeyCount + ". secret key count: " +
                        secretKeyCount);
                }

                /*
                 * If a password has been provided, we check the keyed digest
                 * at the end. If this check fails, the store has been tampered
                 * with
                 */
                if (password != null) {
                    byte[] computed = md.digest();
                    byte[] actual = IOUtils.readExactlyNBytes(dis, computed.length);
                    if (!MessageDigest.isEqual(computed, actual)) {
                        throw new IOException(
                                "Keystore was tampered with, or "
                                        + "password was incorrect",
                                new UnrecoverableKeyException(
                                        "Password verification failed"));
                    }
                }
            }  finally {
                if (ois != null) {
                    ois.close();
                } else {
                    dis.close();
                }
            }
        }
    }

    /**
     * To guard against tampering with the keystore, we append a keyed
     * hash with a bit of extra data.
     */
    private MessageDigest getPreKeyedHash(char[] password)
        throws NoSuchAlgorithmException
    {
        int i, j;

        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] passwdBytes = new byte[password.length * 2];
        for (i=0, j=0; i<password.length; i++) {
            passwdBytes[j++] = (byte)(password[i] >> 8);
            passwdBytes[j++] = (byte)password[i];
        }
        md.update(passwdBytes);
        for (i=0; i<passwdBytes.length; i++)
            passwdBytes[i] = 0;
        md.update("Mighty Aphrodite".getBytes(UTF_8));
        return md;
    }

    /**
     * Probe the first few bytes of the keystore data stream for a valid
     * JCEKS keystore encoding.
     */
    @Override
    public boolean engineProbe(InputStream stream) throws IOException {
        DataInputStream dataStream;
        if (stream instanceof DataInputStream) {
            dataStream = (DataInputStream)stream;
        } else {
            dataStream = new DataInputStream(stream);
        }

        return JCEKS_MAGIC == dataStream.readInt();
    }

    /*
     * An ObjectInputFilter that checks the format of the secret key being
     * deserialized.
     */
    private static class DeserializationChecker implements ObjectInputFilter {

        // Full length of keystore, anything inside a SecretKeyEntry should not
        // be bigger. Otherwise, must be illegal.
        private final int fullLength;

        public DeserializationChecker(int fullLength) {
            this.fullLength = fullLength;
        }

        @Override
        public ObjectInputFilter.Status
            checkInput(ObjectInputFilter.FilterInfo info) {

            if (info.arrayLength() > fullLength) {
                return Status.REJECTED;
            }
            // First run a custom filter
            Class<?> clazz = info.serialClass();
            switch((int)info.depth()) {
                case 1:
                    if (clazz != SealedObjectForKeyProtector.class) {
                        return Status.REJECTED;
                    }
                    break;
                case 2:
                    if (clazz != null && clazz != SealedObject.class
                            && clazz != byte[].class) {
                        return Status.REJECTED;
                    }
                    break;
                default:
                    if (clazz != null && clazz != Object.class) {
                        return Status.REJECTED;
                    }
                    break;
            }

            // Next run the default filter, if available
            ObjectInputFilter defaultFilter =
                ObjectInputFilter.Config.getSerialFilter();
            if (defaultFilter != null) {
                return defaultFilter.checkInput(info);
            }

            return Status.UNDECIDED;
        }
    }
}
