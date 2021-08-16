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

package sun.security.provider;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.pkcs12.PKCS12KeyStore;
import sun.security.util.Debug;
import sun.security.util.IOUtils;
import sun.security.util.KeyStoreDelegator;

/**
 * This class provides the keystore implementation referred to as "JKS".
 *
 * @author Jan Luehe
 * @author David Brownell
 *
 *
 * @see KeyProtector
 * @see java.security.KeyStoreSpi
 * @see KeyTool
 *
 * @since 1.2
 */

public abstract class JavaKeyStore extends KeyStoreSpi {

    // regular JKS
    public static final class JKS extends JavaKeyStore {
        String convertAlias(String alias) {
            return alias.toLowerCase(Locale.ENGLISH);
        }
    }

    // special JKS that uses case sensitive aliases
    public static final class CaseExactJKS extends JavaKeyStore {
        String convertAlias(String alias) {
            return alias;
        }
    }

    // special JKS that supports JKS and PKCS12 file formats
    public static final class DualFormatJKS extends KeyStoreDelegator {
        public DualFormatJKS() {
            super("JKS", JKS.class, "PKCS12", PKCS12KeyStore.class);
        }

        /**
         * Probe the first few bytes of the keystore data stream for a valid
         * JKS keystore encoding.
         */
        @Override
        public boolean engineProbe(InputStream stream) throws IOException {
            DataInputStream dataStream;
            if (stream instanceof DataInputStream) {
                dataStream = (DataInputStream)stream;
            } else {
                dataStream = new DataInputStream(stream);
            }

            return MAGIC == dataStream.readInt();
        }
    }

    private static final Debug debug = Debug.getInstance("keystore");
    private static final int MAGIC = 0xfeedfeed;
    private static final int VERSION_1 = 0x01;
    private static final int VERSION_2 = 0x02;

    // Private keys and their supporting certificate chains
    private static class KeyEntry {
        Date date; // the creation date of this entry
        byte[] protectedPrivKey;
        Certificate[] chain;
    };

    // Trusted certificates
    private static class TrustedCertEntry {
        Date date; // the creation date of this entry
        Certificate cert;
    };

    /**
     * Private keys and certificates are stored in a hashtable.
     * Hash entries are keyed by alias names.
     */
    private final Hashtable<String, Object> entries;

    JavaKeyStore() {
        entries = new Hashtable<String, Object>();
    }

    // convert an alias to internal form, overridden in subclasses:
    // lower case for regular JKS
    // original string for CaseExactJKS
    abstract String convertAlias(String alias);

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
        Object entry = entries.get(convertAlias(alias));

        if (entry == null || !(entry instanceof KeyEntry)) {
            return null;
        }
        if (password == null) {
            throw new UnrecoverableKeyException("Password must not be null");
        }

        byte[] passwordBytes = convertToBytes(password);
        KeyProtector keyProtector = new KeyProtector(passwordBytes);
        byte[] encrBytes = ((KeyEntry)entry).protectedPrivKey;
        EncryptedPrivateKeyInfo encrInfo;
        try {
            encrInfo = new EncryptedPrivateKeyInfo(encrBytes);
            return keyProtector.recover(encrInfo);
        } catch (IOException ioe) {
            throw new UnrecoverableKeyException("Private key not stored as "
                                                + "PKCS #8 "
                                                + "EncryptedPrivateKeyInfo");
        } finally {
            Arrays.fill(passwordBytes, (byte) 0x00);
        }
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
    public Certificate[] engineGetCertificateChain(String alias) {
        Object entry = entries.get(convertAlias(alias));

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
        Object entry = entries.get(convertAlias(alias));

        if (entry != null) {
            if (entry instanceof TrustedCertEntry) {
                return ((TrustedCertEntry)entry).cert;
            } else {
                if (((KeyEntry)entry).chain == null) {
                    return null;
                } else {
                    return ((KeyEntry)entry).chain[0];
                }
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
        Object entry = entries.get(convertAlias(alias));

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
     * Assigns the given private key to the given alias, protecting
     * it with the given password as defined in PKCS8.
     *
     * <p>The given java.security.PrivateKey <code>key</code> must
     * be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key and certificate
     * chain.
     *
     * @param alias the alias name
     * @param key the private key to be associated with the alias
     * @param password the password to protect the key
     * @param chain the certificate chain for the corresponding public
     * key (only required if the given key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if the given key is not a private key,
     * cannot be protected, or this operation fails for some other reason
     */
    public void engineSetKeyEntry(String alias, Key key, char[] password,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        KeyProtector keyProtector;
        byte[] passwordBytes = null;

        if (!(key instanceof java.security.PrivateKey)) {
            throw new KeyStoreException("Cannot store non-PrivateKeys");
        }
        if (password == null) {
            throw new KeyStoreException("password can't be null");
        }
        try {
            synchronized(entries) {
                KeyEntry entry = new KeyEntry();
                entry.date = new Date();

                // Protect the encoding of the key
                passwordBytes = convertToBytes(password);
                keyProtector = new KeyProtector(passwordBytes);
                entry.protectedPrivKey = keyProtector.protect(key);

                // clone the chain
                if ((chain != null) &&
                    (chain.length != 0)) {
                    entry.chain = chain.clone();
                } else {
                    entry.chain = null;
                }

                entries.put(convertAlias(alias), entry);
            }
        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreException("Key protection algorithm not found");
        } finally {
            if (passwordBytes != null)
                Arrays.fill(passwordBytes, (byte) 0x00);
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
        synchronized(entries) {
            // key must be encoded as EncryptedPrivateKeyInfo as defined in
            // PKCS#8
            try {
                new EncryptedPrivateKeyInfo(key);
            } catch (IOException ioe) {
                throw new KeyStoreException("key is not encoded as "
                                            + "EncryptedPrivateKeyInfo");
            }

            KeyEntry entry = new KeyEntry();
            entry.date = new Date();

            entry.protectedPrivKey = key.clone();
            if ((chain != null) &&
                (chain.length != 0)) {
                entry.chain = chain.clone();
            } else {
                entry.chain = null;
            }

            entries.put(convertAlias(alias), entry);
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

            Object entry = entries.get(convertAlias(alias));
            if ((entry != null) && (entry instanceof KeyEntry)) {
                throw new KeyStoreException
                    ("Cannot overwrite own certificate");
            }

            TrustedCertEntry trustedCertEntry = new TrustedCertEntry();
            trustedCertEntry.cert = cert;
            trustedCertEntry.date = new Date();
            entries.put(convertAlias(alias), trustedCertEntry);
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
            entries.remove(convertAlias(alias));
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
        return entries.containsKey(convertAlias(alias));
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
        Object entry = entries.get(convertAlias(alias));
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
        Object entry = entries.get(convertAlias(alias));
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
        Certificate certElem;

        for (Enumeration<String> e = entries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = entries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                certElem = ((TrustedCertEntry)entry).cert;
            } else if (((KeyEntry)entry).chain != null) {
                certElem = ((KeyEntry)entry).chain[0];
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
             *      tag=1 (big-endian integer),
             *      alias (UTF string)
             *      timestamp
             *      encrypted private-key info according to PKCS #8
             *          (integer length followed by encoding)
             *      cert chain (integer count, then certs; for each cert,
             *          integer length followed by encoding)
             *     }
             *
             * or:
             *
             *     {
             *      tag=2 (big-endian integer)
             *      alias (UTF string)
             *      timestamp
             *      cert (integer length followed by encoding)
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

            dos.writeInt(MAGIC);
            // always write the latest version
            dos.writeInt(VERSION_2);

            dos.writeInt(entries.size());

            for (Enumeration<String> e = entries.keys(); e.hasMoreElements();) {

                String alias = e.nextElement();
                Object entry = entries.get(alias);

                if (entry instanceof KeyEntry) {

                    // Store this entry as a KeyEntry
                    dos.writeInt(1);

                    // Write the alias
                    dos.writeUTF(alias);

                    // Write the (entry creation) date
                    dos.writeLong(((KeyEntry)entry).date.getTime());

                    // Write the protected private key
                    dos.writeInt(((KeyEntry)entry).protectedPrivKey.length);
                    dos.write(((KeyEntry)entry).protectedPrivKey);

                    // Write the certificate chain
                    int chainLen;
                    if (((KeyEntry)entry).chain == null) {
                        chainLen = 0;
                    } else {
                        chainLen = ((KeyEntry)entry).chain.length;
                    }
                    dos.writeInt(chainLen);
                    for (int i = 0; i < chainLen; i++) {
                        encoded = ((KeyEntry)entry).chain[i].getEncoded();
                        dos.writeUTF(((KeyEntry)entry).chain[i].getType());
                        dos.writeInt(encoded.length);
                        dos.write(encoded);
                    }
                } else {

                    // Store this entry as a certificate
                    dos.writeInt(2);

                    // Write the alias
                    dos.writeUTF(alias);

                    // Write the (entry creation) date
                    dos.writeLong(((TrustedCertEntry)entry).date.getTime());

                    // Write the trusted certificate
                    encoded = ((TrustedCertEntry)entry).cert.getEncoded();
                    dos.writeUTF(((TrustedCertEntry)entry).cert.getType());
                    dos.writeInt(encoded.length);
                    dos.write(encoded);
                }
            }

            /*
             * Write the keyed hash which is used to detect tampering with
             * the keystore (such as deleting or modifying key or
             * certificate entries).
             */
            byte[] digest = md.digest();

            dos.write(digest);
            dos.flush();
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
            int trustedKeyCount = 0, privateKeyCount = 0;

            if (stream == null)
                return;

            if (password != null) {
                md = getPreKeyedHash(password);
                dis = new DataInputStream(new DigestInputStream(stream, md));
            } else {
                dis = new DataInputStream(stream);
            }

            // Body format: see store method

            int xMagic = dis.readInt();
            int xVersion = dis.readInt();

            if (xMagic!=MAGIC ||
                (xVersion!=VERSION_1 && xVersion!=VERSION_2)) {
                throw new IOException("Invalid keystore format");
            }

            if (xVersion == VERSION_1) {
                cf = CertificateFactory.getInstance("X509");
            } else {
                // version 2
                cfs = new Hashtable<String, CertificateFactory>(3);
            }

            entries.clear();
            int count = dis.readInt();

            for (int i = 0; i < count; i++) {
                int tag;
                String alias;

                tag = dis.readInt();

                if (tag == 1) { // private key entry
                    privateKeyCount++;
                    KeyEntry entry = new KeyEntry();

                    // Read the alias
                    alias = dis.readUTF();

                    // Read the (entry creation) date
                    entry.date = new Date(dis.readLong());

                    // Read the private key
                    entry.protectedPrivKey =
                            IOUtils.readExactlyNBytes(dis, dis.readInt());

                    // Read the certificate chain
                    int numOfCerts = dis.readInt();
                    if (numOfCerts > 0) {
                        List<Certificate> certs = new ArrayList<>(
                                numOfCerts > 10 ? 10 : numOfCerts);
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
                                    cf = CertificateFactory.getInstance(certType);
                                    // store the certificate factory so we can
                                    // reuse it later
                                    cfs.put(certType, cf);
                                }
                            }
                            // instantiate the certificate
                            encoded = IOUtils.readExactlyNBytes(dis, dis.readInt());
                            bais = new ByteArrayInputStream(encoded);
                            certs.add(cf.generateCertificate(bais));
                            bais.close();
                        }
                        // We can be sure now that numOfCerts of certs are read
                        entry.chain = certs.toArray(new Certificate[numOfCerts]);
                    }

                    // Add the entry to the list
                    entries.put(alias, entry);

                } else if (tag == 2) { // trusted certificate entry
                    trustedKeyCount++;
                    TrustedCertEntry entry = new TrustedCertEntry();

                    // Read the alias
                    alias = dis.readUTF();

                    // Read the (entry creation) date
                    entry.date = new Date(dis.readLong());

                    // Read the trusted certificate
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
                    bais.close();

                    // Add the entry to the list
                    entries.put(alias, entry);

                } else {
                    throw new IOException("Unrecognized keystore entry: " +
                            tag);
                }
            }

            if (debug != null) {
                debug.println("JavaKeyStore load: private key count: " +
                    privateKeyCount + ". trusted key count: " + trustedKeyCount);
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
                    Throwable t = new UnrecoverableKeyException
                            ("Password verification failed");
                    throw (IOException) new IOException
                            ("Keystore was tampered with, or "
                                    + "password was incorrect").initCause(t);
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

        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] passwdBytes = convertToBytes(password);
        md.update(passwdBytes);
        Arrays.fill(passwdBytes, (byte) 0x00);
        md.update("Mighty Aphrodite".getBytes(UTF_8));
        return md;
    }

    /**
     * Helper method to convert char[] to byte[]
     */

    private byte[] convertToBytes(char[] password) {
        int i, j;
        byte[] passwdBytes = new byte[password.length * 2];
        for (i=0, j=0; i<password.length; i++) {
            passwdBytes[j++] = (byte)(password[i] >> 8);
            passwdBytes[j++] = (byte)password[i];
        }
        return passwdBytes;
    }
}
